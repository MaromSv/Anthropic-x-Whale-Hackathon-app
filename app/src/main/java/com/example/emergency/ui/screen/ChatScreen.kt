package com.example.emergency.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.emergency.ui.screen.chat.ChatInputBar
import com.example.emergency.ui.screen.chat.ChatTopBar
import com.example.emergency.ui.screen.chat.HeroSection
import com.example.emergency.ui.screen.chat.MapPreviewCard
import com.example.emergency.ui.screen.chat.ToolGrid
import com.example.emergency.ui.state.HomeUiState
import com.example.emergency.ui.state.ToolId
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun ChatScreen(
    state: HomeUiState,
    onMenuClick: () -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onToolClick: (ToolId) -> Unit = {},
    onSend: (String) -> Unit = {},
    onMic: () -> Unit = {},
    onCamera: () -> Unit = {},
    onGallery: () -> Unit = {},
    pendingImages: List<String> = emptyList(),
    onRemoveImage: (String) -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    var composer by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        ChatTopBar(
            onMenuClick = onMenuClick,
            onNewChatClick = onNewChatClick,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            HeroSection(
                greeting = state.greeting,
                subtitle = state.subtitle,
            )
            Spacer(modifier = Modifier.height(18.dp))
            MapPreviewCard(
                summary = state.mapSummary,
                onClick = { onToolClick(ToolId.MAP) },
            )
            Spacer(modifier = Modifier.height(14.dp))
            ToolGrid(
                tools = state.tools,
                onToolClick = onToolClick,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        ChatInputBar(
            text = composer,
            onTextChange = { composer = it },
            onSend = {
                if (composer.isNotEmpty() || pendingImages.isNotEmpty()) {
                    onSend(composer)
                    composer = ""
                }
            },
            onMic = onMic,
            onCamera = onCamera,
            onGallery = onGallery,
            pendingImages = pendingImages,
            onRemoveImage = onRemoveImage,
        )
    }
}
