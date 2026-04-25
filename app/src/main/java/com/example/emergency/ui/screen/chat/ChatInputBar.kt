package com.example.emergency.ui.screen.chat

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
<<<<<<< HEAD
=======
import androidx.compose.foundation.layout.PaddingValues
>>>>>>> feature/integrate-map-app-with-maps
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme
import java.io.File

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit = {},
    onCamera: () -> Unit = {},
    pendingImages: List<String> = emptyList(),
    onRemoveImage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val hasText = text.isNotEmpty()
    val hasImages = pendingImages.isNotEmpty()
    val canSend = hasText || hasImages

<<<<<<< HEAD
    Column(modifier = modifier.fillMaxWidth()) {
        // Show pending images
        if (hasImages) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
=======
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Image previews
        if (hasImages) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
>>>>>>> feature/integrate-map-app-with-maps
            ) {
                items(pendingImages) { path ->
                    Box {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        IconButton(
                            onClick = { onRemoveImage(path) },
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
<<<<<<< HEAD
                                .background(colors.panel),
=======
                                .background(colors.surface),
>>>>>>> feature/integrate-map-app-with-maps
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
<<<<<<< HEAD
                                tint = colors.text,
                                modifier = Modifier.size(12.dp),
=======
                                modifier = Modifier.size(12.dp),
                                tint = colors.text,
>>>>>>> feature/integrate-map-app-with-maps
                            )
                        }
                    }
                }
            }
        }

<<<<<<< HEAD
=======
        // Input row
>>>>>>> feature/integrate-map-app-with-maps
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .background(colors.panel, EmergencyShapes.pill)
                .border(1.dp, colors.line, EmergencyShapes.pill)
                .padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
<<<<<<< HEAD
            ComposerIconButton(
                icon = Icons.Outlined.PhotoCamera,
                contentDescription = "Add photo",
                tint = colors.textDim,
                onClick = onCamera,
            )

            Spacer(modifier = Modifier.width(4.dp))
=======
        ComposerIconButton(
            icon = Icons.Outlined.PhotoCamera,
            contentDescription = "Add photo",
            tint = colors.textDim,
            onClick = onCamera,
        )

        Spacer(modifier = Modifier.width(4.dp))
>>>>>>> feature/integrate-map-app-with-maps

            Box(modifier = Modifier.weight(1f)) {
                if (!hasText) {
                    Text(
                        text = if (hasImages) "Add a description (optional)..." else "Describe what's happening\u2026",
                        style = typography.body,
                        color = colors.textFaint,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = typography.body.copy(color = colors.text),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Crossfade(targetState = canSend, label = "composer-action") { showSend ->
                if (showSend) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .clickable(onClick = onSend),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = "Send",
                            tint = colors.accentInk,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    ComposerIconButton(
                        icon = Icons.Outlined.Mic,
                        contentDescription = "Voice input",
                        tint = colors.textDim,
                        onClick = onMic,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
