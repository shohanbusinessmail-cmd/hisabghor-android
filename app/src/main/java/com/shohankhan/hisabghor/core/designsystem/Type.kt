package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shohankhan.hisabghor.R

/**
 * HisabGhor typography.
 *
 * Noto Sans Bengali (SIL OFL 1.1) is bundled locally so Bengali shaping is
 * identical on every device, offline, including on cheap phones whose OEM
 * Bengali fallback fonts are often incomplete. The same family also carries
 * Latin glyphs, which keeps mixed Bengali/Latin strings optically consistent.
 *
 * Bengali script needs more vertical room than Latin: matra, কার and ফলা can
 * reach far above and below the baseline. Line heights here are deliberately
 * generous (≈1.45–1.55×) and every style leaves extra room for accessibility
 * font scaling. Letterspacing is slightly tightened only on large headings,
 * where Noto Bengali otherwise looks loose.
 */
val BengaliFontFamily = FontFamily(
    Font(R.font.noto_sans_bengali_regular, FontWeight.Normal),
    Font(R.font.noto_sans_bengali_medium, FontWeight.Medium),
    Font(R.font.noto_sans_bengali_semibold, FontWeight.SemiBold),
    Font(R.font.noto_sans_bengali_bold, FontWeight.Bold)
)

/** Extra amount styles used by financial surfaces (not part of M3's scale). */
object HgAmountType {
    /** Hero balance on the dashboard. */
    val hero = TextStyle(
        fontFamily = BengaliFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.6).sp
    )
    /** Large summary figures (account balance, report totals). */
    val large = TextStyle(
        fontFamily = BengaliFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp
    )
    /** Amounts in list rows and compact stats. */
    val medium = TextStyle(
        fontFamily = BengaliFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    /** Small amounts in dense rows. */
    val small = TextStyle(
        fontFamily = BengaliFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
}

val HisabGhorTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 46.sp, lineHeight = 58.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 40.sp, lineHeight = 52.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp
    ),
    displaySmall = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 32.sp, lineHeight = 44.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 26.sp, lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 23.sp, lineHeight = 33.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 20.sp, lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 18.sp, lineHeight = 27.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 14.sp, lineHeight = 21.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 16.sp, lineHeight = 26.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 14.sp, lineHeight = 23.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 12.sp, lineHeight = 19.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 12.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = BengaliFontFamily, fontSize = 11.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
)
