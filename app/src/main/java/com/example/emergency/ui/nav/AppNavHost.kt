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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                    
                    // Stream the initial response
                    withContext(Dispatchers.IO) {
                        gemma.generateStreamingWithImages(text, images).collect { token ->
                            fullResponse += token
                            val idx = threadMessages.indexOfFirst { it.id == assistantId }
                            if (idx >= 0) {
                                val current = threadMessages[idx]
                                threadMessages[idx] = current.copy(text = fullResponse)
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
                            
                            // Generate follow-up response with tool result
                            val followUpPrompt = "Tool ${toolCall.toolName} returned: ${result.data}\n\nBased on this information, provide your response to the user."
                            
                            // Clear previous assistant message and stream new response
                            val assistantIdx = threadMessages.indexOfFirst { it.id == assistantId }
                            if (assistantIdx >= 0) {
                                threadMessages[assistantIdx] = threadMessages[assistantIdx].copy(
                                    text = toolManager.removeToolCallBlocks(fullResponse)
                                )
                            }
                            
                            // Add new assistant message for tool-based response
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
                    navController.navigate(id.toRoute().path)
                },
                onDrawerItemClick = { id ->
                    navController.navigate(id.toRoute().path)
                },
                onSend = { text ->
                    sendUserMessage(text)
                    navController.navigate(Route.ChatThread.path) {
                        launchSingleTop = true
                    }
                },
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
                onSend = { text -> sendUserMessage(text) },
                onCamera = { onCamera() },
                onGallery = { onGallery() },
                pendingImages = pendingImages,
                onRemoveImage = { path -> onRemoveImage(path) },
                onOpenTool = { name ->
                    when (name) {
                        "cpr_instructions" -> navController.navigate(Route.CprWalkthrough.path)
                        "find_nearest" -> navController.navigate(Route.Map.path)
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
        composable(Route.Map.path) {
            MapScreen(
                state = SampleMapUiState,
                onBack = { navController.popBackStack() },
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
                onStartCpr = { navController.navigate(Route.CprWalkthrough.path) },
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
You are an emergency medical assistant AI that provides concise, step-by-step advice in urgent medical situations. Your responses must be brief, focused on essential actions, and suitable for laypeople. Always advise calling emergency services if the situation may be life-threatening.

${toolManager.getToolDescriptions()}

**Critical Guidelines:**
- Keep responses extremely brief (maximum 2-3 concise steps, each ≤20 words)
- Use numbered bullets for clarity
- Before offering actions, assess symptoms, risk level, and urgency
- ALWAYS use tools when they would be helpful:
  - Use get_location when the user needs emergency services or their location
  - Use search_medical_database when you need medical protocols or treatment information
  - Use cpr_instructions when someone needs CPR guidance
- Do NOT provide lengthy explanations or medical jargon
- If life-threatening, immediately advise to call emergency services

**Example interactions:**
User: "Someone collapsed and isn't breathing"
Assistant: 
<tool_call>
cpr_instructions
</tool_call>

User: "Where am I? I need an ambulance"
Assistant:
<tool_call>
get_location
</tool_call>

User: "How do I treat a severe burn?"
Assistant:
<tool_call>
search_medical_database
query=burn treatment
</tool_call>
    """.trimIndent()
}
