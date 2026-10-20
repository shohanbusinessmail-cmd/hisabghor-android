package com.shohankhan.hisabghor.core.money

import com.shohankhan.hisabghor.core.model.InstallmentFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class LoanMathTest {

    private val firstDue: LocalDate = LocalDate.of(2025, 2, 5)

    @Test
    fun emi_knownBankValue() {
        // ৳100,000 at 12% annual, 12 monthly installments → EMI ≈ ৳8,884.88
        val emi = LoanMath.emiPoisha(
            principalPoisha = 100_000_00,
            annualRateBp = 1200,
            installments = 12,
            frequency = InstallmentFrequency.MONTHLY
        )
        assertTrue("EMI $emi should be within 2 poisha of 888488", abs(emi - 888488) <= 2)
    }

    @Test
    fun emi_zeroInterest() {
        val emi = LoanMath.emiPoisha(120000, 0, 12, InstallmentFrequency.MONTHLY)
        assertEquals(10000L, emi)
    }

    @Test
    fun amortizedSchedule_principalSumsExactly() {
        val schedule = LoanMath.amortizedSchedule(
            principalPoisha = 250_000_00,
            annualRateBp = 1450,
            installments = 24,
            firstDueDate = firstDue,
            frequency = InstallmentFrequency.MONTHLY
        )
        assertEquals(24, schedule.size)
        assertEquals(250_000_00L, schedule.sumOf { it.principalPoisha })
        schedule.forEach { row ->
            assertEquals(row.principalPoisha + row.interestPoisha, row.amountPoisha)
            assertTrue(row.interestPoisha >= 0)
        }
        // Interest portion declines over time for an amortizing loan.
        assertTrue(schedule.first().interestPoisha > schedule.last().interestPoisha)
        // Monthly due dates.
        assertEquals(LocalDate.of(2025, 3, 5), schedule[1].dueDate)
    }

    @Test
    fun amortizedSchedule_zeroRateHasNoInterest() {
        val schedule = LoanMath.amortizedSchedule(
            100_00, 0, 4, firstDue, InstallmentFrequency.WEEKLY
        )
        assertEquals(0L, schedule.sumOf { it.interestPoisha })
        assertEquals(100_00L, schedule.sumOf { it.amountPoisha })
        assertEquals(firstDue.plusWeeks(1), schedule[1].dueDate)
    }

    @Test
    fun flatSchedule_ngoStyle() {
        // ৳50,000 principal, ৳55,000 total contractual, 10 weekly installments.
        val schedule = LoanMath.flatSchedule(
            principalPoisha = 50_000_00,
            totalContractualPoisha = 55_000_00,
            installments = 10,
            firstDueDate = firstDue,
            frequency = InstallmentFrequency.WEEKLY
        )
        assertEquals(50_000_00L, schedule.sumOf { it.principalPoisha })
        assertEquals(5_000_00L, schedule.sumOf { it.interestPoisha })
        assertEquals(55_000_00L, schedule.sumOf { it.amountPoisha })
        schedule.forEach { assertEquals(5_500_00L, it.amountPoisha) }
    }

    @Test
    fun flatSchedule_remainderGoesToLastInstallment() {
        // ৳1,000 over 3 installments cannot split evenly in poisha-perfect
        // taka parts; the last row must absorb the remainder and sums hold.
        val schedule = LoanMath.flatSchedule(
            principalPoisha = 100_00,
            totalContractualPoisha = 100_00,
            installments = 3,
            firstDueDate = firstDue,
            frequency = InstallmentFrequency.MONTHLY
        )
        assertEquals(100_00L, schedule.sumOf { it.amountPoisha })
        assertEquals(3333L, schedule[0].amountPoisha)
        assertEquals(3334L, schedule[2].amountPoisha)
    }

    @Test
    fun splitEvenly_sumsExactly() {
        assertEquals(listOf(33L, 33L, 34L), LoanMath.splitEvenly(100, 3))
        assertEquals(listOf(25L, 25L, 25L, 25L), LoanMath.splitEvenly(100, 4))
    }

    @Test
    fun simpleInterest_oneYear() {
        // ৳10,000 at 10% for 365 days = ৳1,000
        assertEquals(100_000L, LoanMath.simpleInterestPoisha(10_000_00, 1000, 365))
        assertEquals(0L, LoanMath.simpleInterestPoisha(10_000_00, 0, 365))
    }

    @Test
    fun nextDueDate_monthEndClamps() {
        // Jan 31 + 1 month = Feb 28 in 2025 (java.time clamping, documented).
        val d = LoanMath.nextDueDate(LocalDate.of(2025, 1, 31), InstallmentFrequency.MONTHLY, 1)
        assertEquals(LocalDate.of(2025, 2, 28), d)
    }
}
