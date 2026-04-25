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
import androidx.compose.foundation.layout.width
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
import com.example.emergency.ui.state.CrowdDensity
import com.example.emergency.ui.state.MapSummary
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun MapPreviewCard(
    summary: MapSummary,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EmergencyTheme.colors
    val semantic = EmergencyTheme.semantic
    val typography = EmergencyTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(EmergencyShapes.hero)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.hero)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(colors.map),
        ) {
            LiveMiniMap(modifier = Modifier.fillMaxSize())

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(EmergencyShapes.full)
                    .background(colors.surface)
                    .border(1.dp, colors.line, EmergencyShapes.full)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null,
                    tint = colors.textDim,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "live",
                    style = typography.monoMicro,
                    color = colors.textDim,
                )
            }

            if (summary.density == CrowdDensity.HIGH) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(EmergencyShapes.full)
                        .background(semantic.crowdWarningBg)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "High density",
                        style = typography.helper.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = semantic.crowdWarningInk,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = summary.title,
                    style = typography.listItem.copy(fontSize = 14.sp),
                    color = colors.text,
                )
                Text(
                    text = summary.sub,
                    style = typography.helper.copy(fontSize = 12.sp),
                    color = colors.textDim,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Open map",
                tint = colors.textFaint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

