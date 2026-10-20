package com.shohankhan.hisabghor.core.accounting

import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentAllocatorTest {

    private fun due(id: Long, seq: Int, principal: Long, interest: Long, fee: Long = 0) =
        InstallmentDue(id, seq, principal, interest, fee)

    @Test
    fun allocatesOldestFirst_interestBeforePrincipal() {
        val dues = listOf(
            due(1, 1, principal = 100_00, interest = 10_00),
            due(2, 2, principal = 100_00, interest = 10_00),
            due(3, 3, principal = 100_00, interest = 10_00)
        )
        // Pay ৳115: clears #1 (10 interest + 100 principal), then ৳5 interest of #2.
        val result = InstallmentAllocator.allocate(dues, 115_00)
        assertEquals(2, result.allocations.size)
        assertEquals(100_00L, result.allocations[0].principalPaidPoisha)
        assertEquals(10_00L, result.allocations[0].interestPaidPoisha)
        assertEquals(5_00L, result.allocations[1].interestPaidPoisha)
        assertEquals(0L, result.allocations[1].principalPaidPoisha)
        assertEquals(0L, result.leftoverPoisha)
        assertEquals(100_00L, result.totalPrincipalPoisha)
        assertEquals(15_00L, result.totalInterestPoisha)
    }

    @Test
    fun partialPaymentSmallerThanInterest() {
        val dues = listOf(due(1, 1, principal = 100_00, interest = 10_00))
        val result = InstallmentAllocator.allocate(dues, 4_00)
        assertEquals(4_00L, result.allocations.single().interestPaidPoisha)
        assertEquals(0L, result.allocations.single().principalPaidPoisha)
    }

    @Test
    fun overpaymentReturnsLeftover() {
        val dues = listOf(due(1, 1, principal = 100_00, interest = 10_00))
        val result = InstallmentAllocator.allocate(dues, 200_00)
        assertEquals(110_00L, result.allocations.sumOf { it.totalPaidPoisha })
        assertEquals(90_00L, result.leftoverPoisha)
    }

    @Test
    fun alreadySettledInstallmentsAreSkipped() {
        val dues = listOf(
            due(1, 1, principal = 0, interest = 0),
            due(2, 2, principal = 50_00, interest = 5_00)
        )
        val result = InstallmentAllocator.allocate(dues, 55_00)
        assertEquals(1, result.allocations.size)
        assertEquals(2L, result.allocations.single().installmentId)
    }
}
