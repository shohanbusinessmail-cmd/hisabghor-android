package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.CategoryTotal
import com.shohankhan.hisabghor.data.MonthCashFlow
import kotlin.math.max

/**
 * Grouped bar chart of monthly income vs expense.
 *
 * Only real posting sums are plotted. Bars carry +/− semantics and the
 * caller supplies the empty state; this composable never invents values.
 */
@Composable
fun IncomeExpenseBarChart(
    data: List<MonthCashFlow>,
    modifier: Modifier = Modifier,
    heightDp: Int = 168
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = Hg.colors.inkTertiary
    val gridColor = Hg.colors.hairline
    val incomeColor = Hg.colors.income
    val expenseColor = Hg.colors.expense
    val maxValue = max(1L, data.maxOfOrNull { max(it.incomePoisha, it.expensePoisha) } ?: 1L)
    val axisLabel = BengaliNumber.formatPoisha(maxValue, withSymbol = false)
    val summary = data.joinToString("; ") {
        "${it.yearMonthLabel}: আয় ${BengaliNumber.formatPoisha(it.incomePoisha)}, " +
            "খরচ ${BengaliNumber.formatPoisha(it.expensePoisha)}"
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .semantics { contentDescription = "মাসিক আয় ও খরচের চার্ট। $summary" }
    ) {
        if (data.isEmpty()) return@Canvas
        val topPad = 18.dp.toPx()
        val bottomPad = 26.dp.toPx()
        val chartHeight = size.height - topPad - bottomPad
        val groupWidth = size.width / data.size
        val barWidth = (groupWidth * 0.26f).coerceAtMost(18.dp.toPx())
        val gap = 3.dp.toPx()

        // Horizontal reference line + scale label (max value only, keeps the
        // chart legible on small screens instead of a crowded axis).
        drawLine(
            color = gridColor,
            start = Offset(0f, topPad),
            end = Offset(size.width, topPad),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, topPad + chartHeight),
            end = Offset(size.width, topPad + chartHeight),
            strokeWidth = 1.dp.toPx()
        )
        val scaleLayout = textMeasurer.measure(
            text = axisLabel,
            style = TextStyle(color = labelColor, fontSize = 9.sp)
        )
        drawText(
            scaleLayout,
            topLeft = Offset(0f, (topPad - scaleLayout.size.height - 2.dp.toPx()).coerceAtLeast(0f))
        )

        data.forEachIndexed { index, month ->
            val centerX = groupWidth * index + groupWidth / 2
            val incomeH = chartHeight * (month.incomePoisha.toFloat() / maxValue)
            val expenseH = chartHeight * (month.expensePoisha.toFloat() / maxValue)

            if (incomeH > 0f) {
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(centerX - barWidth - gap / 2, topPad + chartHeight - incomeH),
                    size = Size(barWidth, incomeH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
            if (expenseH > 0f) {
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(centerX + gap / 2, topPad + chartHeight - expenseH),
                    size = Size(barWidth, expenseH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            // Month label, e.g. "অক্টো" from "2026-10 অক্টোবর".
            val label = monthShortLabel(month.yearMonthLabel)
            val textLayout = textMeasurer.measure(
                text = label,
                style = TextStyle(color = labelColor, fontSize = 10.sp)
            )
            drawText(
                textLayout,
                topLeft = Offset(
                    centerX - textLayout.size.width / 2,
                    topPad + chartHeight + 6.dp.toPx()
                )
            )
        }
    }
}

/** "2026-10 অক্টোবর" → "অক্টো" style short label for chart axes. */
private fun monthShortLabel(raw: String): String {
    val parts = raw.trim().split(" ")
    val name = parts.getOrNull(1) ?: parts.firstOrNull() ?: raw
    return if (name.length > 4) name.take(4) else name
}

@Composable
fun ChartLegend(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
        LegendItem(Hg.colors.income, "আয়")
        LegendItem(Hg.colors.expense, "খরচ")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary)
    }
}

/**
 * Ranked horizontal bars for category breakdowns. Reads better than a donut
 * on narrow screens because every label and amount is horizontal text.
 */
@Composable
fun CategoryBreakdownBars(
    parts: List<CategoryTotal>,
    modifier: Modifier = Modifier,
    maxItems: Int = 5,
    hidden: Boolean = false
) {
    val visible = parts.take(maxItems)
    val total = visible.sumOf { kotlin.math.abs(it.totalPoisha) }
    val maxPart = max(1L, visible.maxOfOrNull { kotlin.math.abs(it.totalPoisha) } ?: 1L)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        visible.forEach { part ->
            val amount = kotlin.math.abs(part.totalPoisha)
            val share = if (total <= 0) 0f else amount.toFloat() / total
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(8.dp)) { drawCircle(colorFromHex(part.colorHex)) }
                    Spacer(Modifier.width(Space.sm))
                    Text(
                        part.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.ink,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        if (hidden) "৳••••" else BengaliNumber.formatPoisha(amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.ink
                    )
                }
                Spacer(Modifier.height(Space.xs))
                HgProgressBar(
                    fraction = amount.toFloat() / maxPart.toFloat(),
                    color = colorFromHex(part.colorHex),
                    height = 6
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${BengaliNumber.toBengaliDigits((share * 100).toInt().toString())}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Hg.colors.inkTertiary
                )
            }
        }
    }
}

/**
 * Donut chart kept for the reports screen's compact overview. The legend is
 * rendered outside, so the donut itself only carries proportions.
 */
@Composable
fun CategoryDonutChart(
    parts: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    val total = parts.sumOf { kotlin.math.abs(it.totalPoisha) }
    Box(modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(132.dp)) {
            val stroke = 18.dp.toPx()
            if (total <= 0 || parts.isEmpty()) {
                drawArc(
                    color = HgColors.Light.surfaceSunken,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = stroke)
                )
                return@Canvas
            }
            var start = -90f
            parts.take(8).forEach { part ->
                val sweep = 360f * (kotlin.math.abs(part.totalPoisha).toFloat() / total)
                drawArc(
                    color = colorFromHex(part.colorHex),
                    startAngle = start,
                    sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("মোট", style = MaterialTheme.typography.labelSmall, color = Hg.colors.inkTertiary)
            Text(
                BengaliNumber.formatPoisha(total),
                style = MaterialTheme.typography.titleSmall,
                color = Hg.colors.ink
            )
        }
    }
}
