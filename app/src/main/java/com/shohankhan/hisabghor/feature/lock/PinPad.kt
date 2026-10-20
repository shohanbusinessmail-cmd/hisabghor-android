package com.shohankhan.hisabghor.feature.lock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.Space
import com.shohankhan.hisabghor.core.designsystem.Stroke
import com.shohankhan.hisabghor.core.money.BengaliNumber

/** Dots indicating PIN progress (PIN digits are never shown). */
@Composable
fun PinDots(entered: Int, maxLength: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "পিন: $entered টি সংখ্যা দেওয়া হয়েছে" },
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(maxLength) { index ->
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < entered) Hg.colors.brand else Hg.colors.surfaceSunken
                    )
            )
        }
    }
}

/** Numeric keypad with Bengali digit labels (values are ASCII under the hood). */
@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leftAction: (@Composable () -> Unit)? = null
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.Center) {
                row.forEach { digit ->
                    PinKey(
                        text = BengaliNumber.toBengaliDigits(digit.toString()),
                        description = "সংখ্যা ${BengaliNumber.toBengaliDigits(digit.toString())}",
                        enabled = enabled
                    ) { onDigit(digit) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                leftAction?.invoke()
            }
            PinKey(
                text = BengaliNumber.toBengaliDigits("0"),
                description = "সংখ্যা ০",
                enabled = enabled
            ) { onDigit('0') }
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onBackspace,
                    enabled = enabled,
                    modifier = Modifier
                        .size(56.dp)
                        .semantics { contentDescription = "শেষ সংখ্যা মুছুন" }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = null,
                        tint = Hg.colors.inkSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
    }
}

@Composable
private fun PinKey(text: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = Radius.card,
        color = Hg.colors.surface,
        border = BorderStroke(Stroke.hairline, Hg.colors.hairline),
        modifier = Modifier
            .size(84.dp)
            .padding(Space.xs)
            .semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                style = MaterialTheme.typography.headlineSmall,
                color = if (enabled) Hg.colors.ink else Hg.colors.inkTertiary
            )
        }
    }
}
