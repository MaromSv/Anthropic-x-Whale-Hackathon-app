package com.example.emergency.ui.screen.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun HeroSection(
    greeting: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = greeting,
            style = typography.greeting,
            color = colors.text,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = typography.helper,
            color = colors.textDim,
        )
    }
}
