package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.state.DrawerEntry
import com.example.emergency.ui.state.DrawerItemId
import com.example.emergency.ui.state.DrawerUiState
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun NavDrawerSheet(
    state: DrawerUiState,
    onItemClick: (DrawerItemId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmergencyTheme.colors
    val semantic = EmergencyTheme.semantic
    val typography = EmergencyTheme.typography

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(290.dp)
            .background(colors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        ) {
            Text(
                text = state.title,
                style = typography.appBarTitle.copy(fontSize = 18.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (state.statusOk) semantic.statusOk else colors.danger),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = state.statusLabel,
                    style = typography.helper.copy(fontSize = 12.sp),
                    color = colors.textDim,
                )
            }
        }

        HorizontalLine(color = colors.line)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            state.entries.forEach { entry ->
                when (entry) {
                    is DrawerEntry.Item -> NavDrawerItem(
                        item = entry,
                        onClick = { onItemClick(entry.id) },
                    )
                    DrawerEntry.Divider -> Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(1.dp)
                            .background(colors.line),
                    )
                }
            }
        }

        HorizontalLine(color = colors.line)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = state.footer,
                style = typography.helper.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = colors.textFaint,
            )
        }
    }
}

@Composable
private fun NavDrawerItem(
    item: DrawerEntry.Item,
    onClick: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val semantic = EmergencyTheme.semantic
    val typography = EmergencyTheme.typography

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Icon(
            imageVector = item.id.icon(),
            contentDescription = null,
            tint = colors.textDim,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = typography.listItem.copy(fontSize = 14.sp),
                color = colors.text,
            )
            item.sub?.let { sub ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub,
                    style = typography.helper.copy(fontSize = 12.sp),
                    color = colors.textDim,
                )
            }
        }
        item.badge?.let { badge ->
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .clip(EmergencyShapes.full)
                    .background(semantic.badgeNew)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = badge,
                    style = typography.monoMicro.copy(fontSize = 9.sp, letterSpacing = 0.45.sp),
                    color = semantic.badgeNewInk,
                )
            }
        }
    }
}

@Composable
private fun HorizontalLine(color: androidx.compose.ui.graphics.Color) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

private fun DrawerItemId.icon(): ImageVector = when (this) {
    DrawerItemId.CONVERSATIONS -> Icons.AutoMirrored.Outlined.List
    DrawerItemId.MAP -> Icons.Outlined.Map
    DrawerItemId.FIRST_AID -> Icons.Outlined.FavoriteBorder
    DrawerItemId.DATA_PACKS -> Icons.Outlined.Layers
    DrawerItemId.SETTINGS -> Icons.Outlined.Shield
    DrawerItemId.PERSONAL_INFO -> Icons.Outlined.Person
}
