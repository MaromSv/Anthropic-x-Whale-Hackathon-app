package com.example.emergency.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val MaterialLightScheme = lightColorScheme(
    background = LightEmergencyColors.bg,
    surface = LightEmergencyColors.surface,
    onBackground = LightEmergencyColors.text,
    onSurface = LightEmergencyColors.text,
    primary = LightEmergencyColors.accent,
    onPrimary = LightEmergencyColors.accentInk,
    outline = LightEmergencyColors.line,
    error = LightEmergencyColors.danger,
)

object EmergencyTheme {
    val colors: EmergencyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEmergencyColors.current

    val typography: EmergencyTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalEmergencyTypography.current

    val semantic: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current

    val toolPalettes: ToolPalettes
        @Composable
        @ReadOnlyComposable
        get() = LocalToolPalettes.current
}

@Composable
fun EmergencyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalEmergencyColors provides LightEmergencyColors,
        LocalEmergencyTypography provides DefaultEmergencyTypography,
        LocalSemanticColors provides LightSemanticColors,
        LocalToolPalettes provides LightToolPalettes,
    ) {
        MaterialTheme(
            colorScheme = MaterialLightScheme,
            content = content,
        )
    }
}
