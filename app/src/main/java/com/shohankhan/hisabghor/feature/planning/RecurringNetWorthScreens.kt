package com.shohankhan.hisabghor.feature.planning

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.NetWorthItemEntity
import com.shohankhan.hisabghor.core.database.RecurringRuleEntity
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.ConfirmDialog
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.Emerald600
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.NetWorthItemKind
import com.shohankhan.hisabghor.core.model.RepeatRule
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.PlanningRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExtendedFloatingActionButton
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgListRow
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgSegmented
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

// ---------------- recurring rules ----------------

class RecurringViewModel(
    private val planning: PlanningRepository,
    categories: CategoryRepository,
    accounts: AccountRepository
) : ViewModel() {

    val rules: StateFlow<List<RecurringRuleEntity>> = planning.observeRecurringRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> =
        categories.observeByKind(CategoryKind.INCOME)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> =
        categories.observeByKind(CategoryKind.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accountList: StateFlow<List<AccountEntity>> =
        accounts.observeAccountsWithBalances().map { list -> list.map { it.account } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            planning.addRecurringRule(rule)
            planning.materializeDueRecurringRules()
        }
    }

    fun update(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            planning.updateRecurringRule(rule)
            planning.materializeDueRecurringRules()
        }
    }

    fun delete(rule: RecurringRuleEntity) {
        viewModelScope.launch { planning.deleteRecurringRule(rule) }
    }
}

