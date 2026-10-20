package com.shohankhan.hisabghor.core.money

/**
 * Bengali number formatting and parsing for BDT amounts.
 *
 * Display rules (consistent everywhere in the app):
 *  - Bengali digits (০১২৩৪৫৬৭৮৯).
 *  - Bangladeshi/Indian digit grouping (lakh style): ১,২৩,৪৫,৬৭৮.
 *  - Taka symbol `৳` prefix; poisha fraction shown only when non-zero.
 *  - Negative amounts get a leading `-`.
 *
 * Parsing accepts Bengali or English digits, grouping commas, the `৳`
 * symbol and surrounding spaces. At most two decimal digits are accepted;
 * anything else returns null so the UI can show a validation error.
 */
object BengaliNumber {

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBengaliDigits(input: String): String = buildString(input.length) {
        for (c in input) append(if (c in '0'..'9') bengaliDigits[c - '0'] else c)
    }

    fun toAsciiDigits(input: String): String = buildString(input.length) {
        for (c in input) {
            val idx = bengaliDigits.indexOf(c)
            append(if (idx >= 0) ('0' + idx) else c)
        }
    }

    /** Groups a non-negative ASCII digit string in lakh style: 12345678 → 1,23,45,678. */
    fun groupLakh(asciiDigits: String): String {
        require(asciiDigits.all { it.isDigit() }) { "digits only" }
        if (asciiDigits.length <= 3) return asciiDigits
        val last3 = asciiDigits.takeLast(3)
        var rest = asciiDigits.dropLast(3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        parts.add(last3)
        return parts.joinToString(",")
    }

    /**
     * Formats poisha for display, e.g. 12345650 → `৳১,২৩,৪৫৬.৫০`,
     * 100000 → `৳১,০০০`, -500 → `-৳৫`.
     */
    fun formatPoisha(poisha: Long, withSymbol: Boolean = true): String {
        val negative = poisha < 0
        val abs = if (poisha == Long.MIN_VALUE) poisha else kotlin.math.abs(poisha)
        val taka = abs / 100
        val rem = (abs % 100).toInt()
        val grouped = groupLakh(taka.toString())
        val fraction = if (rem == 0) "" else "." + rem.toString().padStart(2, '0')
        val body = toBengaliDigits(grouped + fraction)
        val sign = if (negative) "-" else ""
        return if (withSymbol) "$sign৳$body" else "$sign$body"
    }

    /** Formats a plain count with Bengali digits and grouping (no symbol). */
    fun formatCount(value: Long): String {
        val negative = value < 0
        val grouped = groupLakh(kotlin.math.abs(value).toString())
        return (if (negative) "-" else "") + toBengaliDigits(grouped)
    }

    /**
     * Parses a quantity like "১.৫" or "2.250" into thousandths (milli-units),
     * e.g. ১.৫ কেজি → 1500. Up to 3 decimal places; null when invalid.
     */
    fun parseQuantityMilli(raw: String): Long? {
        var s = toAsciiDigits(raw.trim().replace(",", "").replace(" ", ""))
        if (s.isEmpty()) return null
        if (s.startsWith(".")) s = "0$s"
        if (s.endsWith(".")) s += "0"
        if (!s.matches(Regex("""\d+(\.\d{1,3})?"""))) return null
        val parts = s.split(".")
        val whole = parts[0].toLongOrNull() ?: return null
        val frac = if (parts.size == 2) parts[1].padEnd(3, '0').toLong() else 0L
        return try {
            Math.addExact(Math.multiplyExact(whole, 1000L), frac)
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** Formats thousandths back to a Bengali quantity string: 1500 → "১.৫". */
    fun formatQuantityMilli(milli: Long): String {
        val whole = milli / 1000
        val frac = milli % 1000
        val fracText = frac.toString().padStart(3, '0').trimEnd('0')
        val ascii = if (fracText.isEmpty()) whole.toString() else "$whole.$fracText"
        return toBengaliDigits(ascii)
    }

    /**
     * Parses a user-entered amount into poisha. Returns null when the text is
     * not a valid amount (empty, letters, more than 2 decimals, overflow...).
     */
    fun parsePoisha(raw: String): Long? {
        var s = raw.trim()
        if (s.isEmpty()) return null
        s = s.replace("৳", "").replace(",", "").replace(" ", "").replace(" ", "")
        s = toAsciiDigits(s)
        if (s.isEmpty()) return null
        val negative = s.startsWith("-")
        if (negative) s = s.substring(1)
        if (s.startsWith("+")) s = s.substring(1)
        if (s.isEmpty()) return null
        if (s.startsWith(".")) s = "0$s"
        if (s.endsWith(".")) s = s + "0"
        if (!s.matches(Regex("""\d+(\.\d{1,2})?"""))) return null
        val parts = s.split(".")
        val takaPart = parts[0].toLongOrNull() ?: return null
        val fracPart = if (parts.size == 2) parts[1].padEnd(2, '0').toLong() else 0L
        val total = try {
            Math.addExact(Math.multiplyExact(takaPart, 100L), fracPart)
        } catch (e: ArithmeticException) {
            return null
        }
        return if (negative) -total else total
    }
}
