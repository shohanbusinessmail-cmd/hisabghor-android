package com.shohankhan.hisabghor.feature.savings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.SavingsGoalEntity
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.Emerald600
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.designsystem.LabeledProgress
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.SavingsProductType
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.GoalWithProgress
import com.shohankhan.hisabghor.data.SavingsRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------------- savings list ----------------

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsListViewModel(savings: SavingsRepository) : ViewModel() {
    private val _includeArchived = MutableStateFlow(false)
    val includeArchived: StateFlow<Boolean> = _includeArchived
    val goals: StateFlow<List<GoalWithProgress>> =
        _includeArchived.flatMapLatest { include ->
            savings.observeGoalsWithProgress(include)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setIncludeArchived(include: Boolean) { _includeArchived.value = include }
}

@Composable
fun SavingsScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onMove: (goalId: Long, accountId: Long, withdraw: Boolean) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: SavingsListViewModel = viewModel(
        factory = hisabViewModelFactory { SavingsListViewModel(container.savingsRepository) }
    )
    val goals by viewModel.goals.collectAsState()
    val includeArchived by viewModel.includeArchived.collectAsState()
    val totalSaved = goals.sumOf { it.savedPoisha }

    Scaffold(
        topBar = { HisabTopBar(title = "সঞ্চয়") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGoal) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন লক্ষ্য যোগ করুন")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("মোট সঞ্চয় (সব লক্ষ্যে)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatAmount(totalSaved),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Emerald600
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onMove(-1, -1, false) },
                            modifier = Modifier.weight(1f)
                        ) { Text("সঞ্চয়ে জমা দিন") }
                        OutlinedButton(
                            onClick = { onMove(-1, -1, true) },
                            modifier = Modifier.weight(1f)
                        ) { Text("সঞ্চয় থেকে তুলুন") }
                    }
                    Text(
                        "সঞ্চয়ে টাকা রাখা খরচ নয় — টাকা আপনারই থাকে, শুধু অন্য অ্যাকাউন্টে সরানো হয়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !includeArchived,
                    onClick = { viewModel.setIncludeArchived(false) },
                    label = { Text("চলমান লক্ষ্য") }
                )
                FilterChip(
                    selected = includeArchived,
                    onClick = { viewModel.setIncludeArchived(true) },
                    label = { Text("আর্কাইভসহ সব") }
                )
            }
            if (goals.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Savings,
                    title = "কোনো সঞ্চয় লক্ষ্য নেই",
                    subtitle = "ঈদ, জমি, জরুরি তহবিল — লক্ষ্য ঠিক করে ধাপে ধাপে জমাতে শুরু করুন।",
                    actionLabel = "লক্ষ্য যোগ করুন",
                    onAction = onAddGoal
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(goals, key = { it.goal.id }) { item ->
                        val goal = item.goal
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onEditGoal(goal.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    goal.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    BengaliNumber.formatPercentBp((item.progressFraction * 10000).toInt()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Emerald600
                                )
                            }
                            Text(
                                goal.productType.bnLabel() +
                                    (goal.targetDate?.let { " • লক্ষ্য ${Dates.formatShortDate(it)}" } ?: "") +
                                    if (goal.archivedAt != null) " • আর্কাইভ করা" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            LabeledProgress(
                                fraction = item.progressFraction,
                                leftText = "জমেছে ${formatAmount(item.savedPoisha)}",
                                rightText = "লক্ষ্য ${formatAmount(goal.targetPoisha)}"
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onMove(goal.id, goal.accountId ?: -1, false) }
                                ) { Text("জমা দিন") }
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                OutlinedButton(
                                    onClick = { onMove(goal.id, goal.accountId ?: -1, true) }
                                ) { Text("তুলুন") }
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

// ---------------- goal editor ----------------

data class GoalEditorUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val targetText: String = "",
    val productType: SavingsProductType = SavingsProductType.BANK,
    val accountId: Long? = null,
    val savingsAccounts: List<AccountEntity> = emptyList(),
    val hasTargetDate: Boolean = false,
    val targetDate: LocalDate = Dates.today().plusMonths(6),
    val note: String = "",
    val archived: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class GoalEditorViewModel(
    private val editId: Long,
    private val savings: SavingsRepository,
    accounts: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GoalEditorUiState(isEdit = editId > 0))
    val state: StateFlow<GoalEditorUiState> = _state
    private var existing: SavingsGoalEntity? = null

    init {
        viewModelScope.launch {
            val savingsAccounts = accounts.observeAccountsWithBalances().first()
                .map { it.account }
                .filter { it.type == AccountType.SAVINGS }
            if (editId > 0) {
                val goal = savings.getGoal(editId)
                existing = goal
                _state.update {
                    it.copy(
                        loading = false,
                        savingsAccounts = savingsAccounts,
                        name = goal?.name ?: "",
                        targetText = goal?.let { g -> poishaToInputText(g.targetPoisha) } ?: "",
                        productType = goal?.productType ?: SavingsProductType.BANK,
                        accountId = goal?.accountId,
                        hasTargetDate = goal?.targetDate != null,
                        targetDate = goal?.targetDate?.let { ms -> Dates.toLocalDate(ms) }
                            ?: Dates.today().plusMonths(6),
                        note = goal?.note ?: "",
                        archived = goal?.archivedAt != null
                    )
                }
            } else {
                _state.update { it.copy(loading = false, savingsAccounts = savingsAccounts) }
            }
        }
    }

    fun update(change: (GoalEditorUiState) -> GoalEditorUiState) = _state.update(change)

    fun save() {
        val s = _state.value
        val target = BengaliNumber.parsePoisha(s.targetText)
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "লক্ষ্যের নাম দিন") }
            return
        }
        if (target == null || target <= 0) {
            _state.update { it.copy(error = "লক্ষ্যের পরিমাণ ঠিক করে দিন") }
            return
        }
        viewModelScope.launch {
            try {
                val targetDateMs = if (s.hasTargetDate) Dates.startOfDayMs(s.targetDate) else null
                if (s.isEdit) {
                    existing?.let {
                        savings.updateGoal(
                            it.copy(
                                name = s.name.trim(),
                                targetPoisha = target,
                                targetDate = targetDateMs,
                                accountId = s.accountId,
                                productType = s.productType,
                                note = s.note.ifBlank { null }
                            )
                        )
                    }
                } else {
                    savings.createGoal(
                        name = s.name,
                        targetPoisha = target,
                        targetDateMs = targetDateMs,
                        accountId = s.accountId,
                        productType = s.productType,
                        note = s.note.ifBlank { null }
                    )
                }
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "সংরক্ষণ করা যায়নি") }
            }
        }
    }

    fun setArchived(archived: Boolean) {
        val goal = existing ?: return
        viewModelScope.launch {
            savings.setArchived(goal, archived)
            existing = savings.getGoal(editId)
            _state.update { it.copy(archived = archived) }
        }
    }
}

