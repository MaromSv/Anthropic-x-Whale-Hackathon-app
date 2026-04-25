package com.example.emergency.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class EmergencyColors(
    val bg: Color,
    val surface: Color,
    val panel: Color,
    val panel2: Color,
    val line: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val accent: Color,
    val accentInk: Color,
    val danger: Color,
    val dangerSoft: Color,
    val safety: Color,
    val map: Color,
    val mapPath: Color,
    val mapWater: Color,
)

val LightEmergencyColors = EmergencyColors(
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    panel = Color(0xFFF7F7F5),
    panel2 = Color(0xFFEFEEEA),
    line = Color(0xFFE7E6E1),
    text = Color(0xFF1A1A1A),
    textDim = Color(0xFF6B6B66),
    textFaint = Color(0xFF9A9A93),
    accent = Color(0xFF1A1A1A),
    accentInk = Color(0xFFFFFFFF),
    danger = Color(0xFFC0392B),
    dangerSoft = Color(0xFFFBEDE8),
    safety = Color(0xFFD97706),
    map = Color(0xFFEEF0EC),
    mapPath = Color(0xFFD6DAD1),
    mapWater = Color(0xFFDFE7EC),
)

val LocalEmergencyColors = staticCompositionLocalOf { LightEmergencyColors }

@Immutable
data class SemanticColors(
    val youHere: Color,
    val youHereHalo: Color,
    val crowdWarningBg: Color,
    val crowdWarningInk: Color,
    val noteWarningBg: Color,
    val noteWarningInk: Color,
    val noteWarningIcon: Color,
    val safeBg: Color,
    val safeInk: Color,
    val statusOk: Color,
    val badgeNew: Color,
    val badgeNewInk: Color,
    val mapToolBg: Color,
    val mapToolInk: Color,
)

val LightSemanticColors = SemanticColors(
    youHere = Color(0xFF1D4ED8),
    youHereHalo = Color(0x2E1D4ED8),
    crowdWarningBg = Color(0xFFF4DDD0),
    crowdWarningInk = Color(0xFF7C2D12),
    noteWarningBg = Color(0xFFF2EBD8),
    noteWarningInk = Color(0xFF5B3A00),
    noteWarningIcon = Color(0xFF92400E),
    safeBg = Color(0xFFDDF0D6),
    safeInk = Color(0xFF15803D),
    statusOk = Color(0xFF16A34A),
    badgeNew = Color(0xFFC2410C),
    badgeNewInk = Color(0xFFFFFFFF),
    mapToolBg = Color(0xFFDDEFE9),
    mapToolInk = Color(0xFF0F766E),
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

@Immutable
data class ToolPalette(
    val bg: Color,
    val fg: Color,
)

@Immutable
data class ToolPalettes(
    val firstAid: ToolPalette,
    val abcCheck: ToolPalette,
    val map: ToolPalette,
    val getOut: ToolPalette,
)

// Tinted bg + saturated fg per design language. Chroma stays ≤ 0.18.
// Mapped from oklch tokens in DESIGN_LANGUAGE.md / state.jsx.
val LightToolPalettes = ToolPalettes(
    firstAid = ToolPalette(bg = Color(0xFFF9DCD3), fg = Color(0xFFC0392B)),
    abcCheck = ToolPalette(bg = Color(0xFFE0DCEF), fg = Color(0xFF4338CA)),
    map      = ToolPalette(bg = Color(0xFFD7EBE3), fg = Color(0xFF0F766E)),
    getOut   = ToolPalette(bg = Color(0xFFF1E5C9), fg = Color(0xFFB45309)),
)

val LocalToolPalettes = staticCompositionLocalOf { LightToolPalettes }

@Immutable
data class PackPalette(
    val light: Color,
    val dark: Color,
)

// Two-tone gradients per pack identity. Abstract shape only.
// Mapped from oklch tokens in packs.jsx.
val PackPalettes: Map<String, PackPalette> = mapOf(
    "kingsday-ams"    to PackPalette(Color(0xFFF0B97D), Color(0xFFD2723A)),
    "ams-base"        to PackPalette(Color(0xFFA9C3DE), Color(0xFF4A6FAB)),
    "flooding-nl"     to PackPalette(Color(0xFFA9C4DC), Color(0xFF36548E)),
    "power-outage-nl" to PackPalette(Color(0xFFEFB57E), Color(0xFF3F3850)),
    "rotterdam-base"  to PackPalette(Color(0xFF9DCCD0), Color(0xFF4D8AA0)),
)

val DefaultPackPalette = PackPalette(Color(0xFFE7E6E1), Color(0xFFB7B6B0))
