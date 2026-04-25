package com.example.emergency.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.emergency.ui.screen.chat.ChatInputBar
import com.example.emergency.ui.screen.common.SubScreenTopBar
import com.example.emergency.ui.state.ChatRole
import com.example.emergency.ui.state.ChatThreadUiState
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun ChatThreadScreen(
    state: ChatThreadUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onMic: () -> Unit = {},
    onCamera: () -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    var composer by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val totalItems = state.messages.size + if (state.isAssistantTyping) 1 else 0
    LaunchedEffect(totalItems) {
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        SubScreenTopBar(title = state.title, onBack = onBack)

        if (state.messages.isEmpty() && !state.isAssistantTyping) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Start a new conversation below.",
                    style = typography.body,
                    color = colors.textFaint,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(items = state.messages, key = { it.id }) { message ->
                    when (message.role) {
                        ChatRole.USER -> UserMessageBubble(text = message.text)
                        ChatRole.ASSISTANT -> AssistantMessageBubble(text = message.text)
                    }
                }
                if (state.isAssistantTyping) {
                    item(key = "typing-indicator") { AssistantTypingBubble() }
                }
            }
        }

        ChatInputBar(
            text = composer,
            onTextChange = { composer = it },
            onSend = {
                if (composer.isNotEmpty()) {
                    onSend(composer)
                    composer = ""
                }
            },
            onMic = onMic,
            onCamera = onCamera,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(EmergencyShapes.card)
                .background(colors.accent)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                style = typography.body,
                color = colors.accentInk,
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(text: String) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(EmergencyShapes.card)
                .background(colors.surface)
                .border(1.dp, colors.line, EmergencyShapes.card)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                style = typography.body,
                color = colors.text,
            )
        }
    }
}

@Composable
private fun AssistantTypingBubble() {
    val colors = EmergencyTheme.colors
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(EmergencyShapes.card)
                .background(colors.surface)
                .border(1.dp, colors.line, EmergencyShapes.card)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            for (idx in 0..2) {
                val alpha by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 600, delayMillis = idx * 150),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dot$idx",
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.textDim.copy(alpha = alpha)),
                )
            }
        }
    }
}
