package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * HisabGhor design foundations.
 *
 * Everything visual is expressed through these tokens: colours, spacing,
 * radii, strokes and component dimensions. Screens should never invent their
 * own values except for genuinely one-off artwork.
 */

// ---------------------------------------------------------------------------
// Colour roles
// ---------------------------------------------------------------------------

/**
 * Brand palette. Colours are chosen for WCAG AA contrast on [surface] and
 * [canvas]; financial meaning is never carried by colour alone (screens pair
 * these with +/− signs, labels and icons).
 */
@Immutable
data class HgColors(
    val brand: Color,
    val brandPressed: Color,
    val brandDeep: Color,
    val brandTint: Color,
    val onBrandTint: Color,
    val canvas: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceSunken: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val income: Color,
    val incomeTint: Color,
    val expense: Color,
    val expenseTint: Color,
    val warning: Color,
    val warningTint: Color,
    val info: Color,
    val infoTint: Color,
    val scrim: Color
) {
    companion object {
        val Light = HgColors(
            brand = Color(0xFF087F5B),
            brandPressed = Color(0xFF066649),
            brandDeep = Color(0xFF04402E),
            brandTint = Color(0xFFDFF3EA),
            onBrandTint = Color(0xFF06472F),
            canvas = Color(0xFFF6F8F5),
            surface = Color(0xFFFFFFFF),
            surfaceMuted = Color(0xFFF0F4F0),
            surfaceSunken = Color(0xFFE8EFEA),
            ink = Color(0xFF17231D),
            inkSecondary = Color(0xFF5D6A62),
            inkTertiary = Color(0xFF7A867E),
            hairline = Color(0xFFE2E9E3),
            hairlineStrong = Color(0xFFCBD8CF),
            income = Color(0xFF087F5B),
            incomeTint = Color(0xFFE3F4EC),
            expense = Color(0xFFB03A46),
            expenseTint = Color(0xFFFBECEE),
            warning = Color(0xFF92600D),
            warningTint = Color(0xFFFBF2E0),
            info = Color(0xFF315F86),
            infoTint = Color(0xFFEDF3F8),
            scrim = Color(0x660B1712)
        )
    }
}

val LocalHgColors = staticCompositionLocalOf { HgColors.Light }

/** Short accessor so screens read naturally: `Hg.colors.inkSecondary`. */
object Hg {
    val colors: HgColors
        @Composable @ReadOnlyComposable get() = LocalHgColors.current
}

// Legacy colour names kept for call-site compatibility. They now resolve to
// the rebranded palette instead of the previous teal-leaning greens.
val Emerald600 = Color(0xFF087F5B)
val Emerald700 = Color(0xFF066649)
val Emerald500 = Color(0xFF0C8F67)
val Emerald50 = Color(0xFFDFF3EA)
val Ink900 = Color(0xFF17231D)
val Ink600 = Color(0xFF5D6A62)
val Danger600 = Color(0xFFB03A46)
val Amber700 = Color(0xFF92600D)

// ---------------------------------------------------------------------------
// Spacing
// ---------------------------------------------------------------------------

/**
 * 4 dp rhythm. Use [ScreenPadding] for the horizontal page gutter and these
 * steps for everything inside a page.
 */
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
}

val ScreenPadding = 20.dp

// ---------------------------------------------------------------------------
// Radii
// ---------------------------------------------------------------------------

/**
 * Deliberate radius hierarchy: small controls are never as round as large
 * surfaces, and nothing is an automatic pill.
 */
object Radius {
    val xs = 6.dp
    val sm = 8.dp
    val md = 10.dp
    val lg = 12.dp
    val xl = 16.dp
    val xxl = 20.dp
    val sheet = 28.dp
    val pill = 100.dp

    val control = RoundedCornerShape(lg)
    val field = RoundedCornerShape(lg)
    val card = RoundedCornerShape(xl)
    val panel = RoundedCornerShape(xxl)
    val chip = RoundedCornerShape(sm)
    val sheetTop = RoundedCornerShape(topStart = sheet, topEnd = sheet)
}

object Stroke {
    val hairline = 1.dp
    val focus = 2.dp
}

// ---------------------------------------------------------------------------
// Component dimensions
// ---------------------------------------------------------------------------

object Dimens {
    /** Minimum interactive size recommended by the accessibility guidelines. */
    val touchTarget = 48.dp
    val buttonHeight = 52.dp
    val fieldMinHeight = 56.dp
    val topBarHeight = 64.dp
    val bottomBarHeight = 68.dp
    val listRowMinHeight = 56.dp
    val iconSm = 18.dp
    val icon = 22.dp
    val iconLg = 26.dp
    val navIcon = 24.dp
    val avatar = 40.dp
}

val HgShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.lg),
    large = RoundedCornerShape(Radius.xl),
    extraLarge = RoundedCornerShape(Radius.xxl)
)

/** Semantic tones for badges, notices and progress indicators. */
enum class HgTone { Brand, Income, Expense, Warning, Neutral }