@Composable
fun GoalEditorScreen(
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: GoalEditorViewModel = viewModel(
        key = "goal_editor_$editId",
        factory = hisabViewModelFactory {
            GoalEditorViewModel(editId, container.savingsRepository, container.accountRepository)
        }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (state.isEdit) "লক্ষ্য সম্পাদনা" else "নতুন সঞ্চয় লক্ষ্য",
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
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = { Text("লক্ষ্যের নাম") },
                placeholder = { Text("যেমন: জরুরি তহবিল, ঈদের সঞ্চয়") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            MoneyInputField(
                value = state.targetText,
                onValueChange = { v -> viewModel.update { it.copy(targetText = v) } },
                label = "মোট লক্ষ্য পরিমাণ (টাকা)",
                error = state.error
            )
            Text("সঞ্চয়ের ধরন", style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SavingsProductType.entries.forEach { type ->
                    FilterChip(
                        selected = state.productType == type,
                        onClick = { viewModel.update { it.copy(productType = type) } },
                        label = { Text(type.bnLabel()) }
                    )
                }
            }
            if (state.savingsAccounts.isNotEmpty()) {
                SelectionField(
                    label = "যুক্ত সঞ্চয় অ্যাকাউন্ট (ঐচ্ছিক)",
                    selectedText = state.savingsAccounts.firstOrNull { it.id == state.accountId }?.name
                        ?: "কোনোটি নয়",
                    options = state.savingsAccounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(accountId = selected.id) } }
                )
            } else {
                Text(
                    "কোনো সঞ্চয় অ্যাকাউন্ট নেই — অ্যাকাউন্ট পেজ থেকে 'সঞ্চয় অ্যাকাউন্ট' ধরনের অ্যাকাউন্ট তৈরি করলে এখানে যুক্ত করতে পারবেন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("লক্ষ্যের তারিখ আছে", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.hasTargetDate,
                    onCheckedChange = { checked -> viewModel.update { it.copy(hasTargetDate = checked) } }
                )
            }
            if (state.hasTargetDate) {
                DateField(
                    date = state.targetDate,
                    onDateChange = { v -> viewModel.update { it.copy(targetDate = v) } },
                    label = "লক্ষ্যের তারিখ"
                )
            }
            OutlinedTextField(
                value = state.note,
                onValueChange = { v -> viewModel.update { it.copy(note = v) } },
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
                    Text(if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "আর্কাইভ করুন")
                }
            }
        }
    }
}

