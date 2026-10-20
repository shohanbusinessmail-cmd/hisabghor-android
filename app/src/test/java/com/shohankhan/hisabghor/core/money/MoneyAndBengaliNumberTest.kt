package com.shohankhan.hisabghor.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyAndBengaliNumberTest {

    // ---------- Parsing ----------

    @Test
    fun parse_englishDigits() {
        assertEquals(123450L, BengaliNumber.parsePoisha("1234.50"))
        assertEquals(123450L, BengaliNumber.parsePoisha("1,234.50"))
        assertEquals(50000L, BengaliNumber.parsePoisha("500"))
        assertEquals(50L, BengaliNumber.parsePoisha("0.5"))
        assertEquals(5L, BengaliNumber.parsePoisha(".05"))
    }

    @Test
    fun parse_bengaliDigits() {
        assertEquals(123450L, BengaliNumber.parsePoisha("১,২৩৪.৫০"))
        assertEquals(50000L, BengaliNumber.parsePoisha("৳৫০০"))
        assertEquals(1000000L, BengaliNumber.parsePoisha(" ১০,০০০ "))
    }

    @Test
    fun parse_invalidReturnsNull() {
        assertNull(BengaliNumber.parsePoisha(""))
        assertNull(BengaliNumber.parsePoisha("abc"))
        assertNull(BengaliNumber.parsePoisha("1.234")) // >2 decimals
        assertNull(BengaliNumber.parsePoisha("12a"))
        assertNull(BengaliNumber.parsePoisha("99999999999999999999999")) // overflow
    }

    // ---------- Formatting ----------

    @Test
    fun format_basic() {
        assertEquals("৳০", BengaliNumber.formatPoisha(0))
        assertEquals("৳৫", BengaliNumber.formatPoisha(500))
        assertEquals("৳৫.৫০", BengaliNumber.formatPoisha(550))
        assertEquals("৳১,০০০", BengaliNumber.formatPoisha(100000))
        assertEquals("-৳২৫০", BengaliNumber.formatPoisha(-25000))
    }

    @Test
    fun format_lakhGrouping() {
        // ১,২৩,৪৫৬.৫০ টাকা
        assertEquals("৳১,২৩,৪৫৬.৫০", BengaliNumber.formatPoisha(12345650))
        // ১ কোটি টাকা = ১,০০,০০,০০০
        assertEquals("৳১,০০,০০,০০০", BengaliNumber.formatPoisha(1_000_000_000L))
    }

    @Test
    fun groupLakh_ascii() {
        assertEquals("100", BengaliNumber.groupLakh("100"))
        assertEquals("1,000", BengaliNumber.groupLakh("1000"))
        assertEquals("1,23,45,678", BengaliNumber.groupLakh("12345678"))
    }

    // ---------- Money arithmetic ----------

    @Test
    fun money_arithmeticIsExact() {
        assertEquals(Money.taka(3), Money.taka(1) + Money.taka(2))
        assertEquals(Money(150), Money.taka(2) - Money(50))
        assertEquals(Money(-100), -Money(100))
    }

    @Test
    fun money_overflowThrowsInsteadOfCorrupting() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE) + Money(1)
        }
        assertThrows(ArithmeticException::class.java) {
            Money.taka(Long.MAX_VALUE)
        }
    }

    @Test
    fun money_largeValuesStayExact() {
        // Repeatedly adding ১ পয়সা must never drift (the classic float bug).
        var total = Money.ZERO
        repeat(100_000) { total += Money(1) }
        assertEquals(Money.taka(1000), total)
    }
}
