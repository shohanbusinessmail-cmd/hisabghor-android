package com.shohankhan.hisabghor.core.backup

/**
 * RFC-4180 CSV writing with spreadsheet safety:
 *  - UTF-8 BOM so Excel renders Bengali correctly.
 *  - Cells starting with =, +, -, @, tab or CR are prefixed with a single
 *    quote to prevent formula injection when opened in a spreadsheet.
 *  - Quotes, commas and newlines are escaped by quoting.
 */
object CsvExporter {

    const val BOM = "﻿"

    fun escapeCell(raw: String): String {
        var value = raw
        if (value.isNotEmpty() && value[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            value = "'$value"
        }
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun build(headers: List<String>, rows: List<List<String>>): String = buildString {
        append(BOM)
        append(headers.joinToString(",") { escapeCell(it) })
        append("\r\n")
        for (row in rows) {
            append(row.joinToString(",") { escapeCell(it) })
            append("\r\n")
        }
    }

    /** Poisha → plain decimal taka string for spreadsheets ("1234.50", "500"). */
    fun takaPlain(poisha: Long): String {
        val sign = if (poisha < 0) "-" else ""
        val abs = kotlin.math.abs(poisha)
        val taka = abs / 100
        val rem = abs % 100
        return if (rem == 0L) "$sign$taka" else "$sign$taka.${rem.toString().padStart(2, '0')}"
    }
}
