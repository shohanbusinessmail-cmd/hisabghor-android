package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.LedgerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Dashboard + reports read models. All figures come from journal postings. */

data class CategoryTotal(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: Long,
    val totalPoisha: Long
)

data class MonthCashFlow(
    val yearMonthLabel: String,
    val fromMs: Long,
    val incomePoisha: Long,
    val expensePoisha: Long
) {
    val netPoisha: Long get() = incomePoisha - expensePoisha
}

data class NetWorth(
    val accountsPoisha: Long,
    val receivablesPoisha: Long,
    val manualAssetsPoisha: Long,
    val loansPoisha: Long,
    val shopDuesPoisha: Long,
    val manualLiabilitiesPoisha: Long
) {
    val totalAssets: Long get() = accountsPoisha + receivablesPoisha + manualAssetsPoisha
    val totalLiabilities: Long get() = loansPoisha + shopDuesPoisha + manualLiabilitiesPoisha
    val netWorthPoisha: Long get() = totalAssets - totalLiabilities
}

class ReportRepository(
    private val db: HisabGhorDatabase,
    private val ledger: LedgerRepository,
    private val accounts: AccountRepository,
    private val planning: PlanningRepository
) {

    fun observePeriodIncome(fromMs: Long, toMs: Long): Flow<Long> =
        ledger.periodTotal(LedgerType.INCOME_CATEGORY, fromMs, toMs).map { -it }

    fun observePeriodExpense(fromMs: Long, toMs: Long): Flow<Long> =
        ledger.periodTotal(LedgerType.EXPENSE_CATEGORY, fromMs, toMs)

    fun observeCashBalance(): Flow<Long> =
        accounts.observeBalanceByTypes(setOf(AccountType.CASH))

    fun observeBankAndMfsBalance(): Flow<Long> =
        accounts.observeBalanceByTypes(
            setOf(
                AccountType.BANK, AccountType.BKASH, AccountType.NAGAD,
                AccountType.ROCKET, AccountType.MFS_OTHER
            )
        )

    fun observeSavingsBalance(): Flow<Long> =
        accounts.observeBalanceByTypes(setOf(AccountType.SAVINGS))

    fun observeCategoryBreakdown(
        ledgerType: LedgerType,
        fromMs: Long,
        toMs: Long
    ): Flow<List<CategoryTotal>> = combine(
        db.postingDao().observePeriodTotalsByLedger(ledgerType, fromMs, toMs),
        db.categoryDao().observeAll(true)
    ) { rows, categories ->
        val byId = categories.associateBy { it.id }
        rows.map { row ->
            val signed = if (ledgerType == LedgerType.INCOME_CATEGORY) -row.totalPoisha else row.totalPoisha
            val category = byId[row.ledgerId]
            CategoryTotal(
                categoryId = row.ledgerId,
                categoryName = category?.name ?: "অজানা ক্যাটাগরি",
                colorHex = category?.colorHex ?: 0xFF0B7A4B,
                totalPoisha = signed
            )
        }.filter { it.totalPoisha != 0L }
            .sortedByDescending { kotlin.math.abs(it.totalPoisha) }
    }

    /**
     * Net worth = assets − liabilities. Savings are inside account balances
     * already, so goals are never added again (no double counting).
     */
    fun observeNetWorth(): Flow<NetWorth> {
        val assetsFlow = combine(
            accounts.observeTotalBalance(),
            ledger.balancesByLedger(LedgerType.RECEIVABLE),
            planning.observeManualAssetTotal()
        ) { accountTotal, receivables, manualAssets ->
            Triple(accountTotal, receivables.values.sum(), manualAssets)
        }
        val liabilitiesFlow = combine(
            ledger.balancesByLedger(LedgerType.LOAN_LIABILITY),
            ledger.balancesByLedger(LedgerType.SHOP_LIABILITY),
            planning.observeManualLiabilityTotal()
        ) { loans, shops, manualLiabilities ->
            Triple(-loans.values.sum(), -shops.values.sum(), manualLiabilities)
        }
        return combine(assetsFlow, liabilitiesFlow) { assets, liabilities ->
            NetWorth(
                accountsPoisha = assets.first,
                receivablesPoisha = assets.second,
                manualAssetsPoisha = assets.third,
                loansPoisha = liabilities.first,
                shopDuesPoisha = liabilities.second,
                manualLiabilitiesPoisha = liabilities.third
            )
        }
    }

    /** Income vs expense for the last [months] calendar months (oldest first). */
    suspend fun monthlyCashFlow(months: Int, nowMs: Long = Dates.nowMs()): List<MonthCashFlow> {
        val thisMonth = Dates.toLocalDate(nowMs).withDayOfMonth(1)
        return (months - 1 downTo 0).map { back ->
            val start = thisMonth.minusMonths(back.toLong())
            val from = Dates.startOfDayMs(start)
            val to = Dates.startOfDayMs(start.plusMonths(1))
            MonthCashFlow(
                yearMonthLabel = Dates.formatMonthYear(from),
                fromMs = from,
                incomePoisha = -ledger.periodTotalOnce(LedgerType.INCOME_CATEGORY, from, to),
                expensePoisha = ledger.periodTotalOnce(LedgerType.EXPENSE_CATEGORY, from, to)
            )
        }
    }
}
