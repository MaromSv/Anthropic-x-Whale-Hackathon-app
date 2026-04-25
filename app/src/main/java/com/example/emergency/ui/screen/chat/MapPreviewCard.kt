package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.state.CrowdDensity
import com.example.emergency.ui.state.MapSummary
import com.example.emergency.ui.theme.EmergencyColors
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme
import com.example.emergency.ui.theme.SemanticColors

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
            MapPlaceholder(
                colors = colors,
                semantic = semantic,
                modifier = Modifier.fillMaxSize(),
            )

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

@Composable
private fun MapPlaceholder(
    colors: EmergencyColors,
    semantic: SemanticColors,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 1..4) {
                val y = size.height * i / 5
                drawLine(colors.mapPath, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            for (i in 1..5) {
                val x = size.width * i / 6
                drawLine(colors.mapPath, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
            }

            drawLine(
                colors.mapWater,
                Offset(0f, size.height * 0.55f),
                Offset(size.width, size.height * 0.65f),
                strokeWidth = 14.dp.toPx(),
            )
            drawLine(
                colors.mapWater,
                Offset(0f, size.height * 0.78f),
                Offset(size.width, size.height * 0.86f),
                strokeWidth = 10.dp.toPx(),
            )

            val youPos = Offset(size.width * 0.40f, size.height * 0.60f)
            drawCircle(semantic.youHereHalo, radius = 14.dp.toPx(), center = youPos)
            drawCircle(semantic.youHere, radius = 5.dp.toPx(), center = youPos)

            val poiPos = Offset(size.width * 0.62f, size.height * 0.30f)
            drawLine(
                color = colors.text.copy(alpha = 0.7f),
                start = youPos,
                end = poiPos,
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            )

            drawCircle(colors.surface, radius = 9.dp.toPx(), center = poiPos)
            drawCircle(colors.danger, radius = 9.dp.toPx(), center = poiPos, style = Stroke(width = 1.5.dp.toPx()))
            drawLine(colors.danger, Offset(poiPos.x - 4.dp.toPx(), poiPos.y), Offset(poiPos.x + 4.dp.toPx(), poiPos.y), strokeWidth = 1.5.dp.toPx())
            drawLine(colors.danger, Offset(poiPos.x, poiPos.y - 4.dp.toPx()), Offset(poiPos.x, poiPos.y + 4.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        }
    }
}
