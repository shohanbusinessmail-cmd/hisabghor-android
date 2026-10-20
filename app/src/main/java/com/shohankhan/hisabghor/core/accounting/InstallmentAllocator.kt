package com.shohankhan.hisabghor.core.accounting

/**
 * Snapshot of one installment's remaining dues, used by [InstallmentAllocator].
 * Mirrors the Room entity fields so repositories can map 1:1.
 */
data class InstallmentDue(
    val installmentId: Long,
    val seq: Int,
    val principalDuePoisha: Long,
    val interestDuePoisha: Long,
    val feeDuePoisha: Long
) {
    val totalDuePoisha: Long get() = principalDuePoisha + interestDuePoisha + feeDuePoisha
}

/** How much of one payment went to one installment. */
data class InstallmentAllocation(
    val installmentId: Long,
    val principalPaidPoisha: Long,
    val interestPaidPoisha: Long,
    val feePaidPoisha: Long
) {
    val totalPaidPoisha: Long get() = principalPaidPoisha + interestPaidPoisha + feePaidPoisha
}

data class AllocationResult(
    val allocations: List<InstallmentAllocation>,
    /** Payment left after the whole schedule is settled (extra principal). */
    val leftoverPoisha: Long
) {
    val totalPrincipalPoisha: Long get() = allocations.sumOf { it.principalPaidPoisha }
    val totalInterestPoisha: Long get() = allocations.sumOf { it.interestPaidPoisha }
    val totalFeePoisha: Long get() = allocations.sumOf { it.feePaidPoisha }
}

/**
 * Allocates a repayment across a loan's installment schedule.
 *
 * Order: oldest installment first; within an installment, interest first,
 * then fees, then principal (the common collection order). Any amount beyond
 * the full schedule is returned as leftover so the caller can treat it as
 * extra principal and shorten the schedule from the end.
 */
object InstallmentAllocator {

    fun allocate(dues: List<InstallmentDue>, paymentPoisha: Long): AllocationResult {
        require(paymentPoisha > 0) { "payment must be positive" }
        var remaining = paymentPoisha
        val allocations = ArrayList<InstallmentAllocation>()
        for (due in dues.sortedBy { it.seq }) {
            if (remaining == 0L) break
            if (due.totalDuePoisha == 0L) continue
            val interestPaid = minOf(remaining, due.interestDuePoisha); remaining -= interestPaid
            val feePaid = minOf(remaining, due.feeDuePoisha); remaining -= feePaid
            val principalPaid = minOf(remaining, due.principalDuePoisha); remaining -= principalPaid
            if (interestPaid + feePaid + principalPaid > 0) {
                allocations += InstallmentAllocation(due.installmentId, principalPaid, interestPaid, feePaid)
            }
        }
        return AllocationResult(allocations, remaining)
    }
}
