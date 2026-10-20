package com.shohankhan.hisabghor.core.accounting

import com.shohankhan.hisabghor.core.model.LedgerType

/**
 * Pure balance derivations over a set of postings. The Room DAOs implement
 * the identical formulas in SQL; this class exists so the accounting rules
 * can be unit-tested without a database, and so previews can reuse it.
 * See docs/ACCOUNTING_RULES.md §1.
 */
object LedgerCalculator {

    fun accountBalance(postings: Iterable<PostingDraft>, accountId: Long): Long =
        sum(postings, LedgerType.ACCOUNT, accountId)

    fun incomeTotal(postings: Iterable<PostingDraft>): Long =
        -sum(postings, LedgerType.INCOME_CATEGORY, null)

    fun expenseTotal(postings: Iterable<PostingDraft>): Long =
        sum(postings, LedgerType.EXPENSE_CATEGORY, null)

    fun incomeForCategory(postings: Iterable<PostingDraft>, categoryId: Long): Long =
        -sum(postings, LedgerType.INCOME_CATEGORY, categoryId)

    fun expenseForCategory(postings: Iterable<PostingDraft>, categoryId: Long): Long =
        sum(postings, LedgerType.EXPENSE_CATEGORY, categoryId)

    fun shopDue(postings: Iterable<PostingDraft>, shopId: Long): Long =
        -sum(postings, LedgerType.SHOP_LIABILITY, shopId)

    fun loanOutstanding(postings: Iterable<PostingDraft>, loanId: Long): Long =
        -sum(postings, LedgerType.LOAN_LIABILITY, loanId)

    fun receivableOutstanding(postings: Iterable<PostingDraft>, loanId: Long): Long =
        sum(postings, LedgerType.RECEIVABLE, loanId)

    /** Sum of every posting — must always be 0 for a healthy ledger. */
    fun trialBalance(postings: Iterable<PostingDraft>): Long =
        postings.fold(0L) { acc, p -> Math.addExact(acc, p.signedAmountPoisha) }

    private fun sum(postings: Iterable<PostingDraft>, type: LedgerType, ledgerId: Long?): Long =
        postings.fold(0L) { acc, p ->
            if (p.ledgerType == type && (ledgerId == null || p.ledgerId == ledgerId)) {
                Math.addExact(acc, p.signedAmountPoisha)
            } else acc
        }
}
