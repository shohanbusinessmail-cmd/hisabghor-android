package com.shohankhan.hisabghor.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun escapesCommasQuotesAndNewlines() {
        assertEquals("\"a,b\"", CsvExporter.escapeCell("a,b"))
        assertEquals("\"সে বলল \"\"হ্যালো\"\"\"", CsvExporter.escapeCell("সে বলল \"হ্যালো\""))
        assertEquals("\"line1\nline2\"", CsvExporter.escapeCell("line1\nline2"))
        assertEquals("সাধারণ", CsvExporter.escapeCell("সাধারণ"))
    }

    @Test
    fun neutralizesFormulaInjection() {
        assertEquals("'=SUM(A1:A2)", CsvExporter.escapeCell("=SUM(A1:A2)"))
        assertEquals("'+cmd", CsvExporter.escapeCell("+cmd"))
        assertEquals("'-2+3", CsvExporter.escapeCell("-2+3"))
        assertEquals("'@SUM(1)", CsvExporter.escapeCell("@SUM(1)"))
    }

    @Test
    fun build_includesBomAndCrlf() {
        val csv = CsvExporter.build(listOf("শিরোনাম", "পরিমাণ"), listOf(listOf("বেতন", "500")))
        assertTrue(csv.startsWith(CsvExporter.BOM))
        assertTrue(csv.contains("\r\n"))
        assertTrue(csv.contains("বেতন,500"))
    }

    @Test
    fun takaPlain_formatsDecimal() {
        assertEquals("500", CsvExporter.takaPlain(50_000))
        assertEquals("1234.50", CsvExporter.takaPlain(123_450))
        assertEquals("0.05", CsvExporter.takaPlain(5))
        assertEquals("-250.75", CsvExporter.takaPlain(-25_075))
    }
}