// ---------------- savings move ----------------

data class SavingsMoveUiState(
    val loading: Boolean = true,
    val accounts: List<AccountEntity> = emptyList(),
    val goals: List<SavingsGoalEntity> = emptyList(),
    val goalId: Long? = null,
    val sourceAccountId: Long? = null,
    val savingsAccountId: Long? = null,
    val amountText: String = "",
    val date: LocalDate = Dates.today(),
    val note: String = "",
    val error: String? = null,
    val saved: Boolean = false
)

class SavingsMoveViewModel(
    private val goalIdArg: Long,
    private val accountIdArg: Long,
    private val withdraw: Boolean,
    private val editId: Long,
    db: HisabGhorDatabase,
    accounts: AccountRepository,
    private val savings: SavingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SavingsMoveUiState())
    val state: StateFlow<SavingsMoveUiState> = _state

    init {
        viewModelScope.launch {
            val accountList = accounts.observeAccountsWithBalances().first().map { it.account }
            val goalList = savings.observeGoalsWithProgress(false).first().map { it.goal }
            val savingsList = accountList.filter { it.type == AccountType.SAVINGS }
            var initial = SavingsMoveUiState(
                loading = false,
                accounts = accountList,
                goals = goalList,
                goalId = if (goalIdArg > 0) goalIdArg else null,
                savingsAccountId = when {
                    accountIdArg > 0 -> accountIdArg
                    goalIdArg > 0 -> goalList.firstOrNull { it.id == goalIdArg }?.accountId
                    else -> null
                } ?: savingsList.firstOrNull()?.id,
                sourceAccountId = accountList.firstOrNull { it.type != AccountType.SAVINGS }?.id
                    ?: accountList.firstOrNull()?.id
            )
            if (editId > 0) {
                val txn = db.transactionDao().getById(editId)
                if (txn != null) {
                    initial = initial.copy(
                        goalId = txn.savingsGoalId,
                        savingsAccountId = if (withdraw) txn.accountId else txn.counterAccountId,
                        sourceAccountId = if (withdraw) txn.counterAccountId else txn.accountId,
                        amountText = poishaToInputText(txn.amountPoisha),
                        date = Dates.toLocalDate(txn.occurredAt),
                        note = txn.note ?: ""
                    )
                }
            }
            _state.value = initial
        }
    }

    fun update(change: (SavingsMoveUiState) -> SavingsMoveUiState) = _state.update(change)

    fun save() {
        val s = _state.value
        val amount = BengaliNumber.parsePoisha(s.amountText)
        val savingsAccountId = s.savingsAccountId
        val sourceId = s.sourceAccountId
        when {
            amount == null || amount <= 0 ->
                _state.update { it.copy(error = "সঠিক পরিমাণ দিন") }
            savingsAccountId == null ->
                _state.update { it.copy(error = "সঞ্চয় অ্যাকাউন্ট বেছে নিন") }
            sourceId == null ->
                _state.update { it.copy(error = "অ্যাকাউন্ট বেছে নিন") }
            sourceId == savingsAccountId ->
                _state.update { it.copy(error = "দুটি আলাদা অ্যাকাউন্ট বেছে নিন") }
            else -> viewModelScope.launch {
                try {
                    val occurredAt = Dates.startOfDayMs(s.date) + 12 * 3600_000L
                    val replaces = if (editId > 0) editId else null
                    if (withdraw) {
                        savings.withdraw(
                            savingsAccountId = savingsAccountId,
                            toAccountId = sourceId,
                            goalId = s.goalId,
                            amountPoisha = amount,
                            occurredAt = occurredAt,
                            note = s.note.ifBlank { null },
                            replacesTransactionId = replaces
                        )
                    } else {
                        savings.deposit(
                            fromAccountId = sourceId,
                            savingsAccountId = savingsAccountId,
                            goalId = s.goalId,
                            amountPoisha = amount,
                            occurredAt = occurredAt,
                            note = s.note.ifBlank { null },
                            replacesTransactionId = replaces
                        )
                    }
                    _state.update { it.copy(saved = true) }
                } catch (e: Exception) {
                    _state.update { it.copy(error = "সংরক্ষণ করা যায়নি") }
                }
            }
        }
    }
}

