package com.shohankhan.hisabghor.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.designsystem.CategoryDonutChart
import com.shohankhan.hisabghor.core.designsystem.ChartLegend
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.Emerald600
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.IncomeExpenseBarChart
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.designsystem.colorFromHex
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.CategoryTotal
import com.shohankhan.hisabghor.data.MonthCashFlow
import com.shohankhan.hisabghor.data.NetWorth
import com.shohankhan.hisabghor.data.ReportRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

enum class ReportPreset { THIS_MONTH, LAST_MONTH, THIS_YEAR, ALL_TIME, CUSTOM }

data class ReportsUiState(
    val preset: ReportPreset = ReportPreset.THIS_MONTH,
    val customFrom: LocalDate = Dates.today().withDayOfMonth(1),
    val customTo: LocalDate = Dates.today(),
    val incomePoisha: Long = 0,
    val expensePoisha: Long = 0,
    val expenseByCategory: List<CategoryTotal> = emptyList(),
    val incomeByCategory: List<CategoryTotal> = emptyList(),
    val monthly: List<MonthCashFlow> = emptyList(),
    val monthsCount: Int = 6,
    val netWorth: NetWorth? = null,
    val cashPoisha: Long = 0,
    val bankMfsPoisha: Long = 0,
    val savingsPoisha: Long = 0
) {
    val netPoisha: Long get() = incomePoisha - expensePoisha
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(private val reports: ReportRepository) : ViewModel() {

    private data class Controls(
        val preset: ReportPreset = ReportPreset.THIS_MONTH,
        val customFrom: LocalDate = Dates.today().withDayOfMonth(1),
        val customTo: LocalDate = Dates.today(),
        val monthsCount: Int = 6
    )

    private val _controls = MutableStateFlow(Controls())

    private fun rangeFor(controls: Controls): Pair<Long, Long> {
        val now = Dates.nowMs()
        return when (controls.preset) {
            ReportPreset.THIS_MONTH -> Dates.monthRange(now)
            ReportPreset.LAST_MONTH -> {
                val firstOfThis = Dates.toLocalDate(now).withDayOfMonth(1)
                val firstOfLast = firstOfThis.minusMonths(1)
                Dates.startOfDayMs(firstOfLast) to Dates.startOfDayMs(firstOfThis)
            }
            ReportPreset.THIS_YEAR -> Dates.yearRange(now)
            ReportPreset.ALL_TIME -> 0L to Long.MAX_VALUE
            ReportPreset.CUSTOM ->
                Dates.startOfDayMs(controls.customFrom) to
                    Dates.startOfDayMs(controls.customTo.plusDays(1))
        }
    }

    val uiState: StateFlow<ReportsUiState> = _controls.flatMapLatest { controls ->
        val (fromMs, toMs) = rangeFor(controls)
        combine(
            reports.observePeriodIncome(fromMs, toMs),
            reports.observePeriodExpense(fromMs, toMs),
            reports.observeCategoryBreakdown(LedgerType.EXPENSE_CATEGORY, fromMs, toMs),
            reports.observeCategoryBreakdown(LedgerType.INCOME_CATEGORY, fromMs, toMs),
            flow { emit(reports.monthlyCashFlow(controls.monthsCount)) },
            reports.observeNetWorth(),
            reports.observeCashBalance(),
            reports.observeBankAndMfsBalance(),
            reports.observeSavingsBalance()
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            ReportsUiState(
                preset = controls.preset,
                customFrom = controls.customFrom,
                customTo = controls.customTo,
                incomePoisha = values[0] as Long,
                expensePoisha = values[1] as Long,
                expenseByCategory = values[2] as List<CategoryTotal>,
                incomeByCategory = values[3] as List<CategoryTotal>,
                monthly = values[4] as List<MonthCashFlow>,
                monthsCount = controls.monthsCount,
                netWorth = values[5] as NetWorth,
                cashPoisha = values[6] as Long,
                bankMfsPoisha = values[7] as Long,
                savingsPoisha = values[8] as Long
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun setPreset(preset: ReportPreset) = _controls.update { it.copy(preset = preset) }
    fun setCustomFrom(date: LocalDate) = _controls.update { it.copy(customFrom = date) }
    fun setCustomTo(date: LocalDate) = _controls.update { it.copy(customTo = date) }
    fun setMonthsCount(months: Int) = _controls.update { it.copy(monthsCount = months) }
}

@Composable
fun ReportsScreen(onOpenNetWorthItems: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: ReportsViewModel = viewModel(
        factory = hisabViewModelFactory { ReportsViewModel(container.reportRepository) }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { HisabTopBar(title = "রিপোর্ট") }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.preset == ReportPreset.THIS_MONTH,
                    onClick = { viewModel.setPreset(ReportPreset.THIS_MONTH) },
                    label = { Text("এই মাস") }
                )
                FilterChip(
                    selected = state.preset == ReportPreset.LAST_MONTH,
                    onClick = { viewModel.setPreset(ReportPreset.LAST_MONTH) },
                    label = { Text("গত মাস") }
                )
                FilterChip(
                    selected = state.preset == ReportPreset.THIS_YEAR,
                    onClick = { viewModel.setPreset(ReportPreset.THIS_YEAR) },
                    label = { Text("এই বছর") }
                )
                FilterChip(
                    selected = state.preset == ReportPreset.ALL_TIME,
                    onClick = { viewModel.setPreset(ReportPreset.ALL_TIME) },
                    label = { Text("সব সময়") }
                )
                FilterChip(
                    selected = state.preset == ReportPreset.CUSTOM,
                    onClick = { viewModel.setPreset(ReportPreset.CUSTOM) },
                    label = { Text("তারিখ বেছে নিন") }
                )
            }
            if (state.preset == ReportPreset.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        DateField(
                            date = state.customFrom,
                            onDateChange = viewModel::setCustomFrom,
                            label = "শুরু"
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        DateField(
                            date = state.customTo,
                            onDateChange = viewModel::setCustomTo,
                            label = "শেষ"
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("এই সময়ের সারাংশ", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("মোট আয়")
                        Text(formatAmount(state.incomePoisha), color = Emerald600)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("মোট খরচ")
                        Text(formatAmount(state.expensePoisha), color = Danger600)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("হাতে থাকল (আয় − খরচ)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            formatAmount(state.netPoisha),
                            color = if (state.netPoisha >= 0) Emerald600 else Danger600,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        "ট্রান্সফার, সঞ্চয়, ধার ও ঋণের আসল এই হিসাবে আয়/খরচ হিসাবে ধরা হয়নি।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "কোথায় খরচ হলো",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.expenseByCategory.isEmpty()) {
                        Text(
                            "এই সময়ে কোনো খরচ নেই।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        CategoryDonutChart(parts = state.expenseByCategory)
                        Spacer(Modifier.height(8.dp))
                        CategoryRows(state.expenseByCategory)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("কোথা থেকে আয় এল", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (state.incomeByCategory.isEmpty()) {
                        Text(
                            "এই সময়ে কোনো আয় নেই।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        CategoryRows(state.incomeByCategory)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("মাস অনুযায়ী আয়-খরচ", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = state.monthsCount == 6,
                                onClick = { viewModel.setMonthsCount(6) },
                                label = { Text("৬ মাস") }
                            )
                            FilterChip(
                                selected = state.monthsCount == 12,
                                onClick = { viewModel.setMonthsCount(12) },
                                label = { Text("১২ মাস") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    IncomeExpenseBarChart(data = state.monthly)
                    ChartLegend()
                    Spacer(Modifier.height(6.dp))
                    state.monthly.forEach { month ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(month.yearMonthLabel, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "আয় ${formatAmount(month.incomePoisha)} • খরচ ${formatAmount(month.expensePoisha)} • হাতে ${formatAmount(month.netPoisha)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            state.netWorth?.let { netWorth ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("নেট সম্পদ", style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatAmount(netWorth.netWorthPoisha),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (netWorth.netWorthPoisha >= 0) Emerald600 else Danger600
                        )
                        Spacer(Modifier.height(6.dp))
                        KvRow("সব অ্যাকাউন্টের ব্যালান্স", formatAmount(netWorth.accountsPoisha))
                        KvRow("অন্যদের কাছে পাওনা", formatAmount(netWorth.receivablesPoisha))
                        KvRow("অতিরিক্ত সম্পদ (জমি/সোনা)", formatAmount(netWorth.manualAssetsPoisha))
                        KvRow("মোট সম্পদ", formatAmount(netWorth.totalAssets))
                        KvRow("ঋণ ও ধার বাকি", formatAmount(netWorth.loansPoisha))
                        KvRow("দোকানের বাকি", formatAmount(netWorth.shopDuesPoisha))
                        KvRow("অতিরিক্ত দায়", formatAmount(netWorth.manualLiabilitiesPoisha))
                        KvRow("মোট দায়", formatAmount(netWorth.totalLiabilities))
                        Spacer(Modifier.height(6.dp))
                        KvRow("হাতে নগদ", formatAmount(state.cashPoisha))
                        KvRow("ব্যাংক ও মোবাইল ব্যাংকিং", formatAmount(state.bankMfsPoisha))
                        KvRow("সঞ্চয় অ্যাকাউন্টে", formatAmount(state.savingsPoisha))
                        Text(
                            "সঞ্চয়ের টাকা অ্যাকাউন্ট ব্যালান্সের মধ্যেই আছে — দুবার গণনা হয়নি।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onOpenNetWorthItems) {
                            Text("অতিরিক্ত সম্পদ ও দায় সাজান")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CategoryRows(totals: List<CategoryTotal>) {
    val total = totals.sumOf { kotlin.math.abs(it.totalPoisha) }
    Column(Modifier.fillMaxWidth()) {
        totals.forEach { item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(12.dp)) { drawCircle(colorFromHex(item.colorHex)) }
                Text(
                    item.categoryName,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                val percent = if (total > 0)
                    (kotlin.math.abs(item.totalPoisha) * 10000 / total).toInt() else 0
                Text(
                    BengaliNumber.formatPercentBp(percent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(formatAmount(item.totalPoisha), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
