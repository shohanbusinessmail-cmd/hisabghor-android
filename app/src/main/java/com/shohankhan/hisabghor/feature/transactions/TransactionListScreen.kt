package com.shohankhan.hisabghor.feature.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.model.TransactionKind
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
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

    Scaffold(
        topBar = { HisabTopBar(title = "লেনদেন") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন লেনদেন যোগ করুন")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = { text -> viewModel.updateFilter { it.copy(query = text) } },
                placeholder = { Text("শিরোনাম বা মন্তব্য খুঁজুন") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = filter.period == preset,
                        onClick = { viewModel.updateFilter { it.copy(period = preset) } },
                        label = { Text(preset.label) }
                    )
                }
            }

            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val kindOptions = listOf<Pair<String, TransactionKind?>>(
                    "সব ধরন" to null,
                    "আয়" to TransactionKind.INCOME,
                    "খরচ" to TransactionKind.EXPENSE,
                    "ট্রান্সফার" to TransactionKind.TRANSFER,
                    "বাকি ক্রয়" to TransactionKind.SHOP_PURCHASE_CREDIT,
                    "ঋণ পরিশোধ" to TransactionKind.LOAN_REPAYMENT,
                    "সঞ্চয় জমা" to TransactionKind.SAVINGS_DEPOSIT
                )
                kindOptions.forEach { (label, kind) ->
                    FilterChip(
                        selected = filter.kind == kind,
                        onClick = { viewModel.updateFilter { it.copy(kind = kind) } },
                        label = { Text(label) }
                    )
                }
            }

            Row(Modifier.padding(horizontal = 16.dp)) {
                SelectionField(
                    label = "অ্যাকাউন্ট",
                    selectedText = state.accounts.firstOrNull { it.id == filter.accountId }?.name ?: "সব অ্যাকাউন্ট",
                    options = listOf<AccountEntity?>(null) + state.accounts,
                    optionLabel = { it?.name ?: "সব অ্যাকাউন্ট" },
                    onSelect = { selected -> viewModel.updateFilter { it.copy(accountId = selected?.id) } },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                SelectionField(
                    label = "ক্যাটাগরি",
                    selectedText = state.categories.firstOrNull { it.id == filter.categoryId }?.name ?: "সব ক্যাটাগরি",
                    options = listOf<CategoryEntity?>(null) + state.categories,
                    optionLabel = { it?.name ?: "সব ক্যাটাগরি" },
                    onSelect = { selected -> viewModel.updateFilter { it.copy(categoryId = selected?.id) } },
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.transactions.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "কোনো লেনদেন পাওয়া যায়নি",
                    subtitle = "ফিল্টার বদলান অথবা নতুন লেনদেন যোগ করুন।",
                    actionLabel = "নতুন লেনদেন যোগ করুন",
                    onAction = onAddTransaction
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.transactions, key = { it.id }) { txn ->
                        TransactionRow(txn, onClick = { onOpenTransaction(txn.id) })
                        HorizontalDivider(Modifier.padding(start = 70.dp))
                    }
                }
            }
        }
    }
}