@Composable
fun SavingsMoveScreen(
    goalId: Long,
    accountId: Long,
    withdraw: Boolean,
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onCreateSavingsAccount: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: SavingsMoveViewModel = viewModel(
        key = "savings_move_${goalId}_${accountId}_${withdraw}_$editId",
        factory = hisabViewModelFactory {
            SavingsMoveViewModel(
                goalId, accountId, withdraw, editId,
                container.database, container.accountRepository, container.savingsRepository
            )
        }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    val savingsAccounts = state.accounts.filter { it.type == AccountType.SAVINGS }

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (withdraw) "সঞ্চয় থেকে উত্তোলন" else "সঞ্চয়ে জমা",
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
            Text(
                if (withdraw) "উত্তোলন করলেও টাকা আপনারই — এটি আয় হিসাবে গণনা হবে না।"
                else "জমা দেওয়া টাকা খরচ নয় — টাকা আপনার সঞ্চয় অ্যাকাউন্টেই জমা থাকবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (savingsAccounts.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "আপনার কোনো সঞ্চয় অ্যাকাউন্ট নেই। প্রথমে একটি 'সঞ্চয় অ্যাকাউন্ট' তৈরি করুন।",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onCreateSavingsAccount, modifier = Modifier.fillMaxWidth()) {
                            Text("সঞ্চয় অ্যাকাউন্ট তৈরি করুন")
                        }
                    }
                }
            } else {
                MoneyInputField(
                    value = state.amountText,
                    onValueChange = { v -> viewModel.update { it.copy(amountText = v) } },
                    label = "পরিমাণ (টাকা)",
                    error = state.error
                )
                SelectionField(
                    label = "সঞ্চয় অ্যাকাউন্ট",
                    selectedText = savingsAccounts.firstOrNull { it.id == state.savingsAccountId }?.name
                        ?: "বেছে নিন",
                    options = savingsAccounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(savingsAccountId = selected.id) } }
                )
                SelectionField(
                    label = if (withdraw) "যে অ্যাকাউন্টে টাকা আসবে" else "যে অ্যাকাউন্ট থেকে জমা দেবেন",
                    selectedText = state.accounts.firstOrNull { it.id == state.sourceAccountId }?.name
                        ?: "বেছে নিন",
                    options = state.accounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(sourceAccountId = selected.id) } }
                )
                if (state.goals.isNotEmpty()) {
                    SelectionField(
                        label = "সঞ্চয় লক্ষ্য (ঐচ্ছিক)",
                        selectedText = state.goals.firstOrNull { it.id == state.goalId }?.name
                            ?: "কোনো লক্ষ নয়",
                        options = state.goals,
                        optionLabel = { it.name },
                        onSelect = { selected ->
                            viewModel.update {
                                it.copy(
                                    goalId = selected.id,
                                    savingsAccountId = selected.accountId ?: it.savingsAccountId
                                )
                            }
                        }
                    )
                }
                DateField(
                    date = state.date,
                    onDateChange = { v -> viewModel.update { it.copy(date = v) } },
                    label = "তারিখ"
                )
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { v -> viewModel.update { it.copy(note = v) } },
                    label = { Text("মন্তব্য (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
                val fromName = state.accounts.firstOrNull {
                    it.id == if (withdraw) state.savingsAccountId else state.sourceAccountId
                }?.name
                val toName = state.accounts.firstOrNull {
                    it.id == if (withdraw) state.sourceAccountId else state.savingsAccountId
                }?.name
                if (fromName != null && toName != null) {
                    KvRow("টাকা যাবে", "$fromName → $toName")
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text(if (withdraw) "উত্তোলন করুন" else "জমা দিন")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
