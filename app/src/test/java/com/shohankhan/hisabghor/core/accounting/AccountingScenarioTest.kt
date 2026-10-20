package com.shohankhan.hisabghor.core.accounting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The required accounting scenarios from the product spec §22, verified at
 * the posting level (the same formulas run in SQL in the app). Scenario 9
 * (failed restore keeps old data) is covered by database tests.
 */
class AccountingScenarioTest {

    // Ledger ids used across scenarios.
    private val cash = 1L
    private val bkash = 2L
    private val savings = 3L
    private val shop = 11L
    private val catFood = 21L
    private val catSalary = 22L
    private val catInterest = 23L
    private val loan = 31L
    private val lending = 32L

    private fun taka(v: Long) = v * 100

    @Test
    fun scenario1_creditPurchase_partialPaymentLeavesDue() {
        // Buy ৳1,000 on credit, pay ৳300 now → due ৳700, expense ৳1,000 once.
        val postings = PostingFactory.openingAccountBalance(cash, taka(5_000)) +
            PostingFactory.shopCreditPurchase(shop, catFood, taka(1_000), taka(300), cash)

        assertEquals(taka(700), LedgerCalculator.shopDue(postings, shop))
        assertEquals(taka(1_000), LedgerCalculator.expenseTotal(postings))
        assertEquals(taka(4_700), LedgerCalculator.accountBalance(postings, cash))
        assertEquals(0L, LedgerCalculator.incomeTotal(postings))
    }

    @Test
    fun scenario2_secondShopPaymentReducesDueWithoutNewExpense() {
        val postings = PostingFactory.openingAccountBalance(cash, taka(5_000)) +
            PostingFactory.shopCreditPurchase(shop, catFood, taka(1_000), taka(300), cash) +
            PostingFactory.shopPayment(shop, cash, taka(200))

        assertEquals(taka(500), LedgerCalculator.shopDue(postings, shop))
        // Still only the original ৳1,000 expense — never double counted.
        assertEquals(taka(1_000), LedgerCalculator.expenseTotal(postings))
        assertEquals(taka(4_500), LedgerCalculator.accountBalance(postings, cash))
    }

    @Test
    fun scenario3_transferBetweenOwnedAccountsIsNotIncomeOrExpense() {
        val postings = PostingFactory.openingAccountBalance(cash, taka(2_000)) +
            PostingFactory.transfer(cash, bkash, taka(500))

        assertEquals(0L, LedgerCalculator.incomeTotal(postings))
        assertEquals(0L, LedgerCalculator.expenseTotal(postings))
        assertEquals(taka(1_500), LedgerCalculator.accountBalance(postings, cash))
        assertEquals(taka(500), LedgerCalculator.accountBalance(postings, bkash))
    }

    @Test
    fun scenario3b_transferFeeIsExpenseButMovedAmountIsNot() {
        val postings = PostingFactory.openingAccountBalance(cash, taka(2_000)) +
            PostingFactory.transfer(cash, bkash, taka(500), feePoisha = taka(10), feeCategoryId = catInterest)

        assertEquals(taka(10), LedgerCalculator.expenseTotal(postings))
        assertEquals(taka(1_490), LedgerCalculator.accountBalance(postings, cash))
        assertEquals(taka(500), LedgerCalculator.accountBalance(postings, bkash))
    }

    @Test
    fun scenario4_savingsDepositIsNotExpenseAndNotDoubleCounted() {
        val postings = PostingFactory.openingAccountBalance(cash, taka(3_000)) +
            PostingFactory.transfer(cash, savings, taka(1_000))

        assertEquals(0L, LedgerCalculator.expenseTotal(postings))
        val totalAssets = LedgerCalculator.accountBalance(postings, cash) +
            LedgerCalculator.accountBalance(postings, savings)
        assertEquals(taka(3_000), totalAssets) // unchanged, just moved
        assertEquals(taka(1_000), LedgerCalculator.accountBalance(postings, savings))
    }

    @Test
    fun scenario5_borrowingCreatesLiabilityNotIncome() {
        val postings = PostingFactory.loanReceive(loan, cash, taka(10_000))

        assertEquals(taka(10_000), LedgerCalculator.loanOutstanding(postings, loan))
        assertEquals(taka(10_000), LedgerCalculator.accountBalance(postings, cash))
        assertEquals(0L, LedgerCalculator.incomeTotal(postings))
    }

    @Test
    fun scenario6_lendingCreatesReceivableNotExpense() {
        val postings = PostingFactory.openingAccountBalance(cash, taka(5_000)) +
            PostingFactory.lendOut(lending, cash, taka(2_000))

        assertEquals(taka(2_000), LedgerCalculator.receivableOutstanding(postings, lending))
        assertEquals(taka(3_000), LedgerCalculator.accountBalance(postings, cash))
        assertEquals(0L, LedgerCalculator.expenseTotal(postings))
    }

