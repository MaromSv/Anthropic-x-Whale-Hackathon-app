package com.example.emergency.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.screen.common.GroupedListContainer
import com.example.emergency.ui.screen.common.GroupedListDivider
import com.example.emergency.ui.screen.common.SubScreenTopBar
import com.example.emergency.ui.state.ConversationSummary
import com.example.emergency.ui.state.ConversationsUiState
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun ConversationsScreen(
    state: ConversationsUiState,
    onBack: () -> Unit = {},
    onConversationClick: (ConversationSummary) -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        SubScreenTopBar(title = state.title, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (state.conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No saved conversations yet.",
                        style = typography.body,
                        color = colors.textFaint,
                    )
                }
            } else {
                GroupedListContainer {
                    state.conversations.forEachIndexed { index, conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation) },
                        )
                        if (index < state.conversations.lastIndex) {
                            GroupedListDivider()
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    onClick: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                style = typography.listItem.copy(fontSize = 14.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${conversation.messageCount} messages \u00B7 ${conversation.timeLabel}",
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}