@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: RecurringViewModel = viewModel(
        factory = hisabViewModelFactory {
            RecurringViewModel(
                container.planningRepository,
                container.categoryRepository,
                container.accountRepository
            )
        }
    )
    val rules by viewModel.rules.collectAsState()
    val incomeCats by viewModel.incomeCategories.collectAsState()
    val expenseCats by viewModel.expenseCategories.collectAsState()
    val accounts by viewModel.accountList.collectAsState()
    var editing by remember { mutableStateOf<RecurringRuleEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RecurringRuleEntity?>(null) }

    HgScreen(
        title = "নিয়মিত আয়/খরচ",
        subtitle = "স্বয়ংক্রিয় এন্ট্রির নিয়ম",
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন নিয়ম", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        if (rules.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Autorenew,
                title = "কোনো নিয়ম নেই",
                subtitle = "বেতন, ভাড়া, বিল — নিয়মিত আয়/খরচ নিজে থেকেই এন্ট্রি হয়ে যাবে এমন নিয়ম তৈরি করুন।",
                actionLabel = "নিয়ম যোগ করুন",
                onAction = { editing = null; showEditor = true },
                modifier = Modifier.padding(padding)
            )
            return@HgScreen
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(rules, key = { it.id }) { rule ->
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs),
                    padding = PaddingValues(Space.lg)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconChip(
                            Icons.Filled.Autorenew,
                            tint = if (rule.isIncome) Hg.colors.income else Hg.colors.expense,
                            background = if (rule.isIncome) Hg.colors.incomeTint else Hg.colors.expenseTint
                        )
                        Spacer(Modifier.width(Space.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                rule.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Hg.colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                (if (rule.isIncome) "আয়" else "খরচ") + " • " + rule.frequency.bnLabel() +
                                    " • পরের এন্ট্রি ${Dates.formatShortDate(rule.nextRunDate)}" +
                                    (rule.endDate?.let { " • শেষ ${Dates.formatShortDate(it)}" } ?: "") +
                                    if (!rule.enabled) " • বন্ধ" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Hg.colors.inkSecondary
                            )
                        }
                        AmountText(
                            rule.amountPoisha,
                            style = HgAmountType.small,
                            color = if (rule.isIncome) Hg.colors.income else Hg.colors.expense
                        )
                    }
                    rule.lastRunAt?.let {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            "সর্বশেষ এন্ট্রি: ${Dates.formatShortDate(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkTertiary
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { checked ->
                                viewModel.update(rule.copy(enabled = checked))
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                checkedTrackColor = Hg.colors.brand,
                                uncheckedThumbColor = Hg.colors.surface,
                                uncheckedTrackColor = Hg.colors.surfaceSunken,
                                uncheckedBorderColor = Hg.colors.hairlineStrong
                            )
                        )
                        Spacer(Modifier.width(Space.sm))
                        Text(
                            if (rule.enabled) "চালু" else "বন্ধ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        HgButton(
                            text = "বদলান",
                            onClick = { editing = rule; showEditor = true },
                            style = HgButtonStyle.Ghost
                        )
                        HgButton(
                            text = "মুছুন",
                            onClick = { deleting = rule },
                            style = HgButtonStyle.Ghost
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        var isIncome by remember { mutableStateOf(editing?.isIncome ?: false) }
        var title by remember { mutableStateOf(editing?.title ?: "") }
        var amountText by remember {
            mutableStateOf(editing?.let { poishaToInputText(it.amountPoisha) } ?: "")
        }
        var categoryId by remember { mutableStateOf(editing?.categoryId) }
        var accountId by remember { mutableStateOf(editing?.accountId) }
        var frequency by remember { mutableStateOf(editing?.frequency ?: RepeatRule.MONTHLY) }
        var nextRun by remember {
            mutableStateOf(editing?.let { Dates.toLocalDate(it.nextRunDate) } ?: Dates.today())
        }
        var hasEnd by remember { mutableStateOf(editing?.endDate != null) }
        var endDate by remember {
            mutableStateOf(editing?.endDate?.let { Dates.toLocalDate(it) } ?: Dates.today().plusYears(1))
        }
        var autoCreate by remember { mutableStateOf(editing?.autoCreate ?: true) }
        var dialogError by remember { mutableStateOf<String?>(null) }
        val categoryOptions = if (isIncome) incomeCats else expenseCats
        AlertDialog(
            onDismissRequest = { showEditor = false },
            containerColor = Hg.colors.surface,
            shape = Radius.panel,
            title = { Text(if (editing == null) "নতুন নিয়ম" else "নিয়ম বদলান") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    HgSegmented(
                        options = listOf(false, true),
                        selected = isIncome,
                        label = { if (it) "আয়" else "খরচ" },
                        onSelect = {
                            isIncome = it
                            categoryId = null
                        }
                    )
                    HgTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "শিরোনাম"
                    )
                    MoneyInputField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "পরিমাণ (টাকা)",
                        error = dialogError
                    )
                    SelectionField(
                        label = "ক্যাটাগরি",
                        selectedText = categoryOptions.firstOrNull { it.id == categoryId }?.name
                            ?: "বেছে নিন",
                        options = categoryOptions,
                        optionLabel = { it.name },
                        onSelect = { categoryId = it.id }
                    )
                    SelectionField(
                        label = "অ্যাকাউন্ট",
                        selectedText = accounts.firstOrNull { it.id == accountId }?.name ?: "বেছে নিন",
                        options = accounts,
                        optionLabel = { it.name },
                        onSelect = { accountId = it.id }
                    )
                    HgSegmented(
                        options = listOf(
                            RepeatRule.DAILY, RepeatRule.WEEKLY,
                            RepeatRule.MONTHLY, RepeatRule.YEARLY
                        ),
                        selected = frequency,
                        label = { it.bnLabel() },
                        onSelect = { frequency = it }
                    )
                    DateField(
                        date = nextRun,
                        onDateChange = { nextRun = it },
                        label = "প্রথম/পরের এন্ট্রির তারিখ"
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "শেষ তারিখ আছে",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Hg.colors.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = hasEnd,
                            onCheckedChange = { hasEnd = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                checkedTrackColor = Hg.colors.brand,
                                uncheckedThumbColor = Hg.colors.surface,
                                uncheckedTrackColor = Hg.colors.surfaceSunken,
                                uncheckedBorderColor = Hg.colors.hairlineStrong
                            )
                        )
                    }
                    if (hasEnd) {
                        DateField(date = endDate, onDateChange = { endDate = it }, label = "শেষ তারিখ")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "নিজে থেকে এন্ট্রি তৈরি করুন",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Hg.colors.ink
                            )
                            Text(
                                "বন্ধ থাকলে শুধু রিমাইন্ডার হিসাবে কাজ করবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = Hg.colors.inkSecondary
                            )
                        }
                        Spacer(Modifier.width(Space.sm))
                        Switch(
                            checked = autoCreate,
                            onCheckedChange = { autoCreate = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                checkedTrackColor = Hg.colors.brand,
                                uncheckedThumbColor = Hg.colors.surface,
                                uncheckedTrackColor = Hg.colors.surfaceSunken,
                                uncheckedBorderColor = Hg.colors.hairlineStrong
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = BengaliNumber.parsePoisha(amountText)
                    val category = categoryId
                    val account = accountId
                    when {
                        title.isBlank() -> dialogError = "শিরোনাম দিন"
                        amount == null || amount <= 0 -> dialogError = "সঠিক পরিমাণ দিন"
                        category == null -> dialogError = "ক্যাটাগরি বেছে নিন"
                        account == null -> dialogError = "অ্যাকাউন্ট বেছে নিন"
                        else -> {
                            val now = Dates.nowMs()
                            val endMs = if (hasEnd) Dates.startOfDayMs(endDate) else null
                            val existing = editing
                            if (existing == null) {
                                viewModel.add(
                                    RecurringRuleEntity(
                                        isIncome = isIncome,
                                        title = title.trim(),
                                        amountPoisha = amount,
                                        categoryId = category,
                                        accountId = account,
                                        frequency = frequency,
                                        nextRunDate = Dates.startOfDayMs(nextRun),
                                        endDate = endMs,
                                        autoCreate = autoCreate,
                                        createdAt = now, updatedAt = now
                                    )
                                )
                            } else {
                                viewModel.update(
                                    existing.copy(
                                        isIncome = isIncome,
                                        title = title.trim(),
                                        amountPoisha = amount,
                                        categoryId = category,
                                        accountId = account,
                                        frequency = frequency,
                                        nextRunDate = Dates.startOfDayMs(nextRun),
                                        endDate = endMs,
                                        autoCreate = autoCreate
                                    )
                                )
                            }
                            showEditor = false
                        }
                    }
                }) { Text("সংরক্ষণ করুন", color = Hg.colors.brand) }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text("বাতিল", color = Hg.colors.inkSecondary)
                }
            }
        )
    }

    deleting?.let { rule ->
        ConfirmDialog(
            title = "নিয়ম মুছে ফেলবেন?",
            message = "\"${rule.title}\" নিয়মটি মুছে যাবে; আগের এন্ট্রিগুলো থেকে যাবে।",
            confirmText = "মুছে ফেলুন",
            onConfirm = { viewModel.delete(rule); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
fun NetWorthItemsScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: NetWorthItemsViewModel = viewModel(
        factory = hisabViewModelFactory { NetWorthItemsViewModel(container.planningRepository) }
    )
    val items by viewModel.items.collectAsState()
    var editing by remember { mutableStateOf<NetWorthItemEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NetWorthItemEntity?>(null) }
    val assets = items.filter { it.kind == NetWorthItemKind.ASSET }
    val liabilities = items.filter { it.kind == NetWorthItemKind.LIABILITY }
    val assetTotal = assets.sumOf { it.valuePoisha }
    val liabilityTotal = liabilities.sumOf { it.valuePoisha }

    HgScreen(
        title = "অতিরিক্ত সম্পদ ও দায়",
        subtitle = "নেট সম্পদের হিসাব",
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন আইটেম", style = MaterialTheme.typography.labelLarge) }
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
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("অতিরিক্ত সম্পদ")
                            Spacer(Modifier.height(Space.xs))
                            AmountText(assetTotal, style = HgAmountType.medium, color = Hg.colors.income)
                        }
                        Column(Modifier.weight(1f)) {
                            Eyebrow("অতিরিক্ত দায়")
                            Spacer(Modifier.height(Space.xs))
                            AmountText(liabilityTotal, style = HgAmountType.medium, color = Hg.colors.expense)
                        }
                    }
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "জমি, সোনা, পণ্য বা অন্য দায় এখানে রাখুন — এগুলো নেট সম্পদের হিসাবেই যুক্ত হয়, কোনো ব্যালান্স বদলায় না।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
            }
            if (items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Diamond,
                        title = "কোনো আইটেম নেই",
                        subtitle = "জমি, সোনা, ব্যবসার পণ্য বা অন্য দায় যোগ করুন।",
                        actionLabel = "আইটেম যোগ করুন",
                        onAction = { editing = null; showEditor = true }
                    )
                }
            }
            item {
                Text(
                    "সম্পদ",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
                )
            }
            if (assets.isEmpty()) {
                item {
                    Text(
                        "কোনো অতিরিক্ত সম্পদ যোগ করা হয়নি।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkTertiary,
                        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs)
                    )
                }
            }
            item {
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding),
                    padding = PaddingValues(vertical = Space.xs)
                ) {
                    assets.forEachIndexed { index, item ->
                        NetWorthItemRow(
                            item = item,
                            onEdit = { editing = item; showEditor = true },
                            onDelete = { deleting = item }
                        )
                        if (index != assets.lastIndex) HgDivider(startInset = 20)
                    }
                }
            }
            item {
                Text(
                    "দায়",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
                )
            }
            if (liabilities.isEmpty()) {
                item {
                    Text(
                        "কোনো অতিরিক্ত দায় যোগ করা হয়নি।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkTertiary,
                        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs)
                    )
                }
            }
            item {
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding),
                    padding = PaddingValues(vertical = Space.xs)
                ) {
                    liabilities.forEachIndexed { index, item ->
                        NetWorthItemRow(
                            item = item,
                            onEdit = { editing = item; showEditor = true },
                            onDelete = { deleting = item }
                        )
                        if (index != liabilities.lastIndex) HgDivider(startInset = 20)
                    }
                }
            }
        }
    }

    if (showEditor) {
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var kind by remember { mutableStateOf(editing?.kind ?: NetWorthItemKind.ASSET) }
        var valueText by remember {
            mutableStateOf(editing?.let { poishaToInputText(it.valuePoisha) } ?: "")
        }
        var noteText by remember { mutableStateOf(editing?.note ?: "") }
        var dialogError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            containerColor = Hg.colors.surface,
            shape = Radius.panel,
            title = { Text(if (editing == null) "নতুন সম্পদ/দায়" else "আইটেম বদলান") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    HgSegmented(
                        options = NetWorthItemKind.entries,
                        selected = kind,
                        label = { it.bnLabel() },
                        onSelect = { kind = it }
                    )
                    HgTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "নাম",
                        placeholder = "যেমন: জমি, সোনা, ধান"
                    )
                    MoneyInputField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        label = "আনুমানিক মূল্য (টাকা)",
                        error = dialogError
                    )
                    HgTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = "মন্তব্য (ঐচ্ছিক)"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = BengaliNumber.parsePoisha(valueText)
                    when {
                        name.isBlank() -> dialogError = "নাম দিন"
                        value == null || value < 0 -> dialogError = "সঠিক মূল্য দিন"
                        else -> {
                            val now = Dates.nowMs()
                            val existing = editing
                            if (existing == null) {
                                viewModel.add(
                                    NetWorthItemEntity(
                                        name = name.trim(),
                                        kind = kind,
                                        valuePoisha = value,
                                        note = noteText.ifBlank { null },
                                        createdAt = now, updatedAt = now
                                    )
                                )
                            } else {
                                viewModel.update(
                                    existing.copy(
                                        name = name.trim(),
                                        kind = kind,
                                        valuePoisha = value,
                                        note = noteText.ifBlank { null }
                                    )
                                )
                            }
                            showEditor = false
                        }
                    }
                }) { Text("সংরক্ষণ করুন", color = Hg.colors.brand) }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text("বাতিল", color = Hg.colors.inkSecondary)
                }
            }
        )
    }

    deleting?.let { item ->
        ConfirmDialog(
            title = "আইটেম মুছে ফেলবেন?",
            message = "\"${item.name}\" মুছে যাবে।",
            confirmText = "মুছে ফেলুন",
            onConfirm = { viewModel.delete(item); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun NetWorthItemRow(
    item: NetWorthItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        HgListRow(
            title = item.name,
            subtitle = item.note,
            leading = {
                IconChip(
                    Icons.Filled.Diamond,
                    tint = if (item.kind == NetWorthItemKind.ASSET) Hg.colors.income else Hg.colors.expense,
                    background = if (item.kind == NetWorthItemKind.ASSET) Hg.colors.incomeTint
                    else Hg.colors.expenseTint
                )
            },
            trailing = {
                AmountText(
                    item.valuePoisha,
                    style = HgAmountType.small,
                    color = if (item.kind == NetWorthItemKind.ASSET) Hg.colors.income
                    else Hg.colors.expense
                )
            }
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.lg),
            horizontalArrangement = Arrangement.End
        ) {
            HgButton(text = "বদলান", onClick = onEdit, style = HgButtonStyle.Ghost)
            HgButton(text = "মুছুন", onClick = onDelete, style = HgButtonStyle.Ghost)
        }
    }
}

// ---------------- net worth items ----------------

class NetWorthItemsViewModel(private val planning: PlanningRepository) : ViewModel() {

    val items: StateFlow<List<NetWorthItemEntity>> = planning.observeNetWorthItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(item: NetWorthItemEntity) {
        viewModelScope.launch { planning.addNetWorthItem(item) }
    }

    fun update(item: NetWorthItemEntity) {
        viewModelScope.launch { planning.updateNetWorthItem(item) }
    }

    fun delete(item: NetWorthItemEntity) {
        viewModelScope.launch { planning.deleteNetWorthItem(item) }
    }
}

