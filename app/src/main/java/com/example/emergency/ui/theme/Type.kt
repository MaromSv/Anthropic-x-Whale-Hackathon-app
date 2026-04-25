@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.example.emergency.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.emergency.R

private fun wght(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))

val InterFamily = FontFamily(
    Font(R.font.inter, weight = FontWeight.Light, variationSettings = wght(300)),
    Font(R.font.inter, weight = FontWeight.Normal, variationSettings = wght(400)),
    Font(R.font.inter, weight = FontWeight.Medium, variationSettings = wght(500)),
    Font(R.font.inter, weight = FontWeight.SemiBold, variationSettings = wght(600)),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, weight = FontWeight.Normal, variationSettings = wght(400)),
    Font(R.font.jetbrains_mono, weight = FontWeight.Medium, variationSettings = wght(500)),
)

@Immutable
data class EmergencyTypography(
    val hero: TextStyle,
    val greeting: TextStyle,
    val sectionTitle: TextStyle,
    val appBarTitle: TextStyle,
    val body: TextStyle,
    val listItem: TextStyle,
    val helper: TextStyle,
    val eyebrow: TextStyle,
    val monoMicro: TextStyle,
)

val DefaultEmergencyTypography = EmergencyTypography(
    hero = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    greeting = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 31.sp,
    ),
    sectionTitle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    appBarTitle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
    ),
    body = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    listItem = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    helper = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    eyebrow = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.88.sp,
    ),
    monoMicro = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.55.sp,
    ),
)

val LocalEmergencyTypography = staticCompositionLocalOf { DefaultEmergencyTypography }
