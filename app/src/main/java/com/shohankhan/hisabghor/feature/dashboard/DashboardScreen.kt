package com.shohankhan.hisabghor.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.UpcomingInstallmentRow
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.CategoryBreakdownBars
import com.shohankhan.hisabghor.core.designsystem.ChartLegend
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgListRow
import com.shohankhan.hisabghor.core.designsystem.IncomeExpenseBarChart
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.QuickAction
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.SectionHeader
import com.shohankhan.hisabghor.core.designsystem.Space
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.TransactionRow
import java.time.LocalTime

@Composable
fun DashboardScreen(
    onSearch: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onTransfer: () -> Unit,
    onOpenShops: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenLoans: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenReports: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: DashboardViewModel = viewModel(
        factory = hisabViewModelFactory {
            DashboardViewModel(
                db = container.database,
                accounts = container.accountRepository,
                reports = container.reportRepository,
                parties = container.partyRepository,
                loans = container.loanRepository,
                shops = container.shopRepository,
                planning = container.planningRepository,
                savings = container.savingsRepository,
                transactions = container.transactionRepository,
                security = container.securityRepository
            )
        }
    )
    val state by viewModel.uiState.collectAsState()

    if (state.loading) {
        LoadingState()
        return
    }
    val hide = state.hideAmounts
    val now = Dates.nowMs()
    val firstRun = state.recentTransactions.isEmpty() &&
        state.totalBalancePoisha == 0L &&
        state.cashPoisha == 0L

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Hg.colors.canvas)
            .statusBarsPadding()
    ) {
        item {
            DashboardHeader(
                hideAmounts = hide,
                onToggleHide = { viewModel.setHideAmounts(!hide) },
                onSearch = onSearch
            )
        }

        item {
            BalancePanel(state = state, hide = hide)
        }

        item {
            QuickActionsRow(
                onAddIncome = onAddIncome,
                onAddExpense = onAddExpense,
                onTransfer = onTransfer,
                onOpenShops = onOpenShops
            )
        }

        val reminder = attentionItems(state)
        if (reminder.isNotEmpty()) {
            item {
                AttentionCard(items = reminder, onOpenBudgets = onOpenBudgets, onOpenLoans = onOpenLoans)
            }
        }

        if (firstRun) {
            item {
                FirstRunCard(
                    onAddIncome = onAddIncome,
                    onAddExpense = onAddExpense
                )
            }
        }

        item {
            HgCard(
                Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "গত ৬ মাসের আয় ও খরচ",
                            style = MaterialTheme.typography.titleMedium,
                            color = Hg.colors.ink
                        )
                        Text(
                            "মাসিক মোট",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
                        )
                    }
                    ChartLegend()
                }
                Spacer(Modifier.height(Space.md))
                if (state.monthlyCashFlow.any { it.incomePoisha > 0 || it.expensePoisha > 0 }) {
                    IncomeExpenseBarChart(state.monthlyCashFlow)
                } else {
                    InlineHint("আয় বা খরচ যোগ করলে এখানে মাসভিত্তিক তুলনা দেখা যাবে।")
                }
                Spacer(Modifier.height(Space.sm))
                HgDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Space.md),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MonthStat(
                        label = "এই মাসের আয়",
                        amount = state.monthIncomePoisha,
                        color = Hg.colors.income,
                        hide = hide,
                        modifier = Modifier.weight(1f)
                    )
                    MonthStat(
                        label = "এই মাসের খরচ",
                        amount = state.monthExpensePoisha,
                        color = Hg.colors.expense,
                        hide = hide,
                        modifier = Modifier.weight(1f)
                    )
                    MonthStat(
                        label = "এই মাসে বাকি",
                        amount = state.monthIncomePoisha - state.monthExpensePoisha,
                        color = Hg.colors.ink,
                        hide = hide,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (state.monthExpenseByCategory.isNotEmpty()) {
            item {
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm),
                    onClick = onOpenReports
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "এই মাসে কোথায় খরচ হয়েছে",
                            style = MaterialTheme.typography.titleMedium,
                            color = Hg.colors.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "রিপোর্ট",
                            style = MaterialTheme.typography.labelLarge,
                            color = Hg.colors.brand
                        )
                    }
                    Spacer(Modifier.height(Space.md))
                    CategoryBreakdownBars(state.monthExpenseByCategory, maxItems = 4, hidden = hide)
                }
            }
        }

        item {
            PositionCard(
                state = state,
                hide = hide,
                onOpenDebts = onOpenDebts,
                onOpenLoans = onOpenLoans,
                onOpenShops = onOpenShops
            )
        }

        if (state.goals.isNotEmpty()) {
            item {
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm),
                    onClick = onOpenSavings
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "সঞ্চয়ের লক্ষ্য",
                            style = MaterialTheme.typography.titleMedium,
                            color = Hg.colors.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "সব দেখুন",
                            style = MaterialTheme.typography.labelLarge,
                            color = Hg.colors.brand
                        )
                    }
                    Spacer(Modifier.height(Space.md))
                    state.goals.forEachIndexed { index, goal ->
                        if (index > 0) Spacer(Modifier.height(Space.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    goal.goal.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Hg.colors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${if (hide) "৳••••" else formatAmount(goal.savedPoisha)} / " +
                                        (if (hide) "৳••••" else formatAmount(goal.goal.targetPoisha)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Hg.colors.inkSecondary
                                )
                            }
                            Spacer(Modifier.width(Space.md))
                            Text(
                                BengaliNumber.toBengaliDigits(
                                    "${(goal.progressFraction * 100).toInt()}%"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = Hg.colors.brand
                            )
                        }
                        Spacer(Modifier.height(Space.sm))
                        com.shohankhan.hisabghor.core.designsystem.HgProgressBar(
                            fraction = goal.progressFraction,
                            height = 6
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenPadding, end = ScreenPadding - 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "সাম্প্রতিক লেনদেন",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.TextButton(onClick = onSeeAllTransactions) {
                    Text("সব দেখুন", color = Hg.colors.brand)
                }
            }
        }

        if (state.recentTransactions.isEmpty()) {
            item {
                HgCard(Modifier.padding(horizontal = ScreenPadding)) {
                    Text(
                        "এখনো কোনো লেনদেন নেই।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "আয় বা খরচ যোগ করলে সর্বশেষ এন্ট্রিগুলো এখানে দেখা যাবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
            }
        } else {
            item {
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding),
                    padding = androidx.compose.foundation.layout.PaddingValues(vertical = Space.xs)
                ) {
                    state.recentTransactions.forEachIndexed { index, txn ->
                        TransactionRow(
                            transaction = txn,
                            onClick = { onOpenTransaction(txn.id) },
                            hideAmount = hide
                        )
                        if (index != state.recentTransactions.lastIndex) {
                            HgDivider(startInset = ScreenPadding.value.toInt() - 4)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "সব তথ্য শুধু এই ফোনে সংরক্ষিত • ${Dates.formatShortDate(now)}",
                style = MaterialTheme.typography.bodySmall,
                color = Hg.colors.inkTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding, vertical = Space.xxl),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun DashboardHeader(
    hideAmounts: Boolean,
    onToggleHide: () -> Unit,
    onSearch: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = Space.sm, top = Space.md, bottom = Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Hg.colors.brand),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "হ",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    "হিসাবঘর",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink
                )
                Text(
                    "${greeting()} • ${Dates.formatShortDate(Dates.nowMs())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleHide) {
                Icon(
                    if (hideAmounts) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (hideAmounts) "টাকার অঙ্ক দেখান" else "টাকার অঙ্ক লুকান",
                    tint = Hg.colors.inkSecondary
                )
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "খুঁজুন", tint = Hg.colors.inkSecondary)
            }
        }
    }
}

