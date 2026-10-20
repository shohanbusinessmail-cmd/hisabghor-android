package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium white + emerald identity (light mode only, by product decision).
val Emerald600 = Color(0xFF0B7A4B)
val Emerald700 = Color(0xFF065C38)
val Emerald500 = Color(0xFF11998E)
val Emerald50 = Color(0xFFE8F6F0)
val Ink900 = Color(0xFF14201A)
val Ink600 = Color(0xFF4A5A52)
val Danger600 = Color(0xFFC62828)
val Amber700 = Color(0xFFB26A00)

private val LightColors = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Emerald50,
    onPrimaryContainer = Emerald700,
    secondary = Emerald500,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Color(0xFFF2F7F4),
    onSurfaceVariant = Ink600,
    error = Danger600,
    onError = Color.White,
    outline = Color(0xFFC8D8CF)
)

@Composable
fun HisabGhorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = HisabGhorTypography,
        content = content
    )
}
