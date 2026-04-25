package com.example.emergency.ui.screen

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.screen.common.GroupedListContainer
import com.example.emergency.ui.screen.common.GroupedListDivider
import com.example.emergency.ui.screen.common.ScreenSectionHeader
import com.example.emergency.ui.screen.common.SubScreenTopBar
import com.example.emergency.ui.state.CrowdDensity
import com.example.emergency.ui.state.ExitStatus
import com.example.emergency.ui.state.GetOutExit
import com.example.emergency.ui.state.GetOutUiState
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun GetOutScreen(
    state: GetOutUiState,
    onBack: () -> Unit = {},
    onStartNavigation: () -> Unit = {},
    onExitClick: (GetOutExit) -> Unit = {},
) {
    val colors = EmergencyTheme.colors

    val recommended = state.exits.first { it.recommended }
    val others = state.exits.filter { !it.recommended }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        SubScreenTopBar(title = state.title, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            DensityCard(
                density = state.density,
                densityLabel = state.densityLabel,
                locationLabel = state.locationLabel,
                densityHint = state.densityHint,
            )

            Spacer(modifier = Modifier.height(18.dp))

            RecommendedExitCard(exit = recommended, onStartNavigation = onStartNavigation)

            Spacer(modifier = Modifier.height(22.dp))

            ScreenSectionHeader(text = "OTHER EXITS")
            Spacer(modifier = Modifier.height(8.dp))

            GroupedListContainer {
                others.forEachIndexed { index, exit ->
                    ExitRow(exit = exit, onClick = { onExitClick(exit) })
                    if (index < others.lastIndex) {
                        GroupedListDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun densityColor(density: CrowdDensity): Color {
    val colors = EmergencyTheme.colors
    val semantic = EmergencyTheme.semantic
    return when (density) {
        CrowdDensity.LOW -> semantic.safeInk
        CrowdDensity.MEDIUM -> semantic.noteWarningIcon
        CrowdDensity.HIGH -> semantic.crowdWarningInk
        CrowdDensity.NONE -> colors.textDim
    }
}

private fun filledForDensity(density: CrowdDensity): Int = when (density) {
    CrowdDensity.NONE -> 0
    CrowdDensity.LOW -> 4
    CrowdDensity.MEDIUM -> 10
    CrowdDensity.HIGH -> 18
}

@Composable
private fun DensityCard(
    density: CrowdDensity,
    densityLabel: String,
    locationLabel: String,
    densityHint: String,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val tone = densityColor(density)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.card)
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Crowd density",
                    style = typography.eyebrow,
                    color = colors.textFaint,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = locationLabel,
                    style = typography.monoMicro,
                    color = colors.textFaint,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = densityLabel.uppercase(),
                style = typography.sectionTitle.copy(fontSize = 22.sp),
                color = tone,
            )
            Spacer(modifier = Modifier.height(14.dp))
            DotGrid(filled = filledForDensity(density), fillColor = tone)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = densityHint,
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
        }
    }
}

@Composable
private fun DotGrid(
    filled: Int,
    fillColor: Color,
    columns: Int = 5,
    rows: Int = 4,
) {
    val colors = EmergencyTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(columns) { colIndex ->
                    val index = rowIndex * columns + colIndex
                    val isFilled = index < filled
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(EmergencyShapes.full)
                            .background(if (isFilled) fillColor else colors.panel2),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendedExitCard(
    exit: GetOutExit,
    onStartNavigation: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.accent, EmergencyShapes.card)
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(EmergencyShapes.full)
                        .background(colors.accent)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "RECOMMENDED",
                        style = typography.monoMicro.copy(fontSize = 10.sp),
                        color = colors.accentInk,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = exit.crowdLabel,
                    style = typography.helper.copy(fontSize = 11.sp),
                    color = colors.textFaint,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = exit.name,
                style = typography.sectionTitle.copy(fontSize = 20.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exit.distanceLabel,
                    style = typography.monoMicro,
                    color = colors.textDim,
                )
                Text(
                    text = " \u00B7 ",
                    style = typography.helper.copy(fontSize = 12.sp),
                    color = colors.textFaint,
                )
                Text(
                    text = exit.etaLabel,
                    style = typography.monoMicro,
                    color = colors.textDim,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = exit.routeSummary,
                style = typography.helper.copy(fontSize = 13.sp),
                color = colors.textDim,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(EmergencyShapes.full)
                    .background(colors.accent)
                    .clickable(onClick = onStartNavigation)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
                        contentDescription = null,
                        tint = colors.accentInk,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Start navigation",
                        style = typography.listItem.copy(fontSize = 14.sp),
                        color = colors.accentInk,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitRow(
    exit: GetOutExit,
    onClick: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val semantic = EmergencyTheme.semantic

    val crowdColor = when (exit.status) {
        ExitStatus.CLEAR -> semantic.safeInk
        ExitStatus.BUSY -> semantic.noteWarningIcon
        ExitStatus.FULL -> semantic.crowdWarningInk
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exit.name,
                style = typography.listItem.copy(fontSize = 14.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = exit.routeSummary,
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = exit.distanceLabel,
                style = typography.monoMicro,
                color = colors.textDim,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = exit.crowdLabel,
                style = typography.helper.copy(fontSize = 11.sp),
                color = crowdColor,
            )
        }
    }
}