private fun greeting(): String {
    val hour = LocalTime.now(Dates.dhaKaZone).hour
    return when (hour) {
        in 4..11 -> "শুভ সকাল"
        in 12..15 -> "শুভ দুপুর"
        in 16..17 -> "শুভ বিকেল"
        in 18..19 -> "শুভ সন্ধ্যা"
        else -> "শুভ রাত্রি"
    }
}

// ---------------------------------------------------------------------------
// Balance
// ---------------------------------------------------------------------------

@Composable
private fun BalancePanel(
    state: DashboardUiState,
    hide: Boolean
) {
    HgCard(
        Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs),
        padding = androidx.compose.foundation.layout.PaddingValues(Space.xl)
    ) {
        Eyebrow("মোট ব্যালেন্স")
        Spacer(Modifier.height(Space.xs))
        AmountText(
            state.totalBalancePoisha,
            style = HgAmountType.hero,
            color = Hg.colors.ink,
            hidden = hide
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "সব অ্যাকাউন্ট মিলিয়ে",
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.inkSecondary
        )
        Spacer(Modifier.height(Space.lg))
        HgDivider()
        Spacer(Modifier.height(Space.md))
        Row(Modifier.fillMaxWidth()) {
            Breakdown(
                label = "ক্যাশ",
                amount = state.cashPoisha,
                hide = hide,
                modifier = Modifier.weight(1f)
            )
            Breakdown(
                label = "ব্যাংক / মোবাইল",
                amount = state.bankMfsPoisha,
                hide = hide,
                modifier = Modifier.weight(1f)
            )
            Breakdown(
                label = "সঞ্চয়",
                amount = state.savingsPoisha,
                hide = hide,
                modifier = Modifier.weight(1f)
            )
        }
        if (state.totalBalancePoisha == 0L) {
            Spacer(Modifier.height(Space.md))
            InlineHint("অ্যাকাউন্টে টাকা যোগ করতে অ্যাকাউন্ট স্ক্রিনে শুরুর ব্যালান্স দিন বা আয় যোগ করুন।")
        }
    }
}

