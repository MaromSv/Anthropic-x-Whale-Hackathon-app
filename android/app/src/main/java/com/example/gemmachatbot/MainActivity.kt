package com.example.gemmachatbot

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw under system bars; Scaffold's WindowInsets handles padding.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            GemmaChatbotTheme {
                ChatScreen(vm)
            }
        }
    }
}

@Composable
fun GemmaChatbotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) vm.attachImage(uri)
    }

    // Pending camera capture path; set when we launch TakePicture and read
    // back in the result callback.
    var pendingCameraPath by remember { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        pendingCameraPath?.let { vm.confirmCameraImage(it, success) }
        pendingCameraPath = null
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text?.length) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Gemma On-Device Chat", fontWeight = FontWeight.SemiBold) },
                )
                StatusBar(state, onLoad = vm::loadModel)
            }
        },
        bottomBar = {
            InputArea(
                state = state,
                onInputChange = vm::setInput,
                onPickImage = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onTakePhoto = {
                    val (path, uri) = vm.createCameraImageFile()
                    pendingCameraPath = path
                    takePicture.launch(uri)
                },
                onRemoveImage = vm::removePendingImage,
                onSend = vm::send,
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { msg -> Bubble(msg) }
        }
    }
}

@Composable
private fun StatusBar(state: ChatUiState, onLoad: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val statusText = when (state.status) {
            LoadStatus.Idle -> "idle"
            LoadStatus.Loading -> "loading model…"
            LoadStatus.Ready -> "ready"
            LoadStatus.Error -> "error"
        }
        Text(
            text = "status: $statusText" + (state.error?.let { " — $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = if (state.status == LoadStatus.Error)
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.status != LoadStatus.Ready) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onLoad,
                enabled = state.status != LoadStatus.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.status == LoadStatus.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Load model")
                }
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val isUser = msg.role == ChatMessage.Role.User
    val bg = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            msg.imagePaths.forEach { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            if (msg.text.isNotEmpty() || msg.imagePaths.isEmpty()) {
                Text(
                    text = msg.text.ifEmpty { "…" },
                    color = fg,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputArea(
    state: ChatUiState,
    onInputChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onSend: () -> Unit,
) {
    val enabled = state.status == LoadStatus.Ready && !state.generating
    val canSend = enabled && (state.input.isNotBlank() || state.pendingImages.isNotEmpty())

    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Push above the IME and the navigation bar so the input
                // never sits under the soft-key/gesture area.
                .windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (state.pendingImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(state.pendingImages, key = { it }) { path ->
                        Box {
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                            IconButton(
                                onClick = { onRemoveImage(path) },
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilledIconButton(
                    onClick = onPickImage,
                    enabled = enabled,
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add image")
                }
                FilledIconButton(
                    onClick = onTakePhoto,
                    enabled = enabled,
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Take photo")
                }
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when {
                                state.status != LoadStatus.Ready -> "Load the model first"
                                state.pendingImages.isNotEmpty() -> "Describe the image…"
                                else -> "Ask Gemma…"
                            }
                        )
                    },
                    enabled = enabled,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions.Default,
                )
                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                ) {
                    if (state.generating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
