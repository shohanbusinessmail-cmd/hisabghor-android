package com.shohankhan.hisabghor.core.common

import com.shohankhan.hisabghor.core.money.BengaliNumber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date/time helpers. Timestamps are stored as epoch milliseconds (UTC) and
 * always interpreted/displayed in the Asia/Dhaka time zone. Bangladesh does
 * not observe daylight saving, which keeps day arithmetic stable.
 */
object Dates {

    val dhaKaZone: ZoneId = ZoneId.of("Asia/Dhaka")
    private val bnLocale: Locale = Locale.forLanguageTag("bn-BD")

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", bnLocale)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", bnLocale)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", bnLocale)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", bnLocale)

    fun nowMs(): Long = System.currentTimeMillis()

    fun toLocalDate(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(dhaKaZone).toLocalDate()

    fun toLocalDateTime(epochMs: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMs).atZone(dhaKaZone).toLocalDateTime()

    /** Epoch ms at local (Dhaka) midnight of the given date. */
    fun startOfDayMs(date: LocalDate): Long =
        date.atStartOfDay(dhaKaZone).toInstant().toEpochMilli()

    fun startOfDayMs(epochMs: Long): Long = startOfDayMs(toLocalDate(epochMs))

    /** [start, endExclusive) epoch-ms range covering the local day of [epochMs]. */
    fun dayRange(epochMs: Long): Pair<Long, Long> {
        val date = toLocalDate(epochMs)
        return startOfDayMs(date) to startOfDayMs(date.plusDays(1))
    }

    /** [start, endExclusive) range covering the calendar month containing [epochMs]. */
    fun monthRange(epochMs: Long): Pair<Long, Long> {
        val date = toLocalDate(epochMs).withDayOfMonth(1)
        return startOfDayMs(date) to startOfDayMs(date.plusMonths(1))
    }

    /** [start, endExclusive) range covering the calendar year containing [epochMs]. */
    fun yearRange(epochMs: Long): Pair<Long, Long> {
        val date = toLocalDate(epochMs).withDayOfYear(1)
        return startOfDayMs(date) to startOfDayMs(date.plusYears(1))
    }

    fun today(): LocalDate = toLocalDate(nowMs())

    fun formatDate(epochMs: Long): String =
        BengaliNumber.toBengaliDigits(toLocalDate(epochMs).format(dateFormatter))

    fun formatShortDate(epochMs: Long): String =
        BengaliNumber.toBengaliDigits(toLocalDate(epochMs).format(shortDateFormatter))

    fun formatDate(date: LocalDate): String =
        BengaliNumber.toBengaliDigits(date.format(dateFormatter))

    fun formatMonthYear(epochMs: Long): String =
        BengaliNumber.toBengaliDigits(toLocalDate(epochMs).format(monthYearFormatter))

    fun formatDateTime(epochMs: Long): String {
        val dt = toLocalDateTime(epochMs)
        val date = dt.toLocalDate().format(shortDateFormatter)
        val time = dt.format(timeFormatter)
        return BengaliNumber.toBengaliDigits("$date, $time")
    }
}
