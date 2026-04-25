package com.example.emergency.ui.screen.cpr

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.state.CprIllustrationKind
import com.example.emergency.ui.state.CprStep
import com.example.emergency.ui.state.CprWalkthroughSteps
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun CprWalkthroughScreen(
    onBack: () -> Unit,
    steps: List<CprStep> = CprWalkthroughSteps,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    var index by rememberSaveable { mutableStateOf(0) }
    val step = steps[index]
    val total = steps.size
    val isLast = index == total - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        TopBar(
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
            Text(
                text = step.eyebrow.uppercase(),
                style = typography.eyebrow,
                color = colors.textDim,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = step.title,
                style = typography.sectionTitle.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.text,
            )
            Spacer(Modifier.height(16.dp))

            IllustrationPanel(kind = step.illustration)

            Spacer(Modifier.height(16.dp))

            step.bullets.forEach { bullet ->
                BulletRow(text = bullet)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(6.dp))
            SafetyNote(text = step.note)
            Spacer(Modifier.height(8.dp))
        }

        BottomActionBar(
            showBack = index > 0,
            isLast = isLast,
            onPrev = { if (index > 0) index-- },
            onNext = { if (!isLast) index++ },
            onFinish = onBack,
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TopBar(
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
            text = "CPR \u00B7 adult",
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
private fun IllustrationPanel(kind: CprIllustrationKind) {
    val colors = EmergencyTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.panel)
            .padding(16.dp),
    ) {
        if (kind == CprIllustrationKind.METRONOME) {
            CprMetronome()
        } else {
            CprIllustration(kind = kind)
        }
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
private fun BottomActionBar(
    showBack: Boolean,
    isLast: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Box(
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 18.dp),
        ) {
            if (showBack) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.dp, colors.line, CircleShape)
                        .clickable(onClick = onPrev),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Previous step",
                        tint = colors.text,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(EmergencyShapes.full)
                    .background(colors.accent)
                    .clickable(onClick = if (isLast) onFinish else onNext),
            ) {
                if (isLast) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Finish",
                            style = typography.listItem.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                            color = colors.accentInk,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Text(
                        text = "Done \u2014 next step",
                        style = typography.listItem.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        color = colors.accentInk,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
