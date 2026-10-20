package com.shohankhan.hisabghor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.model.AccountScope
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.InstallmentFrequency
import com.shohankhan.hisabghor.core.model.InterestMethod
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.AuditLogger
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.LedgerRepository
import com.shohankhan.hisabghor.data.LoanRepository
import com.shohankhan.hisabghor.data.ShopRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end ledger tests against a real (in-memory) Room database using the
 * production repositories. Verifies the core accounting invariants:
 * every transaction is balanced, transfers are neutral, shop credit is not
 * double-counted, and loan liability tracks principal only.
 */
@RunWith(AndroidJUnit4::class)
class LedgerFlowInstrumentedTest {

    private lateinit var db: HisabGhorDatabase
    private lateinit var ledger: LedgerRepository
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var transactions: TransactionRepository
    private lateinit var shops: ShopRepository
    private lateinit var loans: LoanRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HisabGhorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        ledger = LedgerRepository(db, AuditLogger(db.auditDao()))
        accounts = AccountRepository(db, ledger)
        categories = CategoryRepository(db)
        transactions = TransactionRepository(db, ledger)
        shops = ShopRepository(db, ledger)
        loans = LoanRepository(db, ledger, categories)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun accountBalance(accountId: Long): Long =
        db.postingDao().getLedgerBalance(LedgerType.ACCOUNT, accountId)

    private suspend fun assertBalanced() {
        assertEquals(0L, db.postingDao().trialBalance())
        assertEquals(0, db.postingDao().countUnbalancedTransactions())
    }

    @Test
    fun incomeExpenseTransfer_keepLedgerBalancedAndNeutral() = runBlocking {
        val cashId = accounts.createAccount(
            "ক্যাশ", AccountType.CASH, AccountScope.PERSONAL, null,
            openingBalancePoisha = 100_000 // 1000 taka
        )
        val bankId = accounts.createAccount(
            "ব্যাংক", AccountType.BANK, AccountScope.PERSONAL, null,
            openingBalancePoisha = 0
        )
        val salaryId = categories.create("বেতন", CategoryKind.INCOME, "salary", 0xFF0B7A4B)
        val foodId = categories.create("খাবার", CategoryKind.EXPENSE, "food", 0xFFC62828)
        val now = Dates.nowMs()

        transactions.addIncome(cashId, salaryId, 50_000, now, "বেতন", null)
        transactions.addExpense(
            cashId, foodId, 12_000, emptyList(), 0, 0, now, "বাজার", null
        )
        accounts.transfer(
            fromAccountId = cashId, toAccountId = bankId, amountPoisha = 20_000,
            feePoisha = 0, feeCategoryId = null, occurredAt = now, note = null
        )

        assertEquals(100_000 + 50_000 - 12_000 - 20_000L, accountBalance(cashId))
        assertEquals(20_000L, accountBalance(bankId))

        val cashFlow = accounts.observeTotalBalance().first()
        assertEquals(138_000L, cashFlow)

        // Transfer is neither income nor expense.
        val range = 0L to Long.MAX_VALUE
        val incomeTotal = -db.postingDao()
            .getPeriodTotal(LedgerType.INCOME_CATEGORY, range.first, range.second)
        val expenseTotal = db.postingDao()
            .getPeriodTotal(LedgerType.EXPENSE_CATEGORY, range.first, range.second)
        assertEquals(50_000L, incomeTotal)
        assertEquals(12_000L, expenseTotal)
        assertBalanced()
    }

    @Test
    fun shopCredit_doesNotTouchCash_andExpenseCountedOnce() = runBlocking {
        val cashId = accounts.createAccount(
            "ক্যাশ", AccountType.CASH, AccountScope.PERSONAL, null,
            openingBalancePoisha = 50_000
        )
        val foodId = categories.create("খাবার", CategoryKind.EXPENSE, "food", 0xFFC62828)
        val shopId = shops.createShop("মুদি দোকান", null, null, null, null, null, 0)
        val now = Dates.nowMs()

        // Buy 300 taka on credit, pay nothing now.
        shops.recordCreditPurchase(
            shopId = shopId, categoryId = foodId, amountPoisha = 30_000,
            items = emptyList(), extraChargePoisha = 0, discountPoisha = 0,
            paidNowPoisha = 0, accountId = null, occurredAt = now,
            title = "বাকি কেনা", note = null
        )
        assertEquals(50_000L, accountBalance(cashId))      // cash untouched
        assertEquals(30_000L, shops.observeTotalShopDue().first())

        // Pay the shop 300 taka in cash.
        shops.recordPayment(shopId, cashId, 30_000, now, null)
        assertEquals(20_000L, accountBalance(cashId))
        assertEquals(0L, shops.observeTotalShopDue().first())

        // Expense counted exactly once (at purchase, not again at payment).
        val expenseTotal = db.postingDao()
            .getPeriodTotal(LedgerType.EXPENSE_CATEGORY, 0, Long.MAX_VALUE)
        assertEquals(30_000L, expenseTotal)
        assertBalanced()
    }

    @Test
    fun installmentLoan_liabilityIsPrincipalOnly_andRepaymentSplits() = runBlocking {
        val cashId = accounts.createAccount(
            "ক্যাশ", AccountType.CASH, AccountScope.PERSONAL, null,
            openingBalancePoisha = 0
        )
        val now = Dates.nowMs()
        // 10,000 taka principal, 9,800 received (200 upfront charge),
        // flat total repayable 11,000 over 10 monthly installments of 1,100.
        val loanId = loans.createInstallmentLoan(
            kind = LoanKind.NGO,
            organizationName = "টেস্ট এনজিও",
            branch = null, maskedRef = null,
            accountId = cashId,
            principalPoisha = 1_000_000,
            disbursedPoisha = 980_000,
            interestMethod = InterestMethod.FLAT,
            interestRateBp = 0,
            totalContractualPoisha = 1_100_000,
            installmentAmountPoisha = null,
            installmentCount = 10,
            frequency = InstallmentFrequency.MONTHLY,
            startDateMs = now,
            firstDueDateMs = Dates.startOfDayMs(Dates.today().plusMonths(1)),
            note = null,
            moneyMovedNow = true
        )

        assertEquals(980_000L, accountBalance(cashId))
        val loan = loans.getLoan(loanId)!!
        assertEquals(1_000_000L, loans.observeOutstanding(loan).first())

        // Pay first installment: 1,100 taka = 1,000 principal + 100 interest.
        loans.recordRepayment(
            loanId = loanId, accountId = cashId, totalAmountPoisha = 110_000,
            occurredAt = now, interestCategoryId = null, feePoisha = 0,
            feeCategoryId = null, receiptNo = null, note = null
        )
        assertEquals(900_000L, loans.observeOutstanding(loan).first())
        assertEquals(980_000 - 110_000L, accountBalance(cashId))

        // Interest paid (100) + upfront charge (200) are expenses; principal is not.
        val expenseTotal = db.postingDao()
            .getPeriodTotal(LedgerType.EXPENSE_CATEGORY, 0, Long.MAX_VALUE)
        assertEquals(10_000 + 20_000L, expenseTotal)

        // Loan principal received is not income.
        val incomeTotal = -db.postingDao()
            .getPeriodTotal(LedgerType.INCOME_CATEGORY, 0, Long.MAX_VALUE)
        assertEquals(0L, incomeTotal)
        assertBalanced()
        assertTrue(loans.observeInstallments(loanId).first().size == 10)
    }
}
