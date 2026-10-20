package com.shohankhan.hisabghor.core.backup

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.shohankhan.hisabghor.core.money.BengaliNumber
import java.io.ByteArrayOutputStream

/** One line in a printed statement: [left] text, optional right-aligned [right]. */
data class PdfRow(
    val left: String,
    val right: String = "",
    val bold: Boolean = false,
    val small: Boolean = false
)

data class PdfSection(
    val heading: String,
    val rows: List<PdfRow>
)

/**
 * Minimal, dependency-free PDF statement renderer (Android [PdfDocument]).
 * A4 pages, Bengali text via the platform text stack, automatic pagination.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595   // A4 @72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    private val emerald = 0xFF0B7A4B.toInt()
    private val darkText = 0xFF1B1B1B.toInt()
    private val grayText = 0xFF666666.toInt()
    private val lineGray = 0xFFCCCCCC.toInt()

    fun render(title: String, subtitle: String, sections: List<PdfSection>): ByteArray {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = emerald
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grayText
            textSize = 11f
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 10.5f
        }
        val boldPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grayText
            textSize = 9f
        }
        val linePaint = Paint().apply {
            color = lineGray
            strokeWidth = 0.7f
        }

        var pageNumber = 0
        fun newPage(): Pair<PdfDocument.Page, Canvas> {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(info)
            return page to page.canvas
        }

        var (page, canvas) = newPage()
        var y = MARGIN + 8f
        canvas.drawText(title, MARGIN, y, titlePaint)
        y += 20f
        canvas.drawText(subtitle, MARGIN, y, subtitlePaint)
        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 18f

        fun footer(target: Canvas, number: Int) {
            target.drawText(
                "হিসাবঘর — পৃষ্ঠা ${BengaliNumber.formatCount(number.toLong())}",
                MARGIN, PAGE_HEIGHT - MARGIN + 12f, smallPaint
            )
        }

        for (section in sections) {
            // Keep heading with at least one row.
            if (y > PAGE_HEIGHT - MARGIN - 60f) {
                footer(canvas, pageNumber)
                document.finishPage(page)
                val next = newPage()
                page = next.first
                canvas = next.second
                y = MARGIN + 6f
            }
            canvas.drawText(section.heading, MARGIN, y, headingPaint)
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f
            for (row in section.rows) {
                if (y > PAGE_HEIGHT - MARGIN - 24f) {
                    footer(canvas, pageNumber)
                    document.finishPage(page)
                    val next = newPage()
                    page = next.first
                    canvas = next.second
                    y = MARGIN + 6f
                }
                val paint = when {
                    row.bold -> boldPaint
                    row.small -> smallPaint
                    else -> bodyPaint
                }
                val maxLeftWidth = PAGE_WIDTH - 2 * MARGIN - 150f
                val left = ellipsize(row.left, paint, maxLeftWidth)
                canvas.drawText(left, MARGIN, y, paint)
                if (row.right.isNotEmpty()) {
                    val rightWidth = paint.measureText(row.right)
                    canvas.drawText(row.right, PAGE_WIDTH - MARGIN - rightWidth, y, paint)
                }
                y += if (row.small) 12f else 14f
            }
            y += 10f
        }

        footer(canvas, pageNumber)
        document.finishPage(page)

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }
}
