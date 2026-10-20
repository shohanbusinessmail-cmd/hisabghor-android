package com.shohankhan.hisabghor.feature.savings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExtendedFloatingActionButton
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountInput
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgFormBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgSegmented
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

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
    val totalTarget = goals.sumOf { it.goal.targetPoisha }

    HgScreen(
        title = "সঞ্চয়",
        subtitle = "লক্ষ্য ও জমার হিসাব",
        insetBottom = false,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddGoal,
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন লক্ষ্য", style = MaterialTheme.typography.labelLarge) }
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
                    padding = PaddingValues(Space.lg)
                ) {
                    Eyebrow("মোট সঞ্চয়")
                    Spacer(Modifier.height(Space.xs))
                    AmountText(totalSaved, style = HgAmountType.medium, color = Hg.colors.income)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "লক্ষ্য মিলিয়ে ${formatAmount(totalTarget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                    Spacer(Modifier.height(Space.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        HgButton(
                            text = "জমা দিন",
                            onClick = { onMove(-1, -1, false) },
                            modifier = Modifier.weight(1f)
                        )
                        HgButton(
                            text = "তুলুন",
                            onClick = { onMove(-1, -1, true) },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "সঞ্চয়ে টাকা রাখা খরচ নয় — টাকা আপনারই থাকে, শুধু অন্য অ্যাকাউন্টে সরানো হয়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
            }
            item {
                HgSegmented(
                    options = listOf(false, true),
                    selected = includeArchived,
                    label = { if (it) "আর্কাইভসহ" else "চলমান লক্ষ্য" },
                    onSelect = { viewModel.setIncludeArchived(it) },
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
                )
            }
            if (goals.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Savings,
                        title = "কোনো সঞ্চয় লক্ষ্য নেই",
                        subtitle = "ঈদ, জমি, জরুরি তহবিল — লক্ষ্য ঠিক করে ধাপে ধাপে জমাতে শুরু করুন।",
                        actionLabel = "লক্ষ্য যোগ করুন",
                        onAction = onAddGoal
                    )
                }
            }
            items(goals, key = { it.goal.id }) { item ->
                val goal = item.goal
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs),
                    padding = PaddingValues(Space.lg)
                ) {
                    EgGoalHeader(goal.name, item.progressFraction)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        goal.productType.bnLabel() +
                            (goal.targetDate?.let { " • লক্ষ্য ${Dates.formatShortDate(it)}" } ?: "") +
                            if (goal.archivedAt != null) " • আর্কাইভ করা" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                    Spacer(Modifier.height(Space.sm))
                    LabeledProgress(
                        fraction = item.progressFraction,
                        leftText = "জমেছে ${formatAmount(item.savedPoisha)}",
                        rightText = "লক্ষ্য ${formatAmount(goal.targetPoisha)}"
                    )
                    Spacer(Modifier.height(Space.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        HgButton(
                            text = "জমা দিন",
                            onClick = { onMove(goal.id, goal.accountId ?: -1, false) },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        HgButton(
                            text = "তুলুন",
                            onClick = { onMove(goal.id, goal.accountId ?: -1, true) },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        HgButton(
                            text = "সম্পাদনা",
                            onClick = { onEditGoal(goal.id) },
                            style = HgButtonStyle.Ghost,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EgGoalHeader(name: String, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconChip(Icons.Filled.Savings)
        Spacer(Modifier.width(Space.md))
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = Hg.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            BengaliNumber.formatPercentBp((progress * 10000).toInt()),
            style = MaterialTheme.typography.labelLarge,
            color = Hg.colors.income
        )
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

    HgScreen(
        title = if (state.isEdit) "লক্ষ্য সম্পাদনা" else "নতুন সঞ্চয় লক্ষ্য",
        subtitle = "সঞ্চয়ের লক্ষ্য ঠিক করুন",
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding)
        ) {
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = "লক্ষ্যের নাম",
                placeholder = "যেমন: জরুরি তহবিল, ঈদের সঞ্চয়"
            )
            Spacer(Modifier.height(Space.md))
            HgAmountInput(
                value = state.targetText,
                onValueChange = { v -> viewModel.update { it.copy(targetText = v) } },
                error = state.error
            )
            Spacer(Modifier.height(Space.lg))
            Text(
                "সঞ্চয়ের ধরন",
                style = MaterialTheme.typography.titleMedium,
                color = Hg.colors.ink
            )
            Spacer(Modifier.height(Space.sm))
            HgSegmented(
                options = SavingsProductType.entries,
                selected = state.productType,
                label = { it.bnLabel() },
                onSelect = { type -> viewModel.update { it.copy(productType = type) } }
            )
            Spacer(Modifier.height(Space.md))
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
                HgNotice(
                    title = "সঞ্চয় অ্যাকাউন্ট নেই",
                    message = "অ্যাকাউন্ট পেজ থেকে 'সঞ্চয় অ্যাকাউন্ট' ধরনের অ্যাকাউন্ট তৈরি করলে এখানে যুক্ত করতে পারবেন।",
                    tone = HgTone.Neutral
                )
            }
            Spacer(Modifier.height(Space.md))
            HgCard(padding = PaddingValues(Space.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("লক্ষ্যের তারিখ আছে", style = MaterialTheme.typography.bodyLarge, color = Hg.colors.ink)
                        Text(
                            "নির্বাচিত তারিখ পর্যন্ত সময় বাকি দেখানো হবে",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
                        )
                    }
                    Spacer(Modifier.width(Space.md))
                    Switch(
                        checked = state.hasTargetDate,
                        onCheckedChange = { checked -> viewModel.update { it.copy(hasTargetDate = checked) } },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = Hg.colors.brand,
                            uncheckedThumbColor = Hg.colors.surface,
                            uncheckedTrackColor = Hg.colors.surfaceSunken,
                            uncheckedBorderColor = Hg.colors.hairlineStrong
                        )
                    )
                }
                if (state.hasTargetDate) {
                    Spacer(Modifier.height(Space.md))
                    DateField(
                        date = state.targetDate,
                        onDateChange = { v -> viewModel.update { it.copy(targetDate = v) } },
                        label = "লক্ষ্যের তারিখ"
                    )
                }
            }
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.note,
                onValueChange = { v -> viewModel.update { it.copy(note = v) } },
                label = "মন্তব্য (ঐচ্ছিক)"
            )
            state.error?.let {
                Spacer(Modifier.height(Space.md))
                HgNotice(title = it, tone = HgTone.Expense)
            }
            if (state.isEdit) {
                Spacer(Modifier.height(Space.lg))
                HgButton(
                    text = if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "আর্কাইভ করুন",
                    onClick = { viewModel.setArchived(!state.archived) },
                    style = HgButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(Space.xxl))
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

    HgScreen(
        title = if (withdraw) "সঞ্চয় থেকে উত্তোলন" else "সঞ্চয়ে জমা",
        subtitle = "অ্যাকাউন্টের মধ্যে সরানো",
        onBack = onBack,
        bottomBar = {
            if (savingsAccounts.isNotEmpty()) {
                HgFormBottomBar {
                    HgButton(
                        text = if (withdraw) "উত্তোলন করুন" else "জমা দিন",
                        onClick = viewModel::save,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@HgScreen
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding)
        ) {
            Spacer(Modifier.height(Space.md))
            HgNotice(
                title = if (withdraw) "উত্তোলন করলেও টাকা আপনারই" else "জমা দেওয়া টাকা খরচ নয়",
                message = if (withdraw) "এটি আয় হিসেবে গণনা হবে না, শুধু অ্যাকাউন্ট বদলাবে।"
                else "টাকা আপনার সঞ্চয় অ্যাকাউন্টেই জমা থাকবে।",
                tone = HgTone.Neutral
            )
            Spacer(Modifier.height(Space.md))
            if (savingsAccounts.isEmpty()) {
                HgCard(padding = PaddingValues(Space.lg)) {
                    Text(
                        "আপনার কোনো সঞ্চয় অ্যাকাউন্ট নেই। প্রথমে একটি 'সঞ্চয় অ্যাকাউন্ট' তৈরি করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.ink
                    )
                    Spacer(Modifier.height(Space.md))
                    HgButton(
                        text = "সঞ্চয় অ্যাকাউন্ট তৈরি করুন",
                        onClick = onCreateSavingsAccount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                HgAmountInput(
                    value = state.amountText,
                    onValueChange = { v -> viewModel.update { it.copy(amountText = v) } },
                    error = state.error
                )
                Spacer(Modifier.height(Space.md))
                SelectionField(
                    label = "সঞ্চয় অ্যাকাউন্ট",
                    selectedText = savingsAccounts.firstOrNull { it.id == state.savingsAccountId }?.name
                        ?: "বেছে নিন",
                    options = savingsAccounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(savingsAccountId = selected.id) } }
                )
                Spacer(Modifier.height(Space.md))
                SelectionField(
                    label = if (withdraw) "যে অ্যাকাউন্টে টাকা আসবে" else "যে অ্যাকাউন্ট থেকে জমা দেবেন",
                    selectedText = state.accounts.firstOrNull { it.id == state.sourceAccountId }?.name
                        ?: "বেছে নিন",
                    options = state.accounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(sourceAccountId = selected.id) } }
                )
                if (state.goals.isNotEmpty()) {
                    Spacer(Modifier.height(Space.md))
                    SelectionField(
                        label = "সঞ্চয় লক্ষ্য (ঐচ্ছিক)",
                        selectedText = state.goals.firstOrNull { it.id == state.goalId }?.name
                            ?: "কোনো লক্ষ্য নয়",
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
                Spacer(Modifier.height(Space.md))
                DateField(
                    date = state.date,
                    onDateChange = { v -> viewModel.update { it.copy(date = v) } },
                    label = "তারিখ"
                )
                Spacer(Modifier.height(Space.md))
                HgTextField(
                    value = state.note,
                    onValueChange = { v -> viewModel.update { it.copy(note = v) } },
                    label = "মন্তব্য (ঐচ্ছিক)"
                )
                val fromName = state.accounts.firstOrNull {
                    it.id == if (withdraw) state.savingsAccountId else state.sourceAccountId
                }?.name
                val toName = state.accounts.firstOrNull {
                    it.id == if (withdraw) state.sourceAccountId else state.savingsAccountId
                }?.name
                if (fromName != null && toName != null) {
                    Spacer(Modifier.height(Space.md))
                    HgCard(padding = PaddingValues(vertical = Space.xs)) {
                        Row(
                            Modifier.padding(horizontal = Space.lg, vertical = Space.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Eyebrow("টাকা যাবে")
                            Spacer(Modifier.width(Space.sm))
                            Text(
                                "$fromName → $toName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Hg.colors.ink,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
                state.error?.let {
                    Spacer(Modifier.height(Space.md))
                    HgNotice(title = it, tone = HgTone.Expense)
                }
            }
            Spacer(Modifier.height(Space.xxl))
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

