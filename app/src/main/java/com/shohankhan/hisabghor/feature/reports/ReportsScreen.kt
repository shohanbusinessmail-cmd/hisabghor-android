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
import androidx.compose.foundation.layout.PaddingValues
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgChip
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgSegmented
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

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
    val presets = listOf(
        ReportPreset.THIS_MONTH to "এই মাস",
        ReportPreset.LAST_MONTH to "গত মাস",
        ReportPreset.THIS_YEAR to "এই বছর",
        ReportPreset.ALL_TIME to "সব সময়",
        ReportPreset.CUSTOM to "তারিখ বেছে নিন"
    )

    HgScreen(
        title = "রিপোর্ট",
        subtitle = "আয়, খরচ ও সম্পদের হিসাব"
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Spacer(Modifier.height(Space.sm))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                presets.forEach { (preset, label) ->
                    HgChip(
                        text = label,
                        selected = state.preset == preset,
                        onClick = { viewModel.setPreset(preset) }
                    )
                }
            }
            if (state.preset == ReportPreset.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
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

            HgCard(padding = PaddingValues(Space.lg)) {
                Eyebrow("এই সময়ের সারাংশ")
                Spacer(Modifier.height(Space.sm))
                SummaryRow("মোট আয়", state.incomePoisha, Hg.colors.income)
                SummaryRow("মোট খরচ", state.expensePoisha, Hg.colors.expense)
                Spacer(Modifier.height(Space.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "হাতে থাকল",
                        style = MaterialTheme.typography.titleSmall,
                        color = Hg.colors.ink
                    )
                    AmountText(
                        state.netPoisha,
                        style = HgAmountType.small,
                        color = if (state.netPoisha >= 0) Hg.colors.income else Hg.colors.expense
                    )
                }
                Spacer(Modifier.height(Space.xs))
                Text(
                    "ট্রান্সফার, সঞ্চয়, ধার ও ঋণের আসল এই হিসাবে আয়/খরচ হিসেবে ধরা হয়নি।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary
                )
            }

            HgCard(padding = PaddingValues(Space.lg)) {
                Text(
                    "কোথায় খরচ হলো",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink
                )
                Spacer(Modifier.height(Space.sm))
                if (state.expenseByCategory.isEmpty()) {
                    Text(
                        "এই সময়ে কোনো খরচ নেই।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.inkSecondary
                    )
                } else {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CategoryDonutChart(parts = state.expenseByCategory)
                    }
                    Spacer(Modifier.height(Space.md))
                    CategoryRows(state.expenseByCategory)
                }
            }

            HgCard(padding = PaddingValues(Space.lg)) {
                Text(
                    "কোথা থেকে আয় এল",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink
                )
                Spacer(Modifier.height(Space.sm))
                if (state.incomeByCategory.isEmpty()) {
                    Text(
                        "এই সময়ে কোনো আয় নেই।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.inkSecondary
                    )
                } else {
                    CategoryRows(state.incomeByCategory)
                }
            }

            HgCard(padding = PaddingValues(Space.lg)) {
                Text(
                    "মাস অনুযায়ী আয়-খরচ",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink
                )
                Spacer(Modifier.height(Space.sm))
                HgSegmented(
                    options = listOf(6, 12),
                    selected = state.monthsCount,
                    label = { if (it == 6) "৬ মাস" else "১২ মাস" },
                    onSelect = { viewModel.setMonthsCount(it) }
                )
                Spacer(Modifier.height(Space.md))
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IncomeExpenseBarChart(data = state.monthly)
                }
                ChartLegend()
                Spacer(Modifier.height(Space.sm))
                state.monthly.forEach { month ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = Space.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            month.yearMonthLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
                        )
                        Text(
                            "আয় ${formatAmount(month.incomePoisha)} • খরচ ${formatAmount(month.expensePoisha)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.ink
                        )
                    }
                }
            }

            state.netWorth?.let { netWorth ->
                HgCard(padding = PaddingValues(Space.lg)) {
                    Eyebrow("নেট সম্পদ")
                    Spacer(Modifier.height(Space.xs))
                    AmountText(
                        netWorth.netWorthPoisha,
                        style = HgAmountType.large,
                        color = if (netWorth.netWorthPoisha >= 0) Hg.colors.income else Hg.colors.expense
                    )
                    Spacer(Modifier.height(Space.md))
                    NetWorthLine("সব অ্যাকাউন্টের ব্যালান্স", netWorth.accountsPoisha)
                    NetWorthLine("অন্যদের কাছে পাওনা", netWorth.receivablesPoisha)
                    NetWorthLine("অতিরিক্ত সম্পদ (জমি/সোনা)", netWorth.manualAssetsPoisha)
                    NetWorthLine("মোট সম্পদ", netWorth.totalAssets, strong = true)
                    NetWorthLine("ঋণ ও ধার বাকি", netWorth.loansPoisha)
                    NetWorthLine("দোকানের বাকি", netWorth.shopDuesPoisha)
                    NetWorthLine("অতিরিক্ত দায়", netWorth.manualLiabilitiesPoisha)
                    NetWorthLine("মোট দায়", netWorth.totalLiabilities, strong = true)
                    Spacer(Modifier.height(Space.sm))
                    Eyebrow("নগদ অবস্থান")
                    Spacer(Modifier.height(Space.xs))
                    NetWorthLine("হাতে নগদ", state.cashPoisha)
                    NetWorthLine("ব্যাংক ও মোবাইল ব্যাংকিং", state.bankMfsPoisha)
                    NetWorthLine("সঞ্চয় অ্যাকাউন্টে", state.savingsPoisha)
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "সঞ্চয়ের টাকা অ্যাকাউন্ট ব্যালান্সের মধ্যেই আছে — দুবার গণনা হয়নি।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                    Spacer(Modifier.height(Space.md))
                    HgButton(
                        text = "অতিরিক্ত সম্পদ ও দায় সাজান",
                        onClick = onOpenNetWorthItems,
                        style = HgButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
private fun SummaryRow(label: String, poisha: Long, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Hg.colors.inkSecondary)
        AmountText(poisha, style = HgAmountType.small, color = color)
    }
}

@Composable
private fun NetWorthLine(label: String, poisha: Long, strong: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = if (strong) Hg.colors.ink else Hg.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        AmountText(
            poisha,
            style = if (strong) HgAmountType.small else HgAmountType.small,
            color = Hg.colors.ink
        )
    }
}

@Composable
private fun CategoryRows(totals: List<CategoryTotal>) {
    val total = totals.sumOf { kotlin.math.abs(it.totalPoisha) }
    Column(Modifier.fillMaxWidth()) {
        totals.forEach { item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(10.dp)) { drawCircle(colorFromHex(item.colorHex)) }
                Text(
                    item.categoryName,
                    modifier = Modifier.weight(1f).padding(start = Space.sm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Hg.colors.ink
                )
                val percent = if (total > 0)
                    (kotlin.math.abs(item.totalPoisha) * 10000 / total).toInt() else 0
                Text(
                    BengaliNumber.formatPercentBp(percent),
                    style = MaterialTheme.typography.labelSmall,
                    color = Hg.colors.inkTertiary,
                    modifier = Modifier.padding(end = Space.sm)
                )
                Text(
                    formatAmount(item.totalPoisha),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Hg.colors.ink
                )
            }
        }
    }
}