@Composable
private fun Breakdown(
    label: String,
    amount: Long,
    hide: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.inkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        AmountText(
            amount,
            style = HgAmountType.small,
            color = Hg.colors.ink,
            hidden = hide
        )
    }
}

@Composable
private fun MonthStat(
    label: String,
    amount: Long,
    color: Color,
    hide: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.inkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        AmountText(amount, style = HgAmountType.small, color = color, hidden = hide)
    }
}

// ---------------------------------------------------------------------------
// Quick actions
// ---------------------------------------------------------------------------

@Composable
private fun QuickActionsRow(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onTransfer: () -> Unit,
    onOpenShops: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        QuickAction(
            "আয় যোগ",
            Icons.AutoMirrored.Filled.TrendingUp,
            onAddIncome,
            Modifier.weight(1f)
        )
        QuickAction(
            "খরচ যোগ",
            Icons.AutoMirrored.Filled.TrendingDown,
            onAddExpense,
            Modifier.weight(1f)
        )
        QuickAction(
            "টাকা স্থানান্তর",
            Icons.Filled.AccountBalanceWallet,
            onTransfer,
            Modifier.weight(1f)
        )
        QuickAction(
            "বাকি খাতা",
            Icons.Filled.Storefront,
            onOpenShops,
            Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------------------
// Attention / reminders
// ---------------------------------------------------------------------------

private data class AttentionItem(
    val title: String,
    val subtitle: String,
    val amount: Long,
    val tone: AttentionTone
)

private enum class AttentionTone { Due, Budget, ShopDue }

private fun attentionItems(state: DashboardUiState): List<AttentionItem> {
    val items = mutableListOf<AttentionItem>()
    state.upcomingInstallments.take(2).forEach { row: UpcomingInstallmentRow ->
        items += AttentionItem(
            title = "${row.organizationName} • কিস্তি ${BengaliNumber.toBengaliDigits(row.seq.toString())}",
            subtitle = "শেষ তারিখ ${Dates.formatShortDate(row.dueDate)}",
            amount = row.remainingPoisha,
            tone = AttentionTone.Due
        )
    }
    state.budgets.filter { it.isOverBudget }.take(2).forEach { budget ->
        items += AttentionItem(
            title = "${budget.category?.name ?: "সামগ্রিক"} বাজেট ছাড়িয়েছে",
            subtitle = "সীমা ${formatAmount(budget.budget.amountPoisha)}",
            amount = budget.spentPoisha - budget.budget.amountPoisha,
            tone = AttentionTone.Budget
        )
    }
    if (state.shopDuesPoisha > 0) {
        items += AttentionItem(
            title = "দোকানে বাকি আছে",
            subtitle = "পরিশোধ করলে দেনা কমবে",
            amount = state.shopDuesPoisha,
            tone = AttentionTone.ShopDue
        )
    }
    return items.take(3)
}

@Composable
private fun AttentionCard(
    items: List<AttentionItem>,
    onOpenBudgets: () -> Unit,
    onOpenLoans: () -> Unit
) {
    HgCard(
        Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs),
        padding = androidx.compose.foundation.layout.PaddingValues(vertical = Space.xs)
    ) {
        Row(
            Modifier.padding(horizontal = Space.lg, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Alarm,
                contentDescription = null,
                tint = Hg.colors.warning,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Space.sm))
            Text(
                "মনে রাখুন",
                style = MaterialTheme.typography.titleSmall,
                color = Hg.colors.ink
            )
        }
        items.forEach { item ->
            val icon = when (item.tone) {
                AttentionTone.Due -> Icons.Filled.Payments
                AttentionTone.Budget -> Icons.Filled.PieChart
                AttentionTone.ShopDue -> Icons.Filled.Storefront
            }
            HgListRow(
                title = item.title,
                subtitle = item.subtitle,
                leading = {
                    IconChip(
                        icon = icon,
                        tint = Hg.colors.warning,
                        background = Hg.colors.warningTint
                    )
                },
                trailing = {
                    AmountText(
                        item.amount,
                        style = HgAmountType.small,
                        color = Hg.colors.warning
                    )
                },
                onClick = when (item.tone) {
                    AttentionTone.Budget -> onOpenBudgets
                    else -> onOpenLoans
                }
            )
        }
    }
}

