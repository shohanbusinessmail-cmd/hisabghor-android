package com.shohankhan.hisabghor.core.accounting

import com.shohankhan.hisabghor.core.model.LedgerType

/**
 * A single journal line before persistence.
 * Positive = debit (assets/expenses up), negative = credit (liabilities/income up).
 */
data class PostingDraft(
    val ledgerType: LedgerType,
    val ledgerId: Long,
    val signedAmountPoisha: Long
)

/**
 * Pure factory that turns a financial event into balanced double-entry
 * postings. This is the single implementation of docs/ACCOUNTING_RULES.md;
 * repositories must use it instead of hand-building postings.
 *
 * Invariants enforced here:
 *  - The postings of every event sum to exactly zero.
 *  - Amounts that must be positive are validated and IllegalArgumentException
 *    is thrown otherwise (callers validate user input before this point too).
 *  - Zero-amount lines are omitted.
 */
object PostingFactory {

    private fun build(vararg drafts: PostingDraft): List<PostingDraft> {
        val list = drafts.filter { it.signedAmountPoisha != 0L }
        val sum = list.fold(0L) { acc, d -> Math.addExact(acc, d.signedAmountPoisha) }
        check(sum == 0L) { "postings must balance to zero, got $sum" }
        check(list.size >= 2) { "a balanced entry needs at least two postings" }
        return list
    }

    private fun pos(value: Long, name: String): Long {
        require(value > 0) { "$name must be positive" }
        return value
    }

    private fun nonNeg(value: Long, name: String): Long {
        require(value >= 0) { "$name must be >= 0" }
        return value
    }

    /** Opening balance of an owned account (amount may be negative for an overdrawn start). */
    fun openingAccountBalance(accountId: Long, amountPoisha: Long): List<PostingDraft> {
        require(amountPoisha != 0L) { "opening balance must be non-zero" }
        return build(
            PostingDraft(LedgerType.ACCOUNT, accountId, amountPoisha),
            PostingDraft(LedgerType.EQUITY, 0, -amountPoisha)
        )
    }

