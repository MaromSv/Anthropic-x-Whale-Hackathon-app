package com.example.emergency.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.screen.common.GroupedListContainer
import com.example.emergency.ui.screen.common.GroupedListDivider
import com.example.emergency.ui.screen.common.ScreenSectionHeader
import com.example.emergency.ui.screen.common.SubScreenTopBar
import com.example.emergency.ui.state.CrowdDensity
import com.example.emergency.ui.state.MapFilter
import com.example.emergency.ui.state.MapPoi
import com.example.emergency.ui.state.MapUiState
import com.example.emergency.ui.state.PoiKind
import com.example.emergency.ui.theme.EmergencyColors
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme
import com.example.emergency.ui.theme.SemanticColors

@Composable
fun MapScreen(
    state: MapUiState,
    onBack: () -> Unit = {},
    onFilterClick: (MapFilter) -> Unit = {},
    onPoiClick: (MapPoi) -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    val semantic = EmergencyTheme.semantic
    val typography = EmergencyTheme.typography

    val visiblePois = filterPois(state.pois, state.selectedFilter)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        SubScreenTopBar(title = state.title, onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
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
                    .padding(12.dp)
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

            if (state.density == CrowdDensity.HIGH) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(EmergencyShapes.full)
                        .background(semantic.crowdWarningBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = state.densityLabel,
                        style = typography.helper.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = semantic.crowdWarningInk,
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp),
        ) {
            state.filters.forEach { filter ->
                FilterChip(
                    label = filter.label(),
                    selected = filter == state.selectedFilter,
                    onClick = { onFilterClick(filter) },
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            ScreenSectionHeader(text = "NEARBY")
            Spacer(modifier = Modifier.height(8.dp))
            GroupedListContainer {
                visiblePois.forEachIndexed { index, poi ->
                    PoiRow(poi = poi, onClick = { onPoiClick(poi) })
                    if (index < visiblePois.lastIndex) {
                        GroupedListDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    val background = if (selected) colors.accent else colors.surface
    val ink = if (selected) colors.accentInk else colors.text
    val borderColor = if (selected) colors.accent else colors.line

    Box(
        modifier = Modifier
            .clip(EmergencyShapes.full)
            .background(background)
            .border(1.dp, borderColor, EmergencyShapes.full)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = typography.helper.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = ink,
        )
    }
}

@Composable
private fun PoiRow(
    poi: MapPoi,
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(EmergencyShapes.full)
                .background(colors.panel)
                .border(1.dp, colors.line, EmergencyShapes.full),
        ) {
            Icon(
                imageVector = poi.kind.icon(),
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = poi.name,
                style = typography.listItem.copy(fontSize = 14.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = poi.kindLabel,
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = poi.distanceLabel,
                style = typography.monoMicro,
                color = colors.textDim,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = poi.bearingLabel,
                style = typography.helper.copy(fontSize = 11.sp),
                color = colors.textFaint,
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
            for (i in 1..6) {
                val y = size.height * i / 7
                drawLine(colors.mapPath, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            for (i in 1..7) {
                val x = size.width * i / 8
                drawLine(colors.mapPath, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
            }

            drawLine(
                colors.mapWater,
                Offset(0f, size.height * 0.55f),
                Offset(size.width, size.height * 0.65f),
                strokeWidth = 22.dp.toPx(),
            )
            drawLine(
                colors.mapWater,
                Offset(0f, size.height * 0.80f),
                Offset(size.width, size.height * 0.88f),
                strokeWidth = 16.dp.toPx(),
            )

            val youPos = Offset(size.width * 0.38f, size.height * 0.58f)
            drawCircle(semantic.youHereHalo, radius = 22.dp.toPx(), center = youPos)
            drawCircle(semantic.youHere, radius = 7.dp.toPx(), center = youPos)

            val poiPos = Offset(size.width * 0.66f, size.height * 0.28f)
            drawLine(
                color = colors.text.copy(alpha = 0.7f),
                start = youPos,
                end = poiPos,
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f)),
            )

            drawCircle(colors.surface, radius = 11.dp.toPx(), center = poiPos)
            drawCircle(colors.danger, radius = 11.dp.toPx(), center = poiPos, style = Stroke(width = 1.5.dp.toPx()))
            drawLine(
                colors.danger,
                Offset(poiPos.x - 5.dp.toPx(), poiPos.y),
                Offset(poiPos.x + 5.dp.toPx(), poiPos.y),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawLine(
                colors.danger,
                Offset(poiPos.x, poiPos.y - 5.dp.toPx()),
                Offset(poiPos.x, poiPos.y + 5.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}

private fun filterPois(pois: List<MapPoi>, filter: MapFilter): List<MapPoi> = when (filter) {
    MapFilter.ALL -> pois
    MapFilter.MEDICAL -> pois.filter { it.kind == PoiKind.MEDICAL || it.kind == PoiKind.FIRST_AID }
    MapFilter.EXITS -> pois.filter { it.kind == PoiKind.EXIT }
    MapFilter.SHELTERS -> pois.filter { it.kind == PoiKind.SHELTER }
    MapFilter.CROWD -> pois.filter { it.kind == PoiKind.SHELTER }
}

private fun MapFilter.label(): String = when (this) {
    MapFilter.ALL -> "All"
    MapFilter.MEDICAL -> "Medical"
    MapFilter.EXITS -> "Exits"
    MapFilter.SHELTERS -> "Shelters"
    MapFilter.CROWD -> "Crowd"
}

private fun PoiKind.icon(): ImageVector = when (this) {
    PoiKind.MEDICAL -> Icons.Outlined.LocalHospital
    PoiKind.EXIT -> Icons.AutoMirrored.Outlined.Logout
    PoiKind.SHELTER -> Icons.Outlined.Home
    PoiKind.POLICE -> Icons.Outlined.Shield
    PoiKind.FIRST_AID -> Icons.Outlined.FavoriteBorder
}
