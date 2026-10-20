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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.ui.draw.clip
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountInput
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgBadge
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgChip
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgFormBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgListRow
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgSegmented
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.SignedAmountText
import com.shohankhan.hisabghor.core.designsystem.Space
import com.shohankhan.hisabghor.core.designsystem.ConfirmDialog

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
    val active = accounts.filter { it.account.archivedAt == null }
    val total = active.sumOf { it.balancePoisha }

    HgScreen(
        title = "হিসাব",
        subtitle = "অ্যাকাউন্ট ও ব্যালেন্স",
        insetBottom = false,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAccount,
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন অ্যাকাউন্ট", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.md),
                    padding = PaddingValues(Space.xl)
                ) {
                    Eyebrow("সব অ্যাকাউন্ট মিলিয়ে")
                    Spacer(Modifier.height(Space.xs))
                    AmountText(total, style = HgAmountType.large, color = Hg.colors.ink)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${BengaliNumber.formatCount(active.size.toLong())}টি সক্রিয় অ্যাকাউন্ট",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPadding, vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "অ্যাকাউন্ট",
                        style = MaterialTheme.typography.titleMedium,
                        color = Hg.colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    HgChip(
                        text = "আর্কাইভ দেখান",
                        selected = showArchived,
                        onClick = { viewModel.setShowArchived(!showArchived) }
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    com.shohankhan.hisabghor.core.designsystem.EmptyState(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "কোনো অ্যাকাউন্ট নেই",
                        subtitle = "ক্যাশ, ব্যাংক বা বিকাশের মতো অ্যাকাউন্ট যোগ করে শুরু করুন।",
                        actionLabel = "অ্যাকাউন্ট যোগ করুন",
                        onAction = onAddAccount
                    )
                }
            } else {
                item {
                    HgCard(
                        Modifier.padding(horizontal = ScreenPadding),
                        padding = PaddingValues(vertical = Space.xs)
                    ) {
                        accounts.forEachIndexed { index, item ->
                            AccountRow(item, onClick = { onOpenAccount(item.account.id) })
                            if (index != accounts.lastIndex) HgDivider(startInset = 72)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Space.xxl)) }
        }
    }
}

