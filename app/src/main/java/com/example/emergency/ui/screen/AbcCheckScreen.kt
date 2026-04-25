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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.screen.common.SubScreenTopBar
import com.example.emergency.ui.state.AbcCheckUiState
import com.example.emergency.ui.state.AbcStep
import com.example.emergency.ui.state.AbcStepId
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun AbcCheckScreen(
    state: AbcCheckUiState,
    onBack: () -> Unit = {},
    onStepToggle: (AbcStepId, Boolean) -> Unit = { _, _ -> },
    onStartCpr: () -> Unit = {},
) {
    val colors = EmergencyTheme.colors

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

            NoteCard(text = state.safetyNote)

            Spacer(modifier = Modifier.height(22.dp))

            state.steps.forEachIndexed { index, step ->
                StepCard(
                    step = step,
                    checked = step.id in state.checked,
                    onToggle = { value -> onStepToggle(step.id, value) },
                )
                if (index < state.steps.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            StartCprButton(onClick = onStartCpr)
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun NoteCard(text: String) {
    val semantic = EmergencyTheme.semantic
    val typography = EmergencyTheme.typography

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.card)
            .background(semantic.noteWarningBg)
            .padding(14.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = semantic.noteWarningIcon,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = text,
            style = typography.helper.copy(fontSize = 12.sp, lineHeight = 18.sp),
            color = semantic.noteWarningInk,
        )
    }
}

@Composable
private fun StepCard(
    step: AbcStep,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.card)
            .clickable { onToggle(!checked) }
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(EmergencyShapes.full)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = step.number.toString(),
                        style = typography.listItem.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = colors.accentInk,
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.title,
                        style = typography.sectionTitle.copy(fontSize = 18.sp),
                        color = colors.text,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = step.subtitle,
                        style = typography.helper.copy(fontSize = 12.sp),
                        color = colors.textDim,
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accentInk,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.surface,
                        uncheckedTrackColor = colors.panel,
                        uncheckedBorderColor = colors.line,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            step.checks.forEach { check ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 10.dp)
                            .size(6.dp)
                            .clip(EmergencyShapes.full)
                            .background(colors.text),
                    )
                    Text(
                        text = check,
                        style = typography.body.copy(fontSize = 14.sp),
                        color = colors.text,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "LOOK FOR",
                style = typography.eyebrow,
                color = colors.textFaint,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = step.lookFor,
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
        }
    }
}

@Composable
private fun StartCprButton(onClick: () -> Unit) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.full)
            .background(colors.danger)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "If no breathing — start CPR now",
            style = typography.helper.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color(0xFFFFFFFF),
        )
    }
}
