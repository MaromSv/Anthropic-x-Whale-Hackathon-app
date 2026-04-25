package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun ChatTopBar(
    onMenuClick: () -> Unit = {},
    onNewChatClick: () -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                drawLine(
                    color = colors.line,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 4.dp),
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Menu",
                tint = colors.text,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = "Emergency Assistant",
            style = typography.appBarTitle,
            color = colors.text,
            modifier = Modifier.align(Alignment.Center),
        )

        IconButton(
            onClick = onNewChatClick,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "New chat",
                tint = colors.text,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
