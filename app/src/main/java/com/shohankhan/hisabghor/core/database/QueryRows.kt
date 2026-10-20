package com.shohankhan.hisabghor.core.database

import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.TransactionKind

/** Generic grouped sum of postings for one ledger (account/category/shop/loan). */
data class LedgerBalanceRow(
    val ledgerId: Long,
    val totalPoisha: Long
)

/** One line of an account statement (running balance computed by caller). */
data class AccountLedgerRow(
    val transactionId: Long,
    val occurredAt: Long,
    val title: String,
    val kind: TransactionKind,
    val signedAmountPoisha: Long
)

/** Upcoming/overdue installment with its loan's display name (dashboard, worker). */
data class UpcomingInstallmentRow(
    val installmentId: Long,
    val loanId: Long,
    val organizationName: String,
    val loanKind: LoanKind,
    val seq: Int,
    val dueDate: Long,
    val amountPoisha: Long,
    val paidTotalPoisha: Long
) {
    val remainingPoisha: Long get() = amountPoisha - paidTotalPoisha
}
