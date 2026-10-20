package com.shohankhan.hisabghor.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.CategoryDonutChart
import com.shohankhan.hisabghor.core.designsystem.ChartLegend
import com.shohankhan.hisabghor.core.designsystem.Emerald500
import com.shohankhan.hisabghor.core.designsystem.Emerald600
import com.shohankhan.hisabghor.core.designsystem.Emerald700
import com.shohankhan.hisabghor.core.designsystem.IncomeExpenseBarChart
import com.shohankhan.hisabghor.core.designsystem.LabeledProgress
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.colorFromHex
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.TransactionRow

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

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        item { HeroCard(state, onSearch) { viewModel.setHideAmounts(!hide) } }

        item {
            QuickActions(
                onAddIncome, onAddExpense, onTransfer, onOpenShops,
                onOpenDebts, onOpenSavings
            )
        }

        item {
            SectionCard(title = "এই মাসের সারাংশ") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("আয়", state.monthIncomePoisha, Emerald600, hide)
                    MiniStat("খরচ", state.monthExpensePoisha, Color(0xFFC62828), hide)
                    MiniStat(
                        "হাতে বাকি",
                        state.monthIncomePoisha - state.monthExpensePoisha,
                        MaterialTheme.colorScheme.onSurface, hide
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (state.monthlyCashFlow.any { it.incomePoisha > 0 || it.expensePoisha > 0 }) {
                    IncomeExpenseBarChart(state.monthlyCashFlow)
                    ChartLegend(Modifier.fillMaxWidth())
                } else {
                    EmptyHint("এখনো কোনো লেনদেন নেই — আয় বা খরচ যোগ করলে এখানে চার্ট দেখা যাবে।")
                }
            }
        }

        item {
            SectionCard(title = "অ্যাকাউন্টের অবস্থা") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("ক্যাশ", state.cashPoisha, MaterialTheme.colorScheme.onSurface, hide)
                    MiniStat("ব্যাংক/ওয়ালেট", state.bankMfsPoisha, MaterialTheme.colorScheme.onSurface, hide)
                    MiniStat("সঞ্চয়", state.savingsPoisha, Emerald600, hide)
                }
            }
        }

        if (state.monthExpenseByCategory.isNotEmpty()) {
            item {
                SectionCard(title = "এই মাসে কোথায় খরচ হলো") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryDonutChart(state.monthExpenseByCategory)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            state.monthExpenseByCategory.take(4).forEach { part ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(colorFromHex(part.colorHex))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        part.categoryName,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    AmountText(
                                        kotlin.math.abs(part.totalPoisha),
                                        style = MaterialTheme.typography.bodySmall,
                                        hidden = hide
                                    )
                                }
                            }
                        }
                    }
                    TextButton(onClick = onOpenReports) { Text("বিস্তারিত রিপোর্ট দেখুন") }
                }
            }
        }

        item {
            SectionCard(title = "পাওনা ও দেনা") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ClickableStat("পাওনা (অন্যের কাছে)", state.receivablesPoisha, Emerald600, hide, onOpenDebts)
                    ClickableStat("ঋণের বাকি", state.payablesPoisha, Color(0xFFC62828), hide, onOpenLoans)
                    ClickableStat("দোকানে বকেয়া", state.shopDuesPoisha, Color(0xFFB26A00), hide, onOpenShops)
                }
                state.netWorth?.let { nw ->
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("নিট সম্পদ (Net Worth)", style = MaterialTheme.typography.bodyMedium)
                        AmountText(
                            nw.netWorthPoisha,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (nw.netWorthPoisha >= 0) Emerald700 else Color(0xFFC62828),
                            hidden = hide
                        )
                    }
                }
            }
        }

        if (state.upcomingInstallments.isNotEmpty()) {
            item {
                SectionCard(title = "আসন্ন কিস্তি") {
                    state.upcomingInstallments.forEach { row ->
                        val overdue = row.dueDate < Dates.startOfDayMs(Dates.nowMs())
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLoans() }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.EventNote,
                                contentDescription = null,
                                tint = if (overdue) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(row.organizationName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    (if (overdue) "মেয়াদ পেরিয়ে গেছে • " else "") +
                                        Dates.formatShortDate(row.dueDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (overdue) Color(0xFFC62828)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AmountText(row.remainingPoisha, hidden = hide)
                        }
                    }
                }
            }
        }

        if (state.budgets.isNotEmpty()) {
            item {
                SectionCard(title = "বাজেটের অবস্থা") {
                    state.budgets.forEach { budget ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(
                                budget.category?.name ?: "সামগ্রিক খরচ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            LabeledProgress(
                                fraction = budget.usageFraction,
                                leftText = "খরচ ${if (hide) "৳••••" else formatAmount(budget.spentPoisha)}",
                                rightText = "বাজেট ${if (hide) "৳••••" else formatAmount(budget.budget.amountPoisha)}",
                                overLimit = budget.isOverBudget
                            )
                        }
                    }
                    TextButton(onClick = onOpenBudgets) { Text("সব বাজেট দেখুন") }
                }
            }
        }

        if (state.goals.isNotEmpty()) {
            item {
                SectionCard(title = "সঞ্চয়ের লক্ষ্য") {
                    state.goals.forEach { goal ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(goal.goal.name, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            LabeledProgress(
                                fraction = goal.progressFraction,
                                leftText = "জমেছে ${if (hide) "৳••••" else formatAmount(goal.savedPoisha)}",
                                rightText = "লক্ষ্য ${if (hide) "৳••••" else formatAmount(goal.goal.targetPoisha)}"
                            )
                        }
                    }
                    TextButton(onClick = onOpenSavings) { Text("সঞ্চয়ের বিস্তারিত") }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("সাম্প্রতিক লেনদেন", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onSeeAllTransactions) { Text("সব দেখুন") }
            }
        }
        if (state.recentTransactions.isEmpty()) {
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    EmptyHint("এখনো কোনো লেনদেন যোগ হয়নি। ওপরের বোতাম দিয়ে প্রথম আয় বা খরচ যোগ করুন।")
                }
            }
        } else {
            items(state.recentTransactions, key = { it.id }) { txn ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    TransactionRow(txn, onClick = { onOpenTransaction(txn.id) }, hideAmount = hide)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HeroCard(state: DashboardUiState, onSearch: () -> Unit, onToggleHide: () -> Unit) {
    val hide = state.hideAmounts
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Emerald500, Emerald600, Emerald700)),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .padding(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 22.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "হিসাবঘর",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        Dates.formatDate(Dates.nowMs()),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Row {
                    IconButton(onClick = onToggleHide) {
                        Icon(
                            if (hide) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (hide) "টাকার অঙ্ক দেখান" else "টাকার অঙ্ক লুকান",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onSearch) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "খুঁজুন",
                            tint = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "সর্বমোট ব্যালান্স",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            AmountText(
                state.totalBalancePoisha,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                hidden = hide
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroChip(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "আজকের আয়",
                    amount = state.todayIncomePoisha,
                    hide = hide,
                    modifier = Modifier.weight(1f)
                )
                HeroChip(
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    label = "আজকের খরচ",
                    amount = state.todayExpensePoisha,
                    hide = hide,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroChip(
    icon: ImageVector,
    label: String,
    amount: Long,
    hide: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            AmountText(
                amount,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                hidden = hide
            )
        }
    }
}

@Composable
private fun QuickActions(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onTransfer: () -> Unit,
    onOpenShops: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenSavings: () -> Unit
) {
    val actions = listOf(
        Triple("আয় যোগ", Icons.Filled.Add, onAddIncome),
        Triple("খরচ যোগ", Icons.Filled.ReceiptLong, onAddExpense),
        Triple("ট্রান্সফার", Icons.Filled.SwapHoriz, onTransfer),
        Triple("বাকি ক্রয়", Icons.Filled.Storefront, onOpenShops),
        Triple("ধার/পাওনা", Icons.Filled.Handshake, onOpenDebts),
        Triple("পরিশোধ", Icons.Filled.Payments, onOpenDebts),
        Triple("সঞ্চয়", Icons.Filled.Savings, onOpenSavings)
    )
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { (label, icon, action) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { action() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = Emerald700)
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun MiniStat(label: String, amount: Long, color: Color, hide: Boolean) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(amount, style = MaterialTheme.typography.titleSmall, color = color, hidden = hide)
    }
}

@Composable
private fun ClickableStat(label: String, amount: Long, color: Color, hide: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(amount, style = MaterialTheme.typography.titleSmall, color = color, hidden = hide)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp)
    )
}

// Used by quick-action "আয়" semantics in tests/previews.
internal fun dashboardTodayLabel(): String = BengaliNumber.toBengaliDigits(Dates.formatDate(Dates.nowMs()))
