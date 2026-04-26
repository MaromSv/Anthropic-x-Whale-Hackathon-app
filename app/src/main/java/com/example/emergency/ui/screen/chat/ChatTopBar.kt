package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.rounded.Add as AddRounded
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun ChatTopBar(
    onMenuClick: () -> Unit = {},
    onNewChatClick: () -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val markRed = Color(0xFFEF4444)
    val markNavy = Color(0xFF1E293B)

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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = markNavy, fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                        append("mar")
                    }
                    withStyle(SpanStyle(color = markRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                        append("k")
                    }
                },
            )
        }

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
