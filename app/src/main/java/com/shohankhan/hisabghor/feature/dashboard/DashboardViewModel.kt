package com.shohankhan.hisabghor.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.database.UpcomingInstallmentRow
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.security.SecurityRepository
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.BudgetWithUsage
import com.shohankhan.hisabghor.data.CategoryTotal
import com.shohankhan.hisabghor.data.GoalWithProgress
import com.shohankhan.hisabghor.data.MonthCashFlow
import com.shohankhan.hisabghor.data.NetWorth
import com.shohankhan.hisabghor.data.PartyRepository
import com.shohankhan.hisabghor.data.PlanningRepository
import com.shohankhan.hisabghor.data.ReportRepository
import com.shohankhan.hisabghor.data.SavingsRepository
import com.shohankhan.hisabghor.data.ShopRepository
import com.shohankhan.hisabghor.data.LoanRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val hideAmounts: Boolean = false,
    val totalBalancePoisha: Long = 0,
    val cashPoisha: Long = 0,
    val bankMfsPoisha: Long = 0,
    val savingsPoisha: Long = 0,
    val todayIncomePoisha: Long = 0,
    val todayExpensePoisha: Long = 0,
    val monthIncomePoisha: Long = 0,
    val monthExpensePoisha: Long = 0,
    val receivablesPoisha: Long = 0,
    val payablesPoisha: Long = 0,
    val shopDuesPoisha: Long = 0,
    val netWorth: NetWorth? = null,
    val upcomingInstallments: List<UpcomingInstallmentRow> = emptyList(),
    val budgets: List<BudgetWithUsage> = emptyList(),
    val goals: List<GoalWithProgress> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val monthlyCashFlow: List<MonthCashFlow> = emptyList(),
    val monthExpenseByCategory: List<CategoryTotal> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val db: HisabGhorDatabase,
    accounts: AccountRepository,
    reports: ReportRepository,
    parties: PartyRepository,
    loans: LoanRepository,
    shops: ShopRepository,
    planning: PlanningRepository,
    savings: SavingsRepository,
    transactions: TransactionRepository,
    private val security: SecurityRepository
) : ViewModel() {

    private val now = Dates.nowMs()
    private val todayRange = Dates.dayRange(now)
    private val monthRange = Dates.monthRange(now)
    private val upcomingHorizon = Dates.startOfDayMs(Dates.today().plusDays(15)) - 1

    private val accountTotals = accounts.observeAccountsWithBalances().map { list ->
        AccountTotals(
            total = list.sumOf { it.balancePoisha },
            cash = list.filter { it.account.type == AccountType.CASH }.sumOf { it.balancePoisha },
            bankMfs = list.filter {
                it.account.type in setOf(
                    AccountType.BANK, AccountType.BKASH, AccountType.NAGAD,
                    AccountType.ROCKET, AccountType.MFS_OTHER
                )
            }.sumOf { it.balancePoisha },
            savings = list.filter { it.account.type == AccountType.SAVINGS }.sumOf { it.balancePoisha }
        )
    }

    private val periodTotals = combine(
        reports.observePeriodIncome(todayRange.first, todayRange.second),
        reports.observePeriodExpense(todayRange.first, todayRange.second),
        reports.observePeriodIncome(monthRange.first, monthRange.second),
        reports.observePeriodExpense(monthRange.first, monthRange.second)
    ) { ti, te, mi, me -> PeriodTotals(ti, te, mi, me) }

    private val positionTotals = combine(
        parties.observeTotalReceivables(),
        loans.observeTotalPayables(),
        shops.observeTotalShopDue(),
        reports.observeNetWorth()
    ) { receivables, payables, shopDues, netWorth ->
        PositionTotals(receivables, payables, shopDues, netWorth)
    }

    private val listsFlow = combine(
        db.installmentDao().observeUpcoming(upcomingHorizon, limit = 5),
        planning.observeBudgetsWithUsage(),
        savings.observeGoalsWithProgress(),
        transactions.observeRecent(8)
    ) { upcoming, budgets, goals, recent ->
        ListsPart(upcoming, budgets.take(3), goals.take(3), recent)
    }

    private val chartFlow = combine(
        reports.observeCategoryBreakdown(LedgerType.EXPENSE_CATEGORY, monthRange.first, monthRange.second),
        transactions.observeRecent(1).flatMapLatest {
            flow { emit(reports.monthlyCashFlow(6)) }
        },
        security.settings
    ) { categories, monthly, settings ->
        ChartPart(categories, monthly, settings.hideAmountsOnDashboard)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountTotals, periodTotals, positionTotals, listsFlow, chartFlow
    ) { acc, period, position, lists, chart ->
        DashboardUiState(
            loading = false,
            hideAmounts = chart.hideAmounts,
            totalBalancePoisha = acc.total,
            cashPoisha = acc.cash,
            bankMfsPoisha = acc.bankMfs,
            savingsPoisha = acc.savings,
            todayIncomePoisha = period.todayIncome,
            todayExpensePoisha = period.todayExpense,
            monthIncomePoisha = period.monthIncome,
            monthExpensePoisha = period.monthExpense,
            receivablesPoisha = position.receivables,
            payablesPoisha = position.payables,
            shopDuesPoisha = position.shopDues,
            netWorth = position.netWorth,
            upcomingInstallments = lists.upcoming,
            budgets = lists.budgets,
            goals = lists.goals,
            recentTransactions = lists.recent,
            monthlyCashFlow = chart.monthly,
            monthExpenseByCategory = chart.categories
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun setHideAmounts(hide: Boolean) {
        viewModelScope.launch { security.setHideAmounts(hide) }
    }

    private data class AccountTotals(val total: Long, val cash: Long, val bankMfs: Long, val savings: Long)
    private data class PeriodTotals(val todayIncome: Long, val todayExpense: Long, val monthIncome: Long, val monthExpense: Long)
    private data class PositionTotals(val receivables: Long, val payables: Long, val shopDues: Long, val netWorth: NetWorth)
    private data class ListsPart(
        val upcoming: List<UpcomingInstallmentRow>,
        val budgets: List<BudgetWithUsage>,
        val goals: List<GoalWithProgress>,
        val recent: List<TransactionEntity>
    )
    private data class ChartPart(
        val categories: List<CategoryTotal>,
        val monthly: List<MonthCashFlow>,
        val hideAmounts: Boolean
    )
}
