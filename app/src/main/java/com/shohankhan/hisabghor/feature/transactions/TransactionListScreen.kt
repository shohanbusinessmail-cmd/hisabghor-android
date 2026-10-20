package com.shohankhan.hisabghor.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.displaySign
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgChip
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.Space
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.TransactionKind
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class PeriodPreset(val label: String) {
    ALL("সব সময়"), TODAY("আজ"), LAST_7_DAYS("গত ৭ দিন"),
    THIS_MONTH("এই মাস"), THIS_YEAR("এই বছর")
}

data class TransactionFilter(
    val query: String = "",
    val period: PeriodPreset = PeriodPreset.ALL,
    val kind: TransactionKind? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null
)

data class TransactionListUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val filter: TransactionFilter = TransactionFilter()
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModel(
    private val transactions: TransactionRepository,
    accounts: AccountRepository,
    categories: CategoryRepository
) : ViewModel() {

    private val filterFlow = MutableStateFlow(TransactionFilter())

    private fun rangeFor(period: PeriodPreset): Pair<Long?, Long?> {
        val now = Dates.nowMs()
        return when (period) {
            PeriodPreset.ALL -> null to null
            PeriodPreset.TODAY -> Dates.dayRange(now).let { it.first to it.second }
            PeriodPreset.LAST_7_DAYS ->
                Dates.startOfDayMs(Dates.today().minusDays(6)) to Dates.dayRange(now).second
            PeriodPreset.THIS_MONTH -> Dates.monthRange(now).let { it.first to it.second }
            PeriodPreset.THIS_YEAR -> Dates.yearRange(now).let { it.first to it.second }
        }
    }

    val uiState: StateFlow<TransactionListUiState> = combine(
        filterFlow.flatMapLatest { filter ->
            val (from, to) = rangeFor(filter.period)
            transactions.observeFiltered(
                kind = filter.kind,
                categoryId = filter.categoryId,
                accountId = filter.accountId,
                fromMs = from,
                toMs = to,
                q = filter.query.trim()
            )
        },
        accounts.observeAccountsWithBalances(),
        categories.observeAll(),
        filterFlow
    ) { txns, accountList, categoryList, filter ->
        TransactionListUiState(
            transactions = txns,
            accounts = accountList.map { it.account },
            categories = categoryList,
            filter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionListUiState())

    fun updateFilter(change: (TransactionFilter) -> TransactionFilter) {
        filterFlow.update(change)
    }
}

@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: TransactionListViewModel = viewModel(
        factory = hisabViewModelFactory {
            TransactionListViewModel(
                container.transactionRepository,
                container.accountRepository,
                container.categoryRepository
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val filter = state.filter
    val accountNames = state.accounts.associate { it.id to it.name }

    HgScreen(
        title = "লেনদেন",
        subtitle = summaryLine(state),
        insetBottom = false,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = { text -> viewModel.updateFilter { it.copy(query = text) } },
                placeholder = { Text("শিরোনাম বা মন্তব্য খুঁজুন") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (filter.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateFilter { it.copy(query = "") } }) {
                            Icon(Icons.Filled.Clear, contentDescription = "লেখা মুছুন")
                        }
                    }
                } else null,
                singleLine = true,
                shape = Radius.field,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding, vertical = Space.sm)
            )

            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                PeriodPreset.entries.forEach { preset ->
                    HgChip(
                        text = preset.label,
                        selected = filter.period == preset,
                        onClick = { viewModel.updateFilter { it.copy(period = preset) } }
                    )
                }
            }

            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = ScreenPadding, vertical = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                kindOptions.forEach { (label, kind) ->
                    HgChip(
                        text = label,
                        selected = filter.kind == kind,
                        onClick = { viewModel.updateFilter { it.copy(kind = kind) } }
                    )
                }
            }

            Row(
                Modifier.padding(horizontal = ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                SelectionField(
                    label = "অ্যাকাউন্ট",
                    selectedText = state.accounts.firstOrNull { it.id == filter.accountId }?.name ?: "সব অ্যাকাউন্ট",
                    options = listOf<AccountEntity?>(null) + state.accounts,
                    optionLabel = { it?.name ?: "সব অ্যাকাউন্ট" },
                    onSelect = { selected -> viewModel.updateFilter { it.copy(accountId = selected?.id) } },
                    modifier = Modifier.weight(1f)
                )
                SelectionField(
                    label = "ক্যাটাগরি",
                    selectedText = state.categories.firstOrNull { it.id == filter.categoryId }?.name ?: "সব ক্যাটাগরি",
                    options = listOf<CategoryEntity?>(null) + state.categories,
                    optionLabel = { it?.name ?: "সব ক্যাটাগরি" },
                    onSelect = { selected -> viewModel.updateFilter { it.copy(categoryId = selected?.id) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Space.sm))

            if (state.transactions.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
                    EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = if (filter.query.isBlank() && filter.period == PeriodPreset.ALL)
                            "এখনো কোনো লেনদেন নেই"
                        else "এই শর্তে কিছু পাওয়া যায়নি",
                        subtitle = "ফিল্টার বদলান অথবা নতুন লেনদেন যোগ করুন।",
                        actionLabel = "নতুন লেনদেন যোগ করুন",
                        onAction = onAddTransaction
                    )
                }
            } else {
                val grouped = state.transactions.groupBy { Dates.formatDate(it.occurredAt) }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    grouped.forEach { (date, txns) ->
                        item(key = "header-$date") {
                            DayHeader(
                                date = date,
                                income = txns.filter { it.kind.displaySign() > 0 }.sumOf { it.amountPoisha },
                                expense = txns.filter { it.kind.displaySign() < 0 }.sumOf { it.amountPoisha }
                            )
                        }
                        items(txns, key = { it.id }) { txn ->
                            Column(Modifier.padding(horizontal = ScreenPadding)) {
                                val account = txn.accountId?.let { accountNames[it] }
                                TransactionRow(
                                    transaction = txn,
                                    onClick = { onOpenTransaction(txn.id) },
                                    accountName = account
                                )
                                HgDivider(startInset = 52)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val kindOptions = listOf<Pair<String, TransactionKind?>>(
    "সব ধরন" to null,
    "আয়" to TransactionKind.INCOME,
    "খরচ" to TransactionKind.EXPENSE,
    "ট্রান্সফার" to TransactionKind.TRANSFER,
    "বাকি ক্রয়" to TransactionKind.SHOP_PURCHASE_CREDIT,
    "ঋণ পরিশোধ" to TransactionKind.LOAN_REPAYMENT,
    "সঞ্চয়" to TransactionKind.SAVINGS_DEPOSIT
)

private fun summaryLine(state: TransactionListUiState): String {
    if (state.transactions.isEmpty()) return "সব লেনদেন এক জায়গায়"
    val income = state.transactions.filter { it.kind.displaySign() > 0 }.sumOf { it.amountPoisha }
    val expense = state.transactions.filter { it.kind.displaySign() < 0 }.sumOf { it.amountPoisha }
    return "আয় ${formatAmount(income)} • খরচ ${formatAmount(expense)}"
}

@Composable
private fun DayHeader(date: String, income: Long, expense: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = Space.lg, bottom = Space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            date,
            style = MaterialTheme.typography.labelLarge,
            color = Hg.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        if (income > 0) {
            Text(
                "+${formatAmount(income)}",
                style = MaterialTheme.typography.labelSmall,
                color = Hg.colors.income
            )
            Spacer(Modifier.width(Space.sm))
        }
        if (expense > 0) {
            Text(
                "−${formatAmount(expense)}",
                style = MaterialTheme.typography.labelSmall,
                color = Hg.colors.expense
            )
        }
    }
}