@Composable
private fun FirstRunCard(onAddIncome: () -> Unit, onAddExpense: () -> Unit) {
    HgCard(Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)) {
        Text(
            "শুরু করুন",
            style = MaterialTheme.typography.titleMedium,
            color = Hg.colors.ink
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            "প্রথমে একটি আয় বা খরচ যোগ করুন। অ্যাকাউন্টের ব্যালান্স নিজে থেকেই হিসাব হবে—আলাদা করে কোথাও লিখতে হবে না।",
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.inkSecondary
        )
        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            com.shohankhan.hisabghor.core.designsystem.HgButton(
                "আয় যোগ করুন",
                onAddIncome,
                style = com.shohankhan.hisabghor.core.designsystem.HgButtonStyle.Primary,
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )
            com.shohankhan.hisabghor.core.designsystem.HgButton(
                "খরচ যোগ করুন",
                onAddExpense,
                style = com.shohankhan.hisabghor.core.designsystem.HgButtonStyle.Secondary,
                icon = Icons.AutoMirrored.Filled.TrendingDown
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Positions
// ---------------------------------------------------------------------------

@Composable
private fun PositionCard(
    state: DashboardUiState,
    hide: Boolean,
    onOpenDebts: () -> Unit,
    onOpenLoans: () -> Unit,
    onOpenShops: () -> Unit
) {
    HgCard(
        Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm),
        padding = androidx.compose.foundation.layout.PaddingValues(vertical = Space.xs)
    ) {
        Row(
            Modifier.padding(horizontal = Space.lg, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "পাওনা ও দেনা",
                style = MaterialTheme.typography.titleMedium,
                color = Hg.colors.ink,
                modifier = Modifier.weight(1f)
            )
            state.netWorth?.let { nw ->
                Text(
                    "নিট সম্পদ ${if (hide) "৳••••" else formatAmount(nw.netWorthPoisha)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary
                )
            }
        }
        HgListRow(
            title = "পাওনা (আমি পাব)",
            subtitle = "ধার দেওয়া ও ব্যক্তিগত বাকি",
            leading = { IconChip(Icons.Filled.Handshake, tint = Hg.colors.income, background = Hg.colors.incomeTint) },
            trailing = { AmountText(state.receivablesPoisha, style = HgAmountType.small, color = Hg.colors.income, hidden = hide) },
            onClick = onOpenDebts
        )
        HgListRow(
            title = "ঋণের বাকি (আমি দেব)",
            subtitle = "এনজিও, ব্যাংক ও ব্যক্তিগত ধার",
            leading = { IconChip(Icons.Filled.Payments, tint = Hg.colors.expense, background = Hg.colors.expenseTint) },
            trailing = { AmountText(state.payablesPoisha, style = HgAmountType.small, color = Hg.colors.expense, hidden = hide) },
            onClick = onOpenLoans
        )
        HgListRow(
            title = "দোকানে বকেয়া",
            subtitle = "বাকি কেনা ও পরিশোধ",
            leading = { IconChip(Icons.Filled.Storefront, tint = Hg.colors.warning, background = Hg.colors.warningTint) },
            trailing = { AmountText(state.shopDuesPoisha, style = HgAmountType.small, color = Hg.colors.warning, hidden = hide) },
            onClick = onOpenShops
        )
    }
}

// ---------------------------------------------------------------------------
// Small pieces
// ---------------------------------------------------------------------------

@Composable
private fun InlineHint(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(Radius.sm)
            .background(Hg.colors.surfaceMuted)
            .padding(Space.md)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.inkSecondary
        )
    }
}

// Kept for previews/semantics parity with earlier builds.
internal fun dashboardTodayLabel(): String = BengaliNumber.toBengaliDigits(Dates.formatDate(Dates.nowMs()))
