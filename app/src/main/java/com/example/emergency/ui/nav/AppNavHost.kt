package com.example.emergency.ui.nav

import android.net.Uri
import android.util.Log
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.emergency.agent.ToolManager
import com.example.emergency.llm.GemmaBackend
import com.example.emergency.llm.GemmaLlm
import com.example.emergency.llm.GemmaLoadOptions
import com.example.emergency.ui.state.ToolCallInfo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.emergency.ui.screen.AbcCheckScreen
import com.example.emergency.ui.screen.ChatThreadScreen
import com.example.emergency.ui.screen.ConversationsScreen
import com.example.emergency.ui.screen.DataPacksScreen
import com.example.emergency.ui.screen.FirstAidScreen
import com.example.emergency.ui.screen.GetOutScreen
import com.example.emergency.ui.screen.HomeShell
import com.example.emergency.ui.screen.MapScreen
import com.example.emergency.ui.screen.map.MapDestination
import com.example.emergency.ui.screen.PersonalInfoScreen
import com.example.emergency.ui.screen.SettingsScreen
import com.example.emergency.ui.screen.cpr.CprWalkthroughScreen
import com.example.emergency.ui.state.ChatMessage
import com.example.emergency.ui.state.ChatRole
import com.example.emergency.ui.state.ChatThreadUiState
import com.example.emergency.ui.state.SampleAbcCheckUiState
import com.example.emergency.ui.state.SampleConversationsUiState
import com.example.emergency.ui.state.SampleDataPacksUiState
import com.example.emergency.ui.state.SampleDrawerUiState
import com.example.emergency.ui.state.SampleFirstAidUiState
import com.example.emergency.ui.state.SampleGetOutUiState
import com.example.emergency.ui.state.SampleHomeUiState
import com.example.emergency.ui.state.SampleMapUiState
import com.example.emergency.ui.state.SamplePersonalInfoUiState
import com.example.emergency.ui.state.SampleSettingsUiState
enum class ModelStatus { IDLE, LOADING, READY, ERROR }

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val threadMessages = remember { mutableStateListOf<ChatMessage>() }
    val pendingImages = remember { mutableStateListOf<String>() }
    var isAssistantTyping by remember { mutableStateOf(false) }
    var modelStatus by remember { mutableStateOf(ModelStatus.IDLE) }
    var modelError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Create LLM and ToolManager instances
    val gemma = remember { GemmaLlm(context) }
    val toolManager = remember { ToolManager(context) }

    // Image pickers
    var pendingCameraPath by remember { mutableStateOf<String?>(null) }
    
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && pendingCameraPath != null) {
            pendingImages.add(pendingCameraPath!!)
        }
        pendingCameraPath = null
    }
    
    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingCameraPath != null) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(pendingCameraPath!!)
            )
            takePicture.launch(uri)
        } else {
            pendingCameraPath = null
        }
    }
    
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val ext = context.contentResolver.getType(uri)
                ?.substringAfterLast('/')
                ?.lowercase()
                ?.let { if (it in setOf("jpeg", "jpg", "png", "webp")) it else "jpg" }
                ?: "jpg"
            val dest = File(context.cacheDir, "img_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            pendingImages.add(dest.absolutePath)
        }
    }
    
    // Request location permission on first launch
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("AppNavHost", "Location permissions granted: $permissions")
    }
    
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Clean up when composable leaves composition
    DisposableEffect(Unit) {
        onDispose { gemma.unload() }
    }

    fun startNewChat() {
        threadMessages.clear()
        pendingImages.clear()
        if (gemma.isLoaded) gemma.resetConversation()
    }

    fun sendUserMessage(text: String) {
        val userIndex = threadMessages.size
        val images = pendingImages.toList()
        threadMessages.add(
            ChatMessage(
                id = "u$userIndex",
                role = ChatRole.USER,
                text = text,
                timestampLabel = "now",
                imagePaths = images,
            ),
        )
        pendingImages.clear()

        // Add placeholder assistant message
        val assistantIndex = threadMessages.size
        val assistantId = "a$assistantIndex"
        threadMessages.add(
            ChatMessage(
                id = assistantId,
                role = ChatRole.ASSISTANT,
                text = "",
                timestampLabel = "now",
            ),
        )

        scope.launch {
            isAssistantTyping = true
            try {
                // Load model if not loaded yet
                if (modelStatus == ModelStatus.IDLE) {
                    modelStatus = ModelStatus.LOADING
                    val idx = threadMessages.indexOfFirst { it.id == assistantId }
                    if (idx >= 0) {
                        threadMessages[idx] = threadMessages[idx].copy(
                            text = "Loading model..."
                        )
                    }
                    
                    try {
                        withContext(Dispatchers.IO) {
                            val modelPath = GemmaLlm.defaultModelPath(context)
                            Log.d("AppNavHost", "Loading model from: $modelPath")
                            gemma.load(
                                GemmaLoadOptions(
                                    modelPath = modelPath,
                                    backend = GemmaBackend.GPU,
                                    systemInstruction = buildSystemPrompt(toolManager)
                                )
                            )
                        }
                        modelStatus = ModelStatus.READY
                        Log.d("AppNavHost", "Model loaded successfully")
                        
                        // Clear loading message
                        val idx2 = threadMessages.indexOfFirst { it.id == assistantId }
                        if (idx2 >= 0) {
                            threadMessages[idx2] = threadMessages[idx2].copy(text = "")
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavHost", "Failed to load model", e)
                        modelError = e.message ?: "Unknown error"
                        modelStatus = ModelStatus.ERROR
                        val idx2 = threadMessages.indexOfFirst { it.id == assistantId }
                        if (idx2 >= 0) {
                            threadMessages[idx2] = threadMessages[idx2].copy(
                                text = "Error loading model: ${e.message ?: "Unknown error"}"
                            )
                        }
                        isAssistantTyping = false
                        return@launch
                    }
                }
                
                // Generate response with tool calling support
                if (gemma.isLoaded) {
                    var fullResponse = ""
                    
                    // Stream the initial response. Keep the full text (including any
                    // <tool_call> XML the LLM emits) in fullResponse for the parser, but
                    // strip anything from the first '<' onward before showing it in the
                    // chat bubble so the user never sees raw XML.
                    withContext(Dispatchers.IO) {
                        gemma.generateStreamingWithImages(text, images).collect { token ->
                            fullResponse += token
                            val ltIdx = fullResponse.indexOf('<')
                            val displayText = if (ltIdx >= 0) {
                                fullResponse.substring(0, ltIdx).trim()
                            } else {
                                fullResponse
                            }
                            val idx = threadMessages.indexOfFirst { it.id == assistantId }
                            if (idx >= 0) {
                                val current = threadMessages[idx]
                                threadMessages[idx] = current.copy(text = displayText)
                            }
                        }
                    }
                    
                    Log.d("AppNavHost", "Full LLM response: $fullResponse")
                    
                    // Check for tool calls in the response
                    val toolCalls = toolManager.parseToolCalls(fullResponse)
                    Log.d("AppNavHost", "Parsed tool calls: ${toolCalls.size} found")
                    toolCalls.forEachIndexed { i, call ->
                        Log.d("AppNavHost", "Tool call $i: ${call.toolName} with params: ${call.params}")
                    }
                    if (toolCalls.isNotEmpty()) {
                        // Execute tools
                        for (toolCall in toolCalls) {
                            // Add tool call message
                            val toolIndex = threadMessages.size
                            threadMessages.add(
                                ChatMessage(
                                    id = "t$toolIndex",
                                    role = ChatRole.TOOL,
                                    text = "",
                                    timestampLabel = "now",
                                    toolCall = ToolCallInfo(
                                        toolName = toolCall.toolName,
                                        status = "calling"
                                    )
                                )
                            )
                            
                            // Execute tool
                            Log.d("AppNavHost", "Executing tool: ${toolCall.toolName}")
                            val result = withContext(Dispatchers.IO) {
                                toolManager.executeTool(toolCall)
                            }
                            Log.d("AppNavHost", "Tool ${toolCall.toolName} result: success=${result.success}, data=${result.data}, error=${result.error}")
                            
                            // Update tool call message with result
                            val idx = threadMessages.indexOfFirst { it.id == "t$toolIndex" }
                            if (idx >= 0) {
                                threadMessages[idx] = threadMessages[idx].copy(
                                    toolCall = ToolCallInfo(
                                        toolName = toolCall.toolName,
                                        status = if (result.success) "success" else "error",
                                        result = result.data.take(200) + if (result.data.length > 200) "..." else ""
                                    )
                                )
                            }
                            
                            // Strip the <tool_call> block from the original assistant
                            // bubble so any pre-tool preamble (if present) stays clean.
                            val assistantIdx = threadMessages.indexOfFirst { it.id == assistantId }
                            if (assistantIdx >= 0) {
                                threadMessages[assistantIdx] = threadMessages[assistantIdx].copy(
                                    text = toolManager.removeToolCallBlocks(fullResponse)
                                )
                            }

                            // CPR and ABC are fully handled by their walkthrough cards — no
                            // follow-up LLM response needed (a second bubble confuses).
                            if (toolCall.toolName == "cpr_instructions" || toolCall.toolName == "abc_check") {
                                continue
                            }

                            // For other tools, generate a follow-up response from the result.
                            val followUpPrompt = "The user asked: \"$text\"\n\nTool ${toolCall.toolName} returned:\n${result.data}\n\nUsing this and your system instructions, give the user the final brief, numbered answer."

                            val newAssistantIndex = threadMessages.size
                            val newAssistantId = "a$newAssistantIndex"
                            threadMessages.add(
                                ChatMessage(
                                    id = newAssistantId,
                                    role = ChatRole.ASSISTANT,
                                    text = "",
                                    timestampLabel = "now",
                                )
                            )

                            var toolResponse = ""
                            withContext(Dispatchers.IO) {
                                gemma.generateStreaming(followUpPrompt).collect { token ->
                                    toolResponse += token
                                    val idx = threadMessages.indexOfFirst { it.id == newAssistantId }
                                    if (idx >= 0) {
                                        threadMessages[idx] = threadMessages[idx].copy(text = toolResponse)
                                    }
                                }
                            }
                        }
                    } else {
                        // No tool calls, just clean up the response
                        val idx = threadMessages.indexOfFirst { it.id == assistantId }
                        if (idx >= 0) {
                            threadMessages[idx] = threadMessages[idx].copy(
                                text = toolManager.removeToolCallBlocks(fullResponse)
                            )
                        }
                    }
                } else {
                    // Fallback if model not loaded
                    val idx = threadMessages.indexOfFirst { it.id == assistantId }
                    if (idx >= 0) {
                        threadMessages[idx] = threadMessages[idx].copy(
                            text = "Model not loaded. Please wait or check the model file."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AppNavHost", "Generation error", e)
                val idx = threadMessages.indexOfFirst { it.id == assistantId }
                if (idx >= 0) {
                    threadMessages[idx] = threadMessages[idx].copy(
                        text = "Error: ${e.message ?: "Generation failed"}"
                    )
                }
            } finally {
                isAssistantTyping = false
            }
        }
    }

    fun onCamera() {
        val imageFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        pendingCameraPath = imageFile.absolutePath
        requestCameraPermission.launch(android.Manifest.permission.CAMERA)
    }
    
    fun onGallery() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun onRemoveImage(path: String) {
        pendingImages.remove(path)
    }

    NavHost(
        navController = navController,
        startDestination = Route.Home.path,
    ) {
        composable(Route.Home.path) {
            HomeShell(
                homeState = SampleHomeUiState,
                drawerState = SampleDrawerUiState,
                onToolClick = { id ->
                    navController.navigate(id.toRoute().navigatePath)
                },
                onDrawerItemClick = { id ->
                    navController.navigate(id.toRoute().navigatePath)
                },
                onSend = { text ->
                    sendUserMessage(text)
                    navController.navigate(Route.ChatThread.path) {
                        launchSingleTop = true
                    }
                },
                onNewChatClick = { startNewChat() },
                onCamera = { onCamera() },
                onGallery = { onGallery() },
                pendingImages = pendingImages,
                onRemoveImage = { path -> onRemoveImage(path) },
            )
        }
        composable(Route.ChatThread.path) {
            ChatThreadScreen(
                state = ChatThreadUiState(
                    title = "Conversation",
                    messages = threadMessages,
                    isAssistantTyping = isAssistantTyping,
                ),
                onBack = { navController.popBackStack() },
                onNewChat = {
                    startNewChat()
                    navController.popBackStack(Route.Home.path, inclusive = false)
                },
                onSend = { text -> sendUserMessage(text) },
                onCamera = { onCamera() },
                onGallery = { onGallery() },
                pendingImages = pendingImages,
                onRemoveImage = { path -> onRemoveImage(path) },
                onOpenTool = { toolCall ->
                    when (toolCall.toolName) {
                        "cpr_instructions" -> navController.navigate(Route.CprWalkthrough.path)
                        "abc_check" -> navController.navigate(Route.AbcCheck.path)
                        "find_nearest" -> {
                            val dest = parseFindNearestDestination(toolCall.result)
                            val target = if (dest != null) {
                                Route.Map.withDestination(dest.lat, dest.lon, dest.name, dest.category)
                            } else {
                                "map"
                            }
                            navController.navigate(target)
                        }
                    }
                },
            )
        }
        composable(Route.DataPacks.path) {
            DataPacksScreen(
                state = SampleDataPacksUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.PersonalInfo.path) {
            PersonalInfoScreen(
                state = SamplePersonalInfoUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.Conversations.path) {
            ConversationsScreen(
                state = SampleConversationsUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Route.Map.path,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lon") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val lat = entry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = entry.arguments?.getString("lon")?.toDoubleOrNull()
            val name = entry.arguments?.getString("name")
            val category = entry.arguments?.getString("category")
            val dest = if (lat != null && lon != null && !name.isNullOrBlank() && !category.isNullOrBlank()) {
                MapDestination(name, category, lat, lon)
            } else null
            MapScreen(
                state = SampleMapUiState,
                onBack = { navController.popBackStack() },
                initialDestination = dest,
            )
        }
        composable(Route.FirstAid.path) {
            FirstAidScreen(
                state = SampleFirstAidUiState,
                onBack = { navController.popBackStack() },
                onAbcCheckClick = { navController.navigate(Route.AbcCheck.path) },
            )
        }
        composable(Route.AbcCheck.path) {
            AbcCheckScreen(
                state = SampleAbcCheckUiState,
                onBack = { navController.popBackStack() },
                onStartCpr = {
                    navController.navigate(Route.CprWalkthrough.path) {
                        popUpTo(Route.AbcCheck.path) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.CprWalkthrough.path) {
            CprWalkthroughScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.GetOut.path) {
            GetOutScreen(
                state = SampleGetOutUiState,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.Settings.path) {
            SettingsScreen(
                state = SampleSettingsUiState,
                onBack = { navController.popBackStack() },
                onPersonalInfoClick = { navController.navigate(Route.PersonalInfo.path) },
            )
        }
    }
}

private fun buildSystemPrompt(toolManager: ToolManager): String {
    return """
You are an emergency medical assistant. Your job is to call the correct tool and then return a brief, numbered answer for a layperson.

${toolManager.getToolDescriptions()}

**How to choose a tool — follow this decision tree in order:**

1. User mentions NOT BREATHING, no pulse, cardiac arrest, or explicitly asks for CPR → call `cpr_instructions`.
2. User says someone is unresponsive/collapsed/passed out/fainted/won't wake up BUT has NOT confirmed they are not breathing → call `abc_check` first so they can assess Airway-Breathing-Circulation.
3. After abc_check, if the user reports the person is NOT breathing → THEN call `cpr_instructions`.
4. Medical question (wound, burn, bleeding, fracture, poisoning, choking, etc.) → call `search_medical_database` with the specific condition as query.
5. User asks to find the nearest hospital, pharmacy, AED, police, fire station, shelter, doctor, water, toilet, metro, fuel, supermarket, ATM, phone, school, bunker → call `find_nearest` with the matching category.
   Supported categories: hospital, doctor, first_aid, aed, pharmacy, police, fire, shelter, water, toilet, metro, parking_underground, bunker, fuel, supermarket, atm, phone, school, community, worship.
6. User asks for their location or directions to a specific place → call `get_location`.

**Output rules:**
- Your FIRST response must be the `<tool_call>` block — nothing before it, nothing after it.
- After the tool returns, reply with a numbered list (max 6 short steps, ≤20 words each). No preamble, no medical jargon.

**Examples:**

User: "Someone collapsed, she's not breathing!"
Assistant:
<tool_call>
cpr_instructions
</tool_call>

User: "Someone collapsed and isn't responding — what do I check first?"
Assistant:
<tool_call>
abc_check
</tool_call>

User (after abc_check): "She's not breathing"
Assistant:
<tool_call>
cpr_instructions
</tool_call>

User: "Where is the nearest pharmacy?"
Assistant:
<tool_call>
find_nearest
category=pharmacy
</tool_call>

User: "I need to find an AED"
Assistant:
<tool_call>
find_nearest
category=aed
</tool_call>

User: "Where is the closest police station?"
Assistant:
<tool_call>
find_nearest
category=police
</tool_call>

User: "Find me a hospital"
Assistant:
<tool_call>
find_nearest
category=hospital
</tool_call>

User: "I need shelter"
Assistant:
<tool_call>
find_nearest
category=shelter
</tool_call>

User: "[image of bleeding wound] how do I apply a tourniquet?"
Assistant:
<tool_call>
search_medical_database
query=tourniquet
</tool_call>
    """.trimIndent()
}

private fun parseFindNearestDestination(raw: String): MapDestination? {
    val trimmed = raw.trim().removeSuffix("...")
    if (!trimmed.startsWith("{")) return null
    return runCatching {
        val obj = org.json.JSONObject(trimmed)
        val name = obj.optString("name").takeIf { it.isNotBlank() } ?: return@runCatching null
        val category = obj.optString("category").takeIf { it.isNotBlank() } ?: return@runCatching null
        val lat = obj.optDouble("lat", Double.NaN).takeIf { !it.isNaN() } ?: return@runCatching null
        val lon = obj.optDouble("lon", Double.NaN).takeIf { !it.isNaN() } ?: return@runCatching null
        MapDestination(name, category, lat, lon)
    }.getOrNull()
}