    /** Pre-existing shop due carried into the app. */
    fun openingShopDue(shopId: Long, amountPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.EQUITY, 0, pos(amountPoisha, "amount")),
        PostingDraft(LedgerType.SHOP_LIABILITY, shopId, -amountPoisha)
    )

    /** Pre-existing loan principal carried into the app. */
    fun openingLoanLiability(loanId: Long, amountPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.EQUITY, 0, pos(amountPoisha, "amount")),
        PostingDraft(LedgerType.LOAN_LIABILITY, loanId, -amountPoisha)
    )

    /** Pre-existing receivable (money lent before using the app). */
    fun openingReceivable(loanId: Long, amountPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.RECEIVABLE, loanId, pos(amountPoisha, "amount")),
        PostingDraft(LedgerType.EQUITY, 0, -amountPoisha)
    )

    fun income(accountId: Long, categoryId: Long, amountPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.ACCOUNT, accountId, pos(amountPoisha, "amount")),
        PostingDraft(LedgerType.INCOME_CATEGORY, categoryId, -amountPoisha)
    )

    fun expense(accountId: Long, categoryId: Long, amountPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.EXPENSE_CATEGORY, categoryId, pos(amountPoisha, "amount")),
        PostingDraft(LedgerType.ACCOUNT, accountId, -amountPoisha)
    )

    /** Money returned for a previous expense: reduces expense, not new income. */
    fun expenseRefund(accountId: Long, categoryId: Long, amountPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.ACCOUNT, accountId, pos(amountPoisha, "amount")),
        PostingDraft(LedgerType.EXPENSE_CATEGORY, categoryId, -amountPoisha)
    )

    /**
     * Transfer between owned accounts. Only the optional fee touches an
     * expense category; the moved amount is neither income nor expense.
     */
    fun transfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountPoisha: Long,
        feePoisha: Long = 0,
        feeCategoryId: Long? = null
    ): List<PostingDraft> {
        require(fromAccountId != toAccountId) { "source and destination must differ" }
        pos(amountPoisha, "amount")
        nonNeg(feePoisha, "fee")
        if (feePoisha > 0) require(feeCategoryId != null) { "fee needs an expense category" }
        return build(
            PostingDraft(LedgerType.ACCOUNT, toAccountId, amountPoisha),
            PostingDraft(LedgerType.EXPENSE_CATEGORY, feeCategoryId ?: 0, feePoisha),
            PostingDraft(LedgerType.ACCOUNT, fromAccountId, -(amountPoisha + feePoisha))
        )
    }

    /**
     * Credit purchase: expense is counted once at purchase time; the unpaid
     * part becomes a shop liability; only the amount actually paid now
     * leaves the paying account.
     */
    fun shopCreditPurchase(
        shopId: Long,
        categoryId: Long,
        totalPoisha: Long,
        paidNowPoisha: Long,
        accountId: Long?
    ): List<PostingDraft> {
        pos(totalPoisha, "total")
        nonNeg(paidNowPoisha, "paidNow")
        require(paidNowPoisha <= totalPoisha) { "paidNow cannot exceed total" }
        if (paidNowPoisha > 0) require(accountId != null) { "paidNow needs a paying account" }
        val due = totalPoisha - paidNowPoisha
        return build(
            PostingDraft(LedgerType.EXPENSE_CATEGORY, categoryId, totalPoisha),
            PostingDraft(LedgerType.ACCOUNT, accountId ?: 0, -paidNowPoisha),
            PostingDraft(LedgerType.SHOP_LIABILITY, shopId, -due)
        )
    }

    /** Paying off (part of) a shop due. Not a new expense. */
    fun shopPayment(shopId: Long, accountId: Long, amountPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.SHOP_LIABILITY, shopId, pos(amountPoisha, "amount")),
        PostingDraft(LedgerType.ACCOUNT, accountId, -amountPoisha)
    )

    /** Borrowed/disbursed money arrives: asset up, liability up, not income. */
    fun loanReceive(loanId: Long, accountId: Long, disbursedPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.ACCOUNT, accountId, pos(disbursedPoisha, "disbursed")),
        PostingDraft(LedgerType.LOAN_LIABILITY, loanId, -disbursedPoisha)
    )

    /**
     * Disbursement where the lender kept an upfront charge: liability is the
     * full principal, cash received is less, and the difference is an expense
     * (service charge) recorded openly — never hidden.
     */
    fun loanReceiveWithUpfrontCharge(
        loanId: Long,
        accountId: Long,
        principalPoisha: Long,
        disbursedPoisha: Long,
        chargeCategoryId: Long?
    ): List<PostingDraft> {
        pos(principalPoisha, "principal")
        pos(disbursedPoisha, "disbursed")
        require(disbursedPoisha <= principalPoisha) { "disbursed cannot exceed principal" }
        val charge = principalPoisha - disbursedPoisha
        if (charge > 0) require(chargeCategoryId != null) { "upfront charge needs a category" }
        return build(
            PostingDraft(LedgerType.ACCOUNT, accountId, disbursedPoisha),
            PostingDraft(LedgerType.EXPENSE_CATEGORY, chargeCategoryId ?: 0, charge),
            PostingDraft(LedgerType.LOAN_LIABILITY, loanId, -principalPoisha)
        )
    }

    /**
     * Loan repayment. Principal reduces liability only; interest and fees are
     * expenses recorded under their own categories.
     */
    fun loanRepayment(
        loanId: Long,
        accountId: Long,
        principalPoisha: Long,
        interestPoisha: Long,
        interestCategoryId: Long?,
        feePoisha: Long,
        feeCategoryId: Long?
    ): List<PostingDraft> {
        nonNeg(principalPoisha, "principal")
        nonNeg(interestPoisha, "interest")
        nonNeg(feePoisha, "fee")
        require(principalPoisha + interestPoisha + feePoisha > 0) { "repayment must be positive" }
        if (interestPoisha > 0) require(interestCategoryId != null) { "interest needs a category" }
        if (feePoisha > 0) require(feeCategoryId != null) { "fee needs a category" }
        val total = principalPoisha + interestPoisha + feePoisha
        // Merge interest & fee if they share one category to keep lines clean.
        val drafts = ArrayList<PostingDraft>()
        drafts += PostingDraft(LedgerType.LOAN_LIABILITY, loanId, principalPoisha)
        if (interestCategoryId != null && interestCategoryId == feeCategoryId) {
            drafts += PostingDraft(LedgerType.EXPENSE_CATEGORY, interestCategoryId, interestPoisha + feePoisha)
        } else {
            drafts += PostingDraft(LedgerType.EXPENSE_CATEGORY, interestCategoryId ?: 0, interestPoisha)
            drafts += PostingDraft(LedgerType.EXPENSE_CATEGORY, feeCategoryId ?: 0, feePoisha)
        }
        drafts += PostingDraft(LedgerType.ACCOUNT, accountId, -total)
        return build(*drafts.toTypedArray())
    }

    /** Lending money out: creates a receivable, not an expense. */
    fun lendOut(loanId: Long, accountId: Long, amountPoisha: Long): List<PostingDraft> = build(
        PostingDraft(LedgerType.RECEIVABLE, loanId, pos(amountPoisha, "amount")),
        PostingDraft(LedgerType.ACCOUNT, accountId, -amountPoisha)
    )

    /** Money lent earlier comes back: principal reduces receivable; only interest is income. */
    fun lendReturn(
        loanId: Long,
        accountId: Long,
        principalPoisha: Long,
        interestPoisha: Long,
        interestCategoryId: Long?
    ): List<PostingDraft> {
        nonNeg(principalPoisha, "principal")
        nonNeg(interestPoisha, "interest")
        require(principalPoisha + interestPoisha > 0) { "return must be positive" }
        if (interestPoisha > 0) require(interestCategoryId != null) { "interest needs a category" }
        return build(
            PostingDraft(LedgerType.ACCOUNT, accountId, principalPoisha + interestPoisha),
            PostingDraft(LedgerType.RECEIVABLE, loanId, -principalPoisha),
            PostingDraft(LedgerType.INCOME_CATEGORY, interestCategoryId ?: 0, -interestPoisha)
        )
    }

    /** Manual balance correction against opening equity (labelled, auditable). */
    fun adjustment(accountId: Long, deltaPoisha: Long): List<PostingDraft> {
        require(deltaPoisha != 0L) { "adjustment must be non-zero" }
        return build(
            PostingDraft(LedgerType.ACCOUNT, accountId, deltaPoisha),
            PostingDraft(LedgerType.EQUITY, 0, -deltaPoisha)
        )
    }
}
