package com.benclawbot.cmfflow.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FlowLightColors = lightColorScheme(
    primary = Color(0xFF6D4DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E1FF),
    onPrimaryContainer = Color(0xFF24134F),
    secondary = Color(0xFF7658A6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E8FF),
    onSecondaryContainer = Color(0xFF2D174D),
    tertiary = Color(0xFFB14D88),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8EB),
    onTertiaryContainer = Color(0xFF3C0929),
    background = Color(0xFFF8F6FC),
    onBackground = Color(0xFF211F28),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF211F28),
    surfaceVariant = Color(0xFFF0ECF6),
    onSurfaceVariant = Color(0xFF66606F),
    outline = Color(0xFF8A8293),
    outlineVariant = Color(0xFFD9D2E2),
)

private val FlowDarkColors = darkColorScheme(
    primary = Color(0xFFCDBDFF),
    onPrimary = Color(0xFF321A86),
    primaryContainer = Color(0xFF4A329D),
    onPrimaryContainer = Color(0xFFE9E1FF),
    secondary = Color(0xFFD4B9FF),
    onSecondary = Color(0xFF3E2662),
    secondaryContainer = Color(0xFF563D78),
    onSecondaryContainer = Color(0xFFF0E8FF),
    tertiary = Color(0xFFFFAFD5),
    onTertiary = Color(0xFF641246),
    tertiaryContainer = Color(0xFF842D61),
    onTertiaryContainer = Color(0xFFFFD8EB),
    background = Color(0xFF141219),
    onBackground = Color(0xFFE8E1EB),
    surface = Color(0xFF1B181F),
    onSurface = Color(0xFFE8E1EB),
    surfaceVariant = Color(0xFF302C35),
    onSurfaceVariant = Color(0xFFCBC3CF),
    outline = Color(0xFF958E9A),
    outlineVariant = Color(0xFF49434D),
)

private val FlowTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
)

private val FlowShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun FlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) FlowDarkColors else FlowLightColors,
        typography = FlowTypography,
        shapes = FlowShapes,
        content = content,
    )
}
