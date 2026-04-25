package com.example.emergency.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.screen.common.GroupedListContainer
import com.example.emergency.ui.screen.common.GroupedListDivider
import com.example.emergency.ui.screen.common.ScreenSectionHeader
import com.example.emergency.ui.screen.common.SubScreenTopBar
import com.example.emergency.ui.state.FirstAidTopic
import com.example.emergency.ui.state.FirstAidTopicId
import com.example.emergency.ui.state.FirstAidUiState
import com.example.emergency.ui.state.FirstAidUrgency
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun FirstAidScreen(
    state: FirstAidUiState,
    onBack: () -> Unit = {},
    onAbcCheckClick: () -> Unit = {},
    onTopicClick: (FirstAidTopic) -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    val critical = state.topics.filter { it.urgency == FirstAidUrgency.CRITICAL }
    val general = state.topics.filter { it.urgency == FirstAidUrgency.GENERAL }

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

            Spacer(modifier = Modifier.height(18.dp))
            AbcShortcutCard(onClick = onAbcCheckClick)

            if (critical.isNotEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))
                ScreenSectionHeader(text = "CRITICAL")
                Spacer(modifier = Modifier.height(8.dp))
                TopicList(topics = critical, onTopicClick = onTopicClick)
            }

            if (general.isNotEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))
                ScreenSectionHeader(text = "GENERAL")
                Spacer(modifier = Modifier.height(8.dp))
                TopicList(topics = general, onTopicClick = onTopicClick)
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun AbcShortcutCard(onClick: () -> Unit) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(EmergencyShapes.full)
                .background(colors.accent),
        ) {
            Icon(
                imageVector = Icons.Outlined.MedicalServices,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Run ABC check",
                style = typography.listItem.copy(fontSize = 15.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Airway \u00B7 Breathing \u00B7 Circulation",
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TopicList(
    topics: List<FirstAidTopic>,
    onTopicClick: (FirstAidTopic) -> Unit,
) {
    GroupedListContainer {
        topics.forEachIndexed { index, topic ->
            TopicRow(topic = topic, onClick = { onTopicClick(topic) })
            if (index < topics.lastIndex) {
                GroupedListDivider()
            }
        }
    }
}

@Composable
private fun TopicRow(
    topic: FirstAidTopic,
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
                imageVector = topic.id.icon(),
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = topic.title,
                style = typography.listItem.copy(fontSize = 14.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = topic.summary,
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = topic.readTimeLabel,
                    style = typography.monoMicro,
                    color = colors.textFaint,
                )
                topic.tag?.let { tag ->
                    Text(
                        text = " \u00B7 ",
                        style = typography.helper.copy(fontSize = 11.sp),
                        color = colors.textFaint,
                    )
                    Text(
                        text = tag,
                        style = typography.helper.copy(fontSize = 11.sp),
                        color = colors.textFaint,
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun FirstAidTopicId.icon(): ImageVector = when (this) {
    FirstAidTopicId.CPR -> Icons.Outlined.Favorite
    FirstAidTopicId.BLEEDING -> Icons.Outlined.Healing
    FirstAidTopicId.CHOKING -> Icons.Outlined.Air
    FirstAidTopicId.BURNS -> Icons.Outlined.LocalFireDepartment
    FirstAidTopicId.DROWNING -> Icons.Outlined.Water
    FirstAidTopicId.HEAT_STROKE -> Icons.Outlined.Thermostat
    FirstAidTopicId.HYPOTHERMIA -> Icons.Outlined.AcUnit
    FirstAidTopicId.SEIZURE -> Icons.Outlined.Bolt
}
