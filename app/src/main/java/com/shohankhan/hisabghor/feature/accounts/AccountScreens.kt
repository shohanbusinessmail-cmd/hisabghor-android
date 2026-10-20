package com.shohankhan.hisabghor.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.AccountLedgerRow
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.Emerald600
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.accountTypeIcon
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.AccountScope
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.AccountWithBalance
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- Account list ----------------

@OptIn(ExperimentalCoroutinesApi::class)
class AccountListViewModel(accounts: AccountRepository) : ViewModel() {
    private val includeArchived = MutableStateFlow(false)

    val accounts: StateFlow<List<AccountWithBalance>> = includeArchived
        .flatMapLatest { accounts.observeAccountsWithBalances(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val showArchived: StateFlow<Boolean> = includeArchived

    fun setShowArchived(show: Boolean) {
        includeArchived.value = show
    }
}

@Composable
fun AccountListScreen(
    onAddAccount: () -> Unit,
    onOpenAccount: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: AccountListViewModel = viewModel(
        factory = hisabViewModelFactory { AccountListViewModel(container.accountRepository) }
    )
    val accounts by viewModel.accounts.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val total = accounts.filter { it.account.archivedAt == null }.sumOf { it.balancePoisha }

    Scaffold(
        topBar = { HisabTopBar(title = "অ্যাকাউন্টসমূহ") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন অ্যাকাউন্ট যোগ করুন")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("সর্বমোট ব্যালান্স", style = MaterialTheme.typography.bodyMedium)
                    AmountText(total, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Row(Modifier.padding(horizontal = 16.dp)) {
                FilterChip(
                    selected = showArchived,
                    onClick = { viewModel.setShowArchived(!showArchived) },
                    label = { Text("আর্কাইভ করাগুলোও দেখান") }
                )
            }
            if (accounts.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "কোনো অ্যাকাউন্ট নেই",
                    subtitle = "ক্যাশ, ব্যাংক বা বিকাশের মতো অ্যাকাউন্ট যোগ করুন।",
                    actionLabel = "অ্যাকাউন্ট যোগ করুন",
                    onAction = onAddAccount
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(accounts, key = { it.account.id }) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAccount(item.account.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                accountTypeIcon(item.account.type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(0.dp))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    item.account.name +
                                        if (item.account.archivedAt != null) " (আর্কাইভ)" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${item.account.type.bnLabel()} • ${item.account.scope.bnLabel()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AmountText(
                                item.balancePoisha,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (item.balancePoisha < 0) Danger600 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(Modifier.padding(start = 52.dp))
                    }
                }
            }
        }
    }
}

// ---------------- Account editor ----------------

data class AccountEditorUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val scope: AccountScope = AccountScope.PERSONAL,
    val note: String = "",
    val openingText: String = "",
    val archived: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class AccountEditorViewModel(
    private val editId: Long,
    private val db: HisabGhorDatabase,
    private val accounts: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountEditorUiState(isEdit = editId > 0))
    val state: StateFlow<AccountEditorUiState> = _state

    private var existing: AccountEntity? = null
    private var originalOpeningPoisha: Long = 0

    init {
        viewModelScope.launch {
            if (editId > 0) {
                val account = accounts.getAccount(editId)
                existing = account
                val opening = db.transactionDao().getOpeningTransaction(editId)
                originalOpeningPoisha = opening?.amountPoisha ?: 0
                _state.update {
                    it.copy(
                        loading = false,
                        name = account?.name ?: "",
                        type = account?.type ?: AccountType.CASH,
                        scope = account?.scope ?: AccountScope.PERSONAL,
                        note = account?.note ?: "",
                        openingText = if (originalOpeningPoisha != 0L)
                            poishaToInputText(originalOpeningPoisha) else "",
                        archived = account?.archivedAt != null
                    )
                }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setType(v: AccountType) = _state.update { it.copy(type = v) }
    fun setScope(v: AccountScope) = _state.update { it.copy(scope = v) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }
    fun setOpening(v: String) = _state.update { it.copy(openingText = v) }

    fun setArchived(archived: Boolean) {
        val account = existing ?: return
        viewModelScope.launch {
            accounts.updateAccount(
                account.copy(archivedAt = if (archived) Dates.nowMs() else null)
            )
            existing = accounts.getAccount(editId)
            _state.update { it.copy(archived = archived) }
        }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "অ্যাকাউন্টের নাম দিন") }
            return
        }
        val opening = BengaliNumber.parsePoisha(s.openingText.ifBlank { "0" })
        if (opening == null) {
            _state.update { it.copy(error = "শুরুর ব্যালান্স সঠিক নয়") }
            return
        }
        viewModelScope.launch {
            try {
                if (editId > 0) {
                    val account = existing ?: return@launch
                    accounts.updateAccount(
                        account.copy(
                            name = s.name.trim(), type = s.type, scope = s.scope,
                            note = s.note.trim().ifBlank { null }
                        )
                    )
                    if (opening != originalOpeningPoisha) {
                        accounts.setOpeningBalance(editId, opening)
                    }
                } else {
                    accounts.createAccount(
                        s.name, s.type, s.scope, s.note, opening
                    )
                }
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "সংরক্ষণ করা যায়নি — আবার চেষ্টা করুন") }
            }
        }
    }
}

@Composable
fun AccountEditorScreen(
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: AccountEditorViewModel = viewModel(
        key = "account_editor_$editId",
        factory = hisabViewModelFactory {
            AccountEditorViewModel(editId, container.database, container.accountRepository)
        }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (state.isEdit) "অ্যাকাউন্ট সম্পাদনা" else "নতুন অ্যাকাউন্ট",
                onBack = onBack
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("অ্যাকাউন্টের নাম") },
                placeholder = { Text("যেমন: বিকাশ (01912••••••)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SelectionField(
                label = "অ্যাকাউন্টের ধরন",
                selectedText = state.type.bnLabel(),
                options = AccountType.entries.toList(),
                optionLabel = { it.bnLabel() },
                onSelect = viewModel::setType
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountScope.entries.forEach { scope ->
                    FilterChip(
                        selected = state.scope == scope,
                        onClick = { viewModel.setScope(scope) },
                        label = { Text(scope.bnLabel()) }
                    )
                }
            }
            MoneyInputField(
                value = state.openingText,
                onValueChange = viewModel::setOpening,
                label = "শুরুর ব্যালান্স (টাকা)"
            )
            Text(
                "শুরুর ব্যালান্স আয় হিসেবে ধরা হয় না — এটি শুধু হিসাব শুরুর অবস্থা।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("মন্তব্য (ঐচ্ছিক)") },
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("সংরক্ষণ করুন")
            }
            if (state.isEdit) {
                OutlinedButton(
                    onClick = { viewModel.setArchived(!state.archived) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "অ্যাকাউন্ট আর্কাইভ করুন")
                }
                Text(
                    "আর্কাইভ করলে অ্যাকাউন্টটি তালিকায় লুকিয়ে থাকবে, কিন্তু পুরনো লেনদেন ও হিসাব অক্ষত থাকবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------- Account detail / statement ----------------

data class StatementLine(
    val row: AccountLedgerRow,
    val runningBalancePoisha: Long
)

data class AccountDetailUiState(
    val loading: Boolean = true,
    val account: AccountEntity? = null,
    val balancePoisha: Long = 0,
    val lines: List<StatementLine> = emptyList()
)

class AccountDetailViewModel(
    private val accountId: Long,
    private val accounts: AccountRepository,
    db: HisabGhorDatabase
) : ViewModel() {

    val uiState: StateFlow<AccountDetailUiState> = combine(
        accounts.observeAccount(accountId),
        db.postingDao().observeAccountLedger(accountId)
    ) { account, rows ->
        var running = 0L
        val lines = rows.map { row ->
            running += row.signedAmountPoisha
            StatementLine(row, running)
        }
        AccountDetailUiState(
            loading = false,
            account = account,
            balancePoisha = running,
            lines = lines.reversed()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState())

    val balanceText: StateFlow<String> = uiState
        .map { formatAmount(it.balancePoisha) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun adjustBalance(targetPoisha: Long, note: String?) {
        viewModelScope.launch {
            runCatching { accounts.adjustBalanceTo(accountId, targetPoisha, note) }
        }
    }
}

@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: AccountDetailViewModel = viewModel(
        key = "account_detail_$accountId",
        factory = hisabViewModelFactory {
            AccountDetailViewModel(accountId, container.accountRepository, container.database)
        }
    )
    val state by viewModel.uiState.collectAsState()
    var showAdjust by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HisabTopBar(
                title = state.account?.name ?: "অ্যাকাউন্ট",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onEdit(accountId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "অ্যাকাউন্ট সম্পাদনা")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${state.account?.type?.bnLabel() ?: ""} • ${state.account?.scope?.bnLabel() ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    AmountText(
                        state.balancePoisha,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (state.balancePoisha < 0) Danger600 else Emerald600
                    )
                    state.account?.note?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { showAdjust = true }) {
                        Text("ব্যালান্স মিলিয়ে নিন (সমন্বয়)")
                    }
                }
            }
            Text(
                "হিসাবের বিবরণী (স্টেটমেন্ট)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            if (state.lines.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "এখনো কোনো লেনদেন নেই",
                    subtitle = "এই অ্যাকাউন্টে লেনদেন হলেই এখানে ক্রমিক ব্যালান্সসহ দেখা যাবে।"
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.lines, key = { it.row.transactionId }) { line ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTransaction(line.row.transactionId) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(line.row.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    Dates.formatShortDate(line.row.occurredAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                AmountText(
                                    line.row.signedAmountPoisha,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (line.row.signedAmountPoisha < 0) Danger600 else Emerald600
                                )
                                Text(
                                    "ব্যালান্স: ${formatAmount(line.runningBalancePoisha)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }

    if (showAdjust) {
        var targetText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showAdjust = false },
            title = { Text("ব্যালান্স সমন্বয়") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "বর্তমান ব্যালান্স: ${formatAmount(state.balancePoisha)}। " +
                            "আসল ব্যালান্স লিখুন — পার্থক্যটি একটি সমন্বয় এন্ট্রি হিসেবে যোগ হবে (আয়/খরচ নয়)।",
                        style = MaterialTheme.typography.bodySmall
                    )
                    MoneyInputField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = "আসল ব্যালান্স (টাকা)",
                        error = error
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("কারণ (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = BengaliNumber.parsePoisha(targetText)
                    if (target == null) {
                        error = "সঠিক পরিমাণ দিন"
                    } else if (target == state.balancePoisha) {
                        error = "ব্যালান্স ইতিমধ্যে মিলে আছে"
                    } else {
                        viewModel.adjustBalance(target, noteText.ifBlank { null })
                        showAdjust = false
                    }
                }) { Text("সমন্বয় করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showAdjust = false }) { Text("বাতিল") }
            }
        )
    }
}