@Composable
private fun AccountRow(item: AccountWithBalance, onClick: () -> Unit) {
    HgListRow(
        title = item.account.name,
        subtitle = "${item.account.type.bnLabel()} • ${item.account.scope.bnLabel()}",
        leading = {
            IconChip(accountTypeIcon(item.account.type))
        },
        titleTrailing = if (item.account.archivedAt != null) {
            { HgBadge("আর্কাইভ", HgTone.Neutral) }
        } else null,
        trailing = {
            AmountText(
                item.balancePoisha,
                style = HgAmountType.medium,
                color = if (item.balancePoisha < 0) Hg.colors.expense else Hg.colors.ink
            )
        },
        onClick = onClick
    )
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
    var showArchiveConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    HgScreen(
        title = if (state.isEdit) "অ্যাকাউন্ট সম্পাদনা" else "নতুন অ্যাকাউন্ট",
        onBack = onBack,
        bottomBar = {
            HgFormBottomBar {
                HgButton(
                    text = "সংরক্ষণ করুন",
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@HgScreen
        }
        androidx.compose.foundation.layout.Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding)
        ) {
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = "অ্যাকাউন্টের নাম",
                placeholder = "যেমন: বিকাশ (০১৯…)",
                error = if (state.name.isBlank() && state.error != null) state.error else null
            )
            Spacer(Modifier.height(Space.md))
            SelectionField(
                label = "অ্যাকাউন্টের ধরন",
                selectedText = state.type.bnLabel(),
                options = AccountType.entries.toList(),
                optionLabel = { it.bnLabel() },
                onSelect = viewModel::setType
            )
            Spacer(Modifier.height(Space.md))
            com.shohankhan.hisabghor.core.designsystem.FormSectionLabel("ব্যবহার")
            HgSegmented(
                options = AccountScope.entries.toList(),
                selected = state.scope,
                label = { it.bnLabel() },
                onSelect = viewModel::setScope
            )
            Spacer(Modifier.height(Space.lg))
            HgAmountInput(
                value = state.openingText,
                onValueChange = viewModel::setOpening
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                "শুরুর ব্যালান্স (ঐচ্ছিক)",
                style = MaterialTheme.typography.bodySmall,
                color = Hg.colors.inkSecondary
            )
            Spacer(Modifier.height(Space.md))
            HgNotice(
                title = "শুরুর ব্যালান্স আয় নয়",
                message = "এটি শুধু হিসাব শুরুর অবস্থা বোঝায় — আয় হিসেবে ধরা হয় না।",
                tone = HgTone.Neutral
            )
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = "মন্তব্য (ঐচ্ছিক)"
            )
            state.error?.let {
                Spacer(Modifier.height(Space.md))
                HgNotice(title = it, tone = HgTone.Expense)
            }
            if (state.isEdit) {
                Spacer(Modifier.height(Space.lg))
                HgDivider()
                Spacer(Modifier.height(Space.md))
                HgButton(
                    text = if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "অ্যাকাউন্ট আর্কাইভ করুন",
                    onClick = {
                        if (state.archived) viewModel.setArchived(false) else showArchiveConfirm = true
                    },
                    style = HgButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    "আর্কাইভ করলে অ্যাকাউন্টটি তালিকায় লুকিয়ে থাকবে, পুরনো লেনদেন ও হিসাব অক্ষত থাকবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary
                )
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }

    if (showArchiveConfirm) {
        ConfirmDialog(
            title = "অ্যাকাউন্ট আর্কাইভ করবেন?",
            message = "তালিকা থেকে লুকিয়ে যাবে, তবে সব পুরনো লেনদেন ও ব্যালান্স অক্ষত থাকবে।",
            confirmText = "আর্কাইভ করুন",
            onConfirm = {
                viewModel.setArchived(true)
                showArchiveConfirm = false
            },
            onDismiss = { showArchiveConfirm = false }
        )
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

    HgScreen(
        title = state.account?.name ?: "অ্যাকাউন্ট",
        subtitle = state.account?.let { "${it.type.bnLabel()} • ${it.scope.bnLabel()}" },
        onBack = onBack,
        actions = {
            IconButton(onClick = { onEdit(accountId) }) {
                Icon(Icons.Filled.Edit, contentDescription = "অ্যাকাউন্ট সম্পাদনা", tint = Hg.colors.ink)
            }
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@HgScreen
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = Space.xxl)
        ) {
            item {
                Column(Modifier.padding(horizontal = ScreenPadding, vertical = Space.md)) {
                    Eyebrow("বর্তমান ব্যালান্স")
                    Spacer(Modifier.height(Space.xs))
                    AmountText(
                        state.balancePoisha,
                        style = HgAmountType.hero,
                        color = if (state.balancePoisha < 0) Hg.colors.expense else Hg.colors.ink
                    )
                    state.account?.note?.let {
                        Spacer(Modifier.height(Space.xs))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary)
                    }
                    Spacer(Modifier.height(Space.md))
                    HgButton(
                        text = "ব্যালান্স মিলিয়ে নিন",
                        onClick = { showAdjust = true },
                        style = HgButtonStyle.Secondary,
                        icon = Icons.Filled.Tune
                    )
                }
            }
            item {
                Text(
                    "স্টেটমেন্ট",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
                )
            }
            if (state.lines.isEmpty()) {
                item {
                    com.shohankhan.hisabghor.core.designsystem.EmptyState(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "এখনো কোনো লেনদেন নেই",
                        subtitle = "এই অ্যাকাউন্টে লেনদেন হলেই এখানে ক্রমিক ব্যালান্সসহ দেখা যাবে।"
                    )
                }
            } else {
                items(state.lines, key = { it.row.transactionId }) { line ->
                    HgListRow(
                        title = line.row.title,
                        subtitle = Dates.formatShortDate(line.row.occurredAt),
                        trailing = {
                            Column(horizontalAlignment = Alignment.End) {
                                SignedAmountText(
                                    poisha = line.row.signedAmountPoisha,
                                    style = HgAmountType.small
                                )
                                Text(
                                    formatAmount(line.runningBalancePoisha),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Hg.colors.inkTertiary
                                )
                            }
                        },
                        onClick = { onOpenTransaction(line.row.transactionId) },
                        showDivider = true,
                        modifier = Modifier.padding(horizontal = ScreenPadding)
                    )
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
            containerColor = Hg.colors.surface,
            shape = Radius.panel,
            title = { Text("ব্যালান্স সমন্বয়") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    Text(
                        "বর্তমান ব্যালান্স ${formatAmount(state.balancePoisha)}। আসল ব্যালান্স লিখুন — " +
                            "পার্থক্যটি সমন্বয় এন্ট্রি হিসেবে যোগ হবে, আয়/খরচ হিসেবে নয়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
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
                        shape = Radius.field,
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
                }) { Text("সমন্বয় করুন", color = Hg.colors.brand) }
            },
            dismissButton = {
                TextButton(onClick = { showAdjust = false }) {
                    Text("বাতিল", color = Hg.colors.inkSecondary)
                }
            }
        )
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

