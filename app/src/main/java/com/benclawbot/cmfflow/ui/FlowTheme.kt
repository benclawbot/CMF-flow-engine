package com.benclawbot.cmfflow.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FlowColors = lightColorScheme(
    primary = Color(0xFF6847C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E0FF),
    onPrimaryContainer = Color(0xFF25104F),
    secondary = Color(0xFF5F5B73),
    secondaryContainer = Color(0xFFE9E7F2),
    background = Color(0xFFF9F7FC),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF0EDF5),
    outline = Color(0xFF7B7585),
)

@Composable
fun FlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FlowColors, content = content)
}
