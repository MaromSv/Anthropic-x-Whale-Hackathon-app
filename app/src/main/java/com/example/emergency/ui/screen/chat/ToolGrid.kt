package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.state.ToolId
import com.example.emergency.ui.state.ToolTile
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun ToolGrid(
    tools: List<ToolTile>,
    onToolClick: (ToolId) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 110.dp)
                            .clip(EmergencyShapes.toolTile)
                            .background(colors.panel)
                            .clickable { onToolClick(tool.id) }
                            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                    ) {
                        Icon(
                            imageVector = tool.id.icon(),
                            contentDescription = tool.title,
                            tint = colors.text,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = tool.title,
                            style = typography.listItem.copy(fontSize = 14.sp),
                            color = colors.text,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tool.subtitle,
                            style = typography.helper.copy(fontSize = 12.sp),
                            color = colors.textDim,
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

private fun ToolId.icon(): ImageVector = when (this) {
    ToolId.FIRST_AID -> Icons.Outlined.FavoriteBorder
    ToolId.ABC_CHECK -> Icons.Outlined.Shield
    ToolId.MAP -> Icons.Outlined.Map
    ToolId.GET_OUT -> Icons.Outlined.Groups
}
