package com.shohankhan.hisabghor.feature.planning

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

    Scaffold(
        topBar = { HisabTopBar(title = "নিয়মিত আয়/খরচ", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন নিয়ম যোগ করুন")
            }
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
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(rules, key = { it.id }) { rule ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                rule.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            AmountText(
                                rule.amountPoisha,
                                color = if (rule.isIncome) Emerald600 else Danger600
                            )
                        }
                        Text(
                            (if (rule.isIncome) "আয়" else "খরচ") + " • " + rule.frequency.bnLabel() +
                                " • পরের এন্ট্রি ${Dates.formatShortDate(rule.nextRunDate)}" +
                                (rule.endDate?.let { " • শেষ ${Dates.formatShortDate(it)}" } ?: "") +
                                if (!rule.enabled) " • বন্ধ" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        rule.lastRunAt?.let {
                            Text(
                                "সর্বশেষ এন্ট্রি: ${Dates.formatShortDate(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("চালু", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { checked ->
                                    viewModel.update(rule.copy(enabled = checked))
                                }
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { editing = rule; showEditor = true }) {
                                Text("বদলান")
                            }
                            TextButton(onClick = { deleting = rule }) {
                                Text("মুছুন", color = Danger600)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 16.dp))
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
            title = { Text(if (editing == null) "নতুন নিয়ম" else "নিয়ম বদলান") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isIncome,
                            onClick = { isIncome = false },
                            label = { Text("খরচ") }
                        )
                        FilterChip(
                            selected = isIncome,
                            onClick = { isIncome = true },
                            label = { Text("আয়") }
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("শিরোনাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            RepeatRule.DAILY, RepeatRule.WEEKLY,
                            RepeatRule.MONTHLY, RepeatRule.YEARLY
                        ).forEach { rule ->
                            FilterChip(
                                selected = frequency == rule,
                                onClick = { frequency = rule },
                                label = { Text(rule.bnLabel()) }
                            )
                        }
                    }
                    DateField(
                        date = nextRun,
                        onDateChange = { nextRun = it },
                        label = "প্রথম/পরের এন্ট্রির তারিখ"
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("শেষ তারিখ আছে")
                        Switch(checked = hasEnd, onCheckedChange = { hasEnd = it })
                    }
                    if (hasEnd) {
                        DateField(date = endDate, onDateChange = { endDate = it }, label = "শেষ তারিখ")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("নিজে থেকে এন্ট্রি তৈরি করুন")
                            Text(
                                "বন্ধ থাকলে শুধু রিমাইন্ডার হিসাবে কাজ করবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = autoCreate, onCheckedChange = { autoCreate = it })
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
                }) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("বাতিল") }
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

    Scaffold(
        topBar = { HisabTopBar(title = "অতিরিক্ত সম্পদ ও দায়", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন আইটেম যোগ করুন")
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "অ্যাকাউন্টের বাইরের সম্পদ (জমি, সোনা, পণ্য) ও দায় এখানে রাখুন — এগুলো নেট সম্পদের হিসাবেই শুধু যুক্ত হয়, কোনো ব্যালান্স বদলায় না।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মোট অতিরিক্ত সম্পদ")
                            AmountText(assetTotal, color = Emerald600)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মোট অতিরিক্ত দায়")
                            AmountText(liabilityTotal, color = Danger600)
                        }
                    }
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            items(assets, key = { it.id }) { item ->
                NetWorthItemRow(
                    item = item,
                    onEdit = { editing = item; showEditor = true },
                    onDelete = { deleting = item }
                )
            }
            item {
                Text(
                    "দায়",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            items(liabilities, key = { it.id }) { item ->
                NetWorthItemRow(
                    item = item,
                    onEdit = { editing = item; showEditor = true },
                    onDelete = { deleting = item }
                )
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
            title = { Text(if (editing == null) "নতুন সম্পদ/দায়" else "আইটেম বদলান") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NetWorthItemKind.entries.forEach { k ->
                            FilterChip(
                                selected = kind == k,
                                onClick = { kind = k },
                                label = { Text(k.bnLabel()) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("নাম") },
                        placeholder = { Text("যেমন: জমি, সোনা, ধান") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MoneyInputField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        label = "আনুমানিক মূল্য (টাকা)",
                        error = dialogError
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("মন্তব্য (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth()
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
                }) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("বাতিল") }
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
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        AmountText(
            item.valuePoisha,
            color = if (item.kind == NetWorthItemKind.ASSET) Emerald600 else Danger600
        )
        TextButton(onClick = onEdit) { Text("বদলান") }
        TextButton(onClick = onDelete) { Text("মুছুন", color = Danger600) }
    }
    HorizontalDivider(Modifier.padding(start = 16.dp))
}
