package com.example.emergency.ui.nav

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.emergency.llm.GemmaBackend
import com.example.emergency.llm.GemmaLlm
import com.example.emergency.llm.GemmaLoadOptions
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Create GemmaLlm instance
    val gemma = remember { GemmaLlm(context) }

    // Image pickers - declare state first
    var pendingCameraPath by remember { mutableStateOf<String?>(null) }
    
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && pendingCameraPath != null) {
            pendingImages.add(pendingCameraPath!!)
        } else {
            pendingCameraPath?.let { File(it).delete() }
        }
        pendingCameraPath = null
    }
    
    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted && pendingCameraPath != null) {
            val file = File(pendingCameraPath!!)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            takePicture.launch(uri)
        } else {
            pendingCameraPath?.let { File(it).delete() }
            pendingCameraPath = null
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
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
                
                if (gemma.isLoaded) {
                    withContext(Dispatchers.IO) {
                        gemma.generateStreamingWithImages(text, images).collect { token ->
                            // Update the assistant message with streaming tokens
                            val idx = threadMessages.indexOfFirst { it.id == assistantId }
                            if (idx >= 0) {
                                val current = threadMessages[idx]
                                threadMessages[idx] = current.copy(text = current.text + token)
                            }
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
                onCamera = {
                    val file = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
                    file.createNewFile()
                    pendingCameraPath = file.absolutePath
                    requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                },
                pendingImages = pendingImages.toList(),
                onRemoveImage = { path -> pendingImages.remove(path) },
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
            )
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
