package com.shohankhan.hisabghor.core.money

/**
 * Authoritative money value: integer **poisha** (1 taka = 100 poisha).
 *
 * Floating point is never used for stored or calculated money anywhere in
 * HisabGhor. All arithmetic is checked (`Math.*Exact`) so an overflow throws
 * instead of silently corrupting a balance.
 */
@JvmInline
value class Money(val poisha: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(Math.addExact(poisha, other.poisha))
    operator fun minus(other: Money): Money = Money(Math.subtractExact(poisha, other.poisha))
    operator fun unaryMinus(): Money = Money(Math.negateExact(poisha))

    fun isZero(): Boolean = poisha == 0L
    fun isPositive(): Boolean = poisha > 0L
    fun isNegative(): Boolean = poisha < 0L

    override fun compareTo(other: Money): Int = poisha.compareTo(other.poisha)

    /** Bengali display string, e.g. `৳১,২৩,৪৫৬.৫০`. */
    fun format(): String = BengaliNumber.formatPoisha(poisha)

    companion object {
        val ZERO = Money(0L)

        /** Whole taka → Money. */
        fun taka(taka: Long): Money = Money(Math.multiplyExact(taka, 100L))

        /** Parse user input (English or Bengali digits) into Money, null if invalid. */
        fun parse(input: String): Money? = BengaliNumber.parsePoisha(input)?.let { Money(it) }
    }
}

fun Iterable<Money>.sumMoney(): Money = fold(Money.ZERO) { acc, m -> acc + m }
