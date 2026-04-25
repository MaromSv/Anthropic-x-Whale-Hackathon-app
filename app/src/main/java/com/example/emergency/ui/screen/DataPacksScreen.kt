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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.state.DataPack
import com.example.emergency.ui.state.DataPackStatus
import com.example.emergency.ui.state.DataPacksUiState
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun DataPacksScreen(
    state: DataPacksUiState,
    onBack: () -> Unit = {},
    onPackAction: (DataPack) -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    val installed = state.packs.filter { it.status != DataPackStatus.NOT_INSTALLED }
    val available = state.packs.filter { it.status == DataPackStatus.NOT_INSTALLED }

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
            Text(
                text = state.subtitle,
                style = typography.body.copy(fontSize = 14.sp),
                color = colors.textDim,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.storageLabel,
                style = typography.monoMicro,
                color = colors.textFaint,
            )

            if (installed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel(text = "INSTALLED")
                Spacer(modifier = Modifier.height(8.dp))
                PackList(packs = installed, onPackAction = onPackAction)
            }

            if (available.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel(text = "AVAILABLE")
                Spacer(modifier = Modifier.height(8.dp))
                PackList(packs = available, onPackAction = onPackAction)
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PackList(
    packs: List<DataPack>,
    onPackAction: (DataPack) -> Unit,
) {
    val colors = EmergencyTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.card),
    ) {
        packs.forEachIndexed { index, pack ->
            PackRow(pack = pack, onClick = { onPackAction(pack) })
            if (index < packs.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(1.dp)
                        .background(colors.line),
                )
            }
        }
    }
}

@Composable
private fun PackRow(
    pack: DataPack,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pack.name,
                style = typography.listItem.copy(fontSize = 14.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pack.description,
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pack.sizeLabel,
                    style = typography.monoMicro,
                    color = colors.textFaint,
                )
                pack.updatedLabel?.let { label ->
                    DotSeparator()
                    Text(
                        text = label,
                        style = typography.helper.copy(fontSize = 11.sp),
                        color = colors.textFaint,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        PackAction(status = pack.status)
    }
}

@Composable
private fun PackAction(status: DataPackStatus) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    when (status) {
        DataPackStatus.INSTALLED -> StatusChip(
            icon = Icons.Outlined.Check,
            label = "Installed",
            tint = colors.textDim,
            background = colors.panel,
        )
        DataPackStatus.UPDATE_AVAILABLE -> ActionChip(
            icon = Icons.Outlined.Sync,
            label = "Update",
            tint = colors.accentInk,
            background = colors.accent,
        )
        DataPackStatus.DOWNLOADING -> Text(
            text = "Downloading\u2026",
            style = typography.helper.copy(fontSize = 12.sp),
            color = colors.textDim,
        )
        DataPackStatus.NOT_INSTALLED -> ActionChip(
            icon = Icons.Outlined.Download,
            label = "Get",
            tint = colors.text,
            background = colors.panel,
        )
    }
}

@Composable
private fun StatusChip(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
) {
    val typography = EmergencyTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(EmergencyShapes.full)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp),
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            style = typography.helper.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = tint,
        )
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
) {
    StatusChip(icon = icon, label = label, tint = tint, background = background)
}

@Composable
private fun DotSeparator() {
    val colors = EmergencyTheme.colors
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(2.dp)
            .background(colors.textFaint, EmergencyShapes.full),
    )
}

@Composable
private fun SectionLabel(text: String) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Text(
        text = text,
        style = typography.eyebrow,
        color = colors.textFaint,
    )
}

@Composable
internal fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

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
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = colors.text,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            style = typography.appBarTitle,
            color = colors.text,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp),
        )
    }
}

@Composable
internal fun ScreenSectionHeader(text: String, modifier: Modifier = Modifier) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Text(
        text = text,
        style = typography.eyebrow,
        color = colors.textFaint,
        modifier = modifier,
    )
}

@Composable
internal fun GroupedListContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = EmergencyTheme.colors
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.card),
    ) {
        content()
    }
}

@Composable
internal fun GroupedListDivider() {
    val colors = EmergencyTheme.colors
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(colors.line),
    )
}
