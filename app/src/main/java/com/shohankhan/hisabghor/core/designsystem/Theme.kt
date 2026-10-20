package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * HisabGhor theme — light only, by product decision.
 *
 * The Material colour scheme is populated from the brand roles in
 * [HgColors] so that stock Material 3 components inherit the identity
 * instead of looking like a default template. All custom surfaces read
 * colours through [LocalHgColors].
 */
private val C = HgColors.Light

private val HisabGhorColorScheme = lightColorScheme(
    primary = C.brand,
    onPrimary = Color.White,
    primaryContainer = C.brandTint,
    onPrimaryContainer = C.onBrandTint,
    inversePrimary = Color(0xFF6FD5AE),

    secondary = C.brandDeep,
    onSecondary = Color.White,
    secondaryContainer = C.surfaceMuted,
    onSecondaryContainer = C.ink,

    tertiary = C.info,
    onTertiary = Color.White,
    tertiaryContainer = C.infoTint,
    onTertiaryContainer = C.info,

    background = C.canvas,
    onBackground = C.ink,
    surface = C.surface,
    onSurface = C.ink,
    surfaceVariant = C.surfaceMuted,
    onSurfaceVariant = C.inkSecondary,
    surfaceTint = C.brand,

    inverseSurface = C.ink,
    inverseOnSurface = C.canvas,

    error = C.expense,
    onError = Color.White,
    errorContainer = C.expenseTint,
    onErrorContainer = C.expense,

    outline = C.hairlineStrong,
    outlineVariant = C.hairline,
    scrim = C.scrim,

    surfaceBright = C.surface,
    surfaceDim = C.surfaceSunken,
    surfaceContainerLowest = C.surface,
    surfaceContainerLow = Color(0xFFFAFCFA),
    surfaceContainer = C.surfaceMuted,
    surfaceContainerHigh = C.surfaceSunken,
    surfaceContainerHighest = Color(0xFFE1E9E3)
)

private val HisabGhorSelectionColors = TextSelectionColors(
    handleColor = C.brand,
    backgroundColor = C.brandTint.copy(alpha = 0.75f)
)

@Composable
fun HisabGhorTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalHgColors provides C,
        LocalTextSelectionColors provides HisabGhorSelectionColors
    ) {
        MaterialTheme(
            colorScheme = HisabGhorColorScheme,
            typography = HisabGhorTypography,
            shapes = HgShapes,
            content = content
        )
    }
}
