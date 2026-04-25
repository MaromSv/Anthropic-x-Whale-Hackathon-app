package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.screen.map.LiveMiniMap
import com.example.emergency.ui.screen.map.MapDestination
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun MapToolCard(
    title: String,
    destination: MapDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val semantic = EmergencyTheme.semantic

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.hero)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.hero)
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .background(colors.map),
            ) {
                LiveMiniMap(
                    modifier = Modifier.fillMaxSize(),
                    destination = destination,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .padding(start = 10.dp, top = 10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(semantic.mapToolBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = null,
                        tint = semantic.mapToolInk,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "map",
                        style = typography.monoMicro.copy(fontSize = 11.sp),
                        color = semantic.mapToolInk,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = title,
                    style = typography.listItem.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Open map",
                    tint = colors.textFaint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
