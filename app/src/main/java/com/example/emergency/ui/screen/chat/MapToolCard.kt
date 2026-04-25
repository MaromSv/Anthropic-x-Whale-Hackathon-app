package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun MapToolCard(
    title: String,
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
        androidx.compose.foundation.layout.Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .background(colors.map),
            ) {
                MiniMap(modifier = Modifier.fillMaxSize())
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

@Composable
private fun MiniMap(modifier: Modifier = Modifier) {
    val colors = EmergencyTheme.colors
    val semantic = EmergencyTheme.semantic
    Canvas(modifier = modifier) {
        // viewBox in JSX is 320×156; scale to actual canvas size.
        val sx = size.width / 320f
        val sy = size.height / 156f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)

        drawWaterCurve(
            colors.mapWater,
            p0 = p(0f, 50f), c1 = p(80f, 30f), c2 = p(240f, 70f), p1 = p(320f, 40f),
        )
        drawWaterCurve(
            colors.mapWater,
            p0 = p(0f, 110f), c1 = p(80f, 90f), c2 = p(240f, 130f), p1 = p(320f, 105f),
        )

        drawLine(colors.mapPath, p(95f, 0f), p(110f, 156f), strokeWidth = 2f * sx)
        drawLine(colors.mapPath, p(215f, 0f), p(230f, 156f), strokeWidth = 2f * sx)
        drawLine(colors.mapPath, p(0f, 78f), p(320f, 78f), strokeWidth = 2f * sy)

        drawDashedRoute(colors.text, p(130f, 95f), p(180f, 75f), p(200f, 65f), strokeWidth = 1.5f * sx)

        val you = p(130f, 95f)
        drawCircle(semantic.youHereHalo, radius = 14f * sx, center = you)
        drawCircle(semantic.youHere, radius = 6f * sx, center = you)
        drawCircle(Color.White, radius = 6f * sx, center = you, style = Stroke(width = 2.5f * sx))
        drawCircle(semantic.youHere, radius = 6f * sx - 1.25f * sx, center = you)

        val dest = p(200f, 65f)
        drawCircle(colors.surface, radius = 11f * sx, center = dest)
        drawCircle(colors.danger, radius = 11f * sx, center = dest, style = Stroke(width = 1.5f * sx))
        drawLine(colors.danger, dest.copy(x = dest.x - 3f * sx), dest.copy(x = dest.x + 3f * sx), strokeWidth = 1.5f * sx, cap = StrokeCap.Round)
        drawLine(colors.danger, dest.copy(y = dest.y - 3f * sy), dest.copy(y = dest.y + 3f * sy), strokeWidth = 1.5f * sx, cap = StrokeCap.Round)

        val exit = p(60f, 50f)
        drawCircle(colors.surface, radius = 9f * sx, center = exit)
        drawCircle(colors.text, radius = 9f * sx, center = exit, style = Stroke(width = 1.5f * sx))
    }
}

private fun DrawScope.drawWaterCurve(color: Color, p0: Offset, c1: Offset, c2: Offset, p1: Offset) {
    val path = Path().apply {
        moveTo(p0.x, p0.y)
        cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
    }
    drawPath(path, color, style = Stroke(width = 9f * (size.width / 320f), cap = StrokeCap.Round))
}

private fun DrawScope.drawDashedRoute(color: Color, start: Offset, ctrl: Offset, end: Offset, strokeWidth: Float) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(ctrl.x, ctrl.y, end.x, end.y)
    }
    val s = size.width / 320f
    drawPath(
        path,
        color.copy(alpha = 0.7f),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * s, 4f * s)),
        ),
    )
}

