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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.screen.abc.AbcIllustration
import com.example.emergency.ui.state.AbcCheckUiState
import com.example.emergency.ui.state.AbcStep
import com.example.emergency.ui.state.AbcStepId
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme
import com.example.emergency.ui.theme.JetBrainsMonoFamily

@Composable
fun AbcCheckScreen(
    state: AbcCheckUiState,
    onBack: () -> Unit = {},
    onStartCpr: () -> Unit = {},
) {
    val colors = EmergencyTheme.colors
    val steps = state.steps
    val total = steps.size

    var index by rememberSaveable { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<AbcStepId, Boolean>() }
    val done = index >= total

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        if (done) {
            ResultsView(
                steps = steps,
                answers = answers,
                onBack = onBack,
                onStartCpr = onStartCpr,
                onRunAgain = {
                    answers.clear()
                    index = 0
                },
            )
        } else {
            val step = steps[index]
            TopBar(
                title = state.title,
                current = index + 1,
                total = total,
                onBack = onBack,
            )
            ProgressDots(current = index, total = total)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp),
            ) {
                StepHeader(step = step)
                Spacer(Modifier.height(16.dp))
                IllustrationPanel(step = step)
                Spacer(Modifier.height(16.dp))
                step.bullets.forEach { b ->
                    BulletRow(text = b)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(6.dp))
                SafetyNote(text = step.note)
                Spacer(Modifier.height(8.dp))
            }

            YesNoBar(
                question = step.question,
                onNo = {
                    answers[step.id] = false
                    index += 1
                },
                onYes = {
                    answers[step.id] = true
                    index += 1
                },
            )
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TopBar(
    title: String,
    current: Int,
    total: Int,
    onBack: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
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
            style = typography.appBarTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            color = colors.text,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        Text(
            text = "$current/$total",
            style = typography.monoMicro.copy(fontSize = 12.sp),
            color = colors.textDim,
        )
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    val colors = EmergencyTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
    ) {
        for (i in 0 until total) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= current) colors.text else colors.panel2),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.line),
    )
}

@Composable
private fun StepHeader(step: AbcStep) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Text(
        text = step.eyebrow.uppercase(),
        style = typography.eyebrow,
        color = colors.textDim,
    )
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = step.letter,
            fontFamily = FontFamily.Serif,
            fontSize = 76.sp,
            fontWeight = FontWeight.Light,
            color = colors.text,
            modifier = Modifier.padding(end = 16.dp),
        )
        Text(
            text = step.title,
            style = typography.sectionTitle.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 26.sp,
            ),
            color = colors.text,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun IllustrationPanel(step: AbcStep) {
    val colors = EmergencyTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.panel)
            .padding(16.dp),
    ) {
        AbcIllustration(kind = step.illustration)
    }
}

@Composable
private fun BulletRow(text: String) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(colors.text),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = typography.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = colors.text,
        )
    }
}

@Composable
private fun SafetyNote(text: String) {
    val semantic = EmergencyTheme.semantic
    val typography = EmergencyTheme.typography
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(semantic.noteWarningBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = semantic.noteWarningIcon,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = typography.helper.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = semantic.noteWarningInk,
        )
    }
}

@Composable
private fun YesNoBar(
    question: String,
    onNo: () -> Unit,
    onYes: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line),
        )
        Text(
            text = question,
            style = typography.helper.copy(fontSize = 13.sp),
            color = colors.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(EmergencyShapes.full)
                    .border(1.dp, colors.line, EmergencyShapes.full)
                    .clickable(onClick = onNo),
            ) {
                Text(
                    text = "No",
                    style = typography.listItem.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = colors.text,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(EmergencyShapes.full)
                    .background(colors.accent)
                    .clickable(onClick = onYes),
            ) {
                Text(
                    text = "Yes",
                    style = typography.listItem.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = colors.accentInk,
                )
            }
        }
    }
}

@Composable
private fun ResultsView(
    steps: List<AbcStep>,
    answers: Map<AbcStepId, Boolean>,
    onBack: () -> Unit,
    onStartCpr: () -> Unit,
    onRunAgain: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val anyFail = answers.values.any { it == false }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = colors.text,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "ABC \u00B7 result",
            style = typography.appBarTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            color = colors.text,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.line),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 8.dp),
    ) {
        Text(
            text = "Result".uppercase(),
            style = typography.eyebrow,
            color = colors.textDim,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (anyFail) "Start CPR and call for help." else "Vitals look stable \u2014 keep monitoring.",
            style = typography.sectionTitle.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 30.sp,
            ),
            color = colors.text,
        )
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            steps.forEach { s ->
                ResultRow(step = s, ok = answers[s.id] == true)
            }
        }
        if (anyFail) {
            Spacer(Modifier.height(16.dp))
            FailHint()
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(EmergencyShapes.full)
                    .border(1.dp, colors.line, EmergencyShapes.full)
                    .clickable(onClick = onRunAgain),
            ) {
                Text(
                    text = "Run again",
                    style = typography.listItem.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = colors.text,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(EmergencyShapes.full)
                    .background(if (anyFail) colors.danger else colors.accent)
                    .clickable(onClick = if (anyFail) onStartCpr else onBack),
            ) {
                Text(
                    text = if (anyFail) "Start CPR" else "Done",
                    style = typography.listItem.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = Color(0xFFFFFFFF),
                )
            }
        }
    }
}

@Composable
private fun ResultRow(step: AbcStep, ok: Boolean) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val pillBg = if (ok) Color(0xFFDDF0D6) else Color(0xFFF6DCD0)
    val pillInk = if (ok) Color(0xFF15803D) else Color(0xFFB91C1C)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.panel)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = step.letter,
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp,
            color = colors.text,
            modifier = Modifier.width(22.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = step.title.removeSuffix("."),
            style = typography.body.copy(fontSize = 14.sp),
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(EmergencyShapes.full)
                .background(pillBg)
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            Text(
                text = if (ok) "OK" else "NOT OK",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = pillInk,
            )
        }
    }
}

@Composable
private fun FailHint() {
    val typography = EmergencyTheme.typography
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFBEDE8))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = Color(0xFFB91C1C),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Tap below to start the timed CPR walkthrough now. Call 112 \u2014 put it on speaker.",
            style = typography.helper.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = Color(0xFF7F1D1D),
        )
    }
}
