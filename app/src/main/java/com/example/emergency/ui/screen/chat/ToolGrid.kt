package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.state.ToolId
import com.example.emergency.ui.state.ToolTile
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme
import com.example.emergency.ui.theme.ToolPalette

@Composable
fun ToolGrid(
    tools: List<ToolTile>,
    onToolClick: (ToolId) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val palettes = EmergencyTheme.toolPalettes

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        for (row in tools.chunked(2)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (tool in row) {
                    val palette = when (tool.id) {
                        ToolId.FIRST_AID -> palettes.firstAid
                        ToolId.ABC_CHECK -> palettes.abcCheck
                        ToolId.MAP -> palettes.map
                        ToolId.GET_OUT -> palettes.getOut
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 110.dp)
                            .clip(EmergencyShapes.toolTile)
                            .background(colors.panel)
                            .clickable { onToolClick(tool.id) }
                            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                    ) {
                        ToolIconChip(
                            icon = tool.id.icon(),
                            contentDescription = tool.title,
                            palette = palette,
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = tool.title,
                            style = typography.listItem.copy(fontSize = 14.sp),
                            color = colors.text,
                        )
                        if (tool.subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tool.subtitle,
                                style = typography.helper.copy(fontSize = 12.sp),
                                color = colors.textDim,
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun ToolIconChip(
    icon: ImageVector,
    contentDescription: String,
    palette: ToolPalette,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.fg,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun ToolId.icon(): ImageVector = when (this) {
    ToolId.FIRST_AID -> Icons.Rounded.Favorite
    ToolId.ABC_CHECK -> Icons.Rounded.Shield
    ToolId.MAP -> Icons.Rounded.Map
    ToolId.GET_OUT -> Icons.Rounded.Groups
}
