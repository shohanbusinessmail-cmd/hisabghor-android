package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.CategoryTotal
import com.shohankhan.hisabghor.data.MonthCashFlow
import kotlin.math.max

/**
 * Grouped bar chart of monthly income (emerald) vs expense (soft red).
 * Values are real posting sums; an empty dataset renders an axis only and
 * callers show an empty state instead.
 */
@Composable
fun IncomeExpenseBarChart(
    data: List<MonthCashFlow>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val incomeColor = Emerald600
    val expenseColor = Color(0xFFE57373)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = max(1L, data.maxOfOrNull { max(it.incomePoisha, it.expensePoisha) } ?: 1L)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        if (data.isEmpty()) return@Canvas
        val chartHeight = size.height - 44.dp.toPx()
        val groupWidth = size.width / data.size
        val barWidth = (groupWidth * 0.30f).coerceAtMost(26.dp.toPx())
        val gap = 4.dp.toPx()

        data.forEachIndexed { index, month ->
            val centerX = groupWidth * index + groupWidth / 2
            val incomeH = chartHeight * (month.incomePoisha.toFloat() / maxValue)
            val expenseH = chartHeight * (month.expensePoisha.toFloat() / maxValue)

            drawRoundRect(
                color = incomeColor,
                topLeft = Offset(centerX - barWidth - gap / 2, chartHeight - incomeH),
                size = Size(barWidth, incomeH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = expenseColor,
                topLeft = Offset(centerX + gap / 2, chartHeight - expenseH),
                size = Size(barWidth, expenseH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            val label = BengaliNumber.toBengaliDigits(
                month.yearMonthLabel.substringBefore(" ").take(3)
            )
            val textLayout = textMeasurer.measure(
                text = label,
                style = androidx.compose.ui.text.TextStyle(color = labelColor, fontSize = 10.sp)
            )
            drawText(
                textLayout,
                topLeft = Offset(centerX - textLayout.size.width / 2, chartHeight + 6.dp.toPx())
            )
        }
        // Axis
        drawLine(
            color = labelColor.copy(alpha = 0.35f),
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun ChartLegend(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center) {
        LegendDot(Emerald600, "আয়")
        Spacer(Modifier.width(16.dp))
        LegendDot(Color(0xFFE57373), "খরচ")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Donut chart for category breakdown with the total in the middle. */
@Composable
fun CategoryDonutChart(
    parts: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    val total = parts.sumOf { kotlin.math.abs(it.totalPoisha) }
    Box(modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(170.dp)) {
            if (total <= 0 || parts.isEmpty()) {
                drawArc(
                    color = Color(0xFFE0EAE3),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 26.dp.toPx())
                )
                return@Canvas
            }
            var start = -90f
            parts.take(8).forEach { part ->
                val sweep = 360f * (kotlin.math.abs(part.totalPoisha).toFloat() / total)
                drawArc(
                    color = colorFromHex(part.colorHex),
                    startAngle = start,
                    sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                    useCenter = false,
                    style = Stroke(width = 26.dp.toPx())
                )
                start += sweep
            }
        }
        Text(
            BengaliNumber.formatPoisha(total),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