    @Test
    fun scenario7_repaymentSplitsPrincipalAndInterest() {
        val postings = PostingFactory.openingAccountBalance(cash, taka(5_000)) +
            PostingFactory.loanReceive(loan, cash, taka(10_000)) +
            PostingFactory.loanRepayment(
                loanId = loan, accountId = cash,
                principalPoisha = taka(1_000), interestPoisha = taka(100),
                interestCategoryId = catInterest, feePoisha = 0, feeCategoryId = null
            )

        assertEquals(taka(9_000), LedgerCalculator.loanOutstanding(postings, loan))
        assertEquals(taka(100), LedgerCalculator.expenseTotal(postings)) // interest only
        assertEquals(taka(13_900), LedgerCalculator.accountBalance(postings, cash))
    }

    @Test
    fun scenario7b_lendReturnPrincipalIsNotIncomeInterestIs() {
        val postings = PostingFactory.openingAccountBalance(cash, taka(5_000)) +
            PostingFactory.lendOut(lending, cash, taka(2_000)) +
            PostingFactory.lendReturn(
                loanId = lending, accountId = cash,
                principalPoisha = taka(1_000), interestPoisha = taka(50),
                interestCategoryId = catSalary
            )

        assertEquals(taka(1_000), LedgerCalculator.receivableOutstanding(postings, lending))
        assertEquals(taka(50), LedgerCalculator.incomeTotal(postings))
        assertEquals(taka(4_050), LedgerCalculator.accountBalance(postings, cash))
    }

    @Test
    fun scenario8_editByVoidAndReplaceChangesBalancesExactlyOnce() {
        val original = PostingFactory.openingAccountBalance(cash, taka(1_000)) +
            PostingFactory.income(cash, catSalary, taka(500))
        assertEquals(taka(1_500), LedgerCalculator.accountBalance(original, cash))

        // Edit = void the ৳500 income and record the corrected ৳650 income.
        val afterEdit = PostingFactory.openingAccountBalance(cash, taka(1_000)) +
            PostingFactory.income(cash, catSalary, taka(650))
        assertEquals(taka(1_650), LedgerCalculator.accountBalance(afterEdit, cash))
        assertEquals(taka(650), LedgerCalculator.incomeTotal(afterEdit))
    }

    @Test
    fun scenario10_refundReducesExpenseNotIncome() {
        val postings = PostingFactory.openingAccountBalance(cash, taka(2_000)) +
            PostingFactory.expense(cash, catFood, taka(800)) +
            PostingFactory.expenseRefund(cash, catFood, taka(300))

        assertEquals(taka(500), LedgerCalculator.expenseTotal(postings))
        assertEquals(0L, LedgerCalculator.incomeTotal(postings))
        assertEquals(taka(1_500), LedgerCalculator.accountBalance(postings, cash))
    }

    @Test
    fun everyEventBalancesAndTrialBalanceIsZero() {
        val all = mutableListOf<PostingDraft>()
        all += PostingFactory.openingAccountBalance(cash, taka(50_000))
        all += PostingFactory.income(cash, catSalary, taka(30_000))
        all += PostingFactory.expense(cash, catFood, taka(4_250))
        all += PostingFactory.transfer(cash, bkash, taka(5_000), taka(25), catInterest)
        all += PostingFactory.shopCreditPurchase(shop, catFood, taka(2_300), taka(1_000), cash)
        all += PostingFactory.shopPayment(shop, bkash, taka(800))
        all += PostingFactory.loanReceive(loan, bkash, taka(20_000))
        all += PostingFactory.loanRepayment(loan, bkash, taka(2_000), taka(180), catInterest, taka(20), catInterest)
        all += PostingFactory.lendOut(lending, cash, taka(3_000))
        all += PostingFactory.lendReturn(lending, cash, taka(1_500), taka(0), null)
        all += PostingFactory.transfer(cash, savings, taka(7_000))
        assertEquals(0L, LedgerCalculator.trialBalance(all))
    }

    @Test
    fun invalidEventsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PostingFactory.transfer(cash, cash, taka(100)) // same account
        }
        assertThrows(IllegalArgumentException::class.java) {
            PostingFactory.expense(cash, catFood, 0) // zero amount
        }
        assertThrows(IllegalArgumentException::class.java) {
            PostingFactory.shopCreditPurchase(shop, catFood, taka(100), taka(150), cash) // paid > total
        }
        assertThrows(IllegalArgumentException::class.java) {
            PostingFactory.loanRepayment(loan, cash, 0, taka(10), null, 0, null) // interest w/o category
        }
    }
}
