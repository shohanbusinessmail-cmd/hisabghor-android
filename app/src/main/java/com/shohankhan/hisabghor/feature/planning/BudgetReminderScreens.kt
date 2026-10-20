package com.shohankhan.hisabghor.feature.planning

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.BudgetEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.ReminderEntity
import com.shohankhan.hisabghor.core.designsystem.ConfirmDialog
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.LabeledProgress
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.BudgetPeriod
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.RepeatRule
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.BudgetWithUsage
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.PlanningRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------------- budgets ----------------

class BudgetsViewModel(
    private val planning: PlanningRepository,
    categories: CategoryRepository
) : ViewModel() {

    val budgets: StateFlow<List<BudgetWithUsage>> = planning.observeBudgetsWithUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> =
        categories.observeByKind(CategoryKind.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(categoryId: Long?, period: BudgetPeriod, amountPoisha: Long, anchorDay: Int) {
        viewModelScope.launch { planning.addBudget(categoryId, period, amountPoisha, anchorDay) }
    }

    fun update(budget: BudgetEntity) {
        viewModelScope.launch { planning.updateBudget(budget) }
    }

    fun delete(budget: BudgetEntity) {
        viewModelScope.launch { planning.deleteBudget(budget) }
    }
}

@Composable
fun BudgetsScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: BudgetsViewModel = viewModel(
        factory = hisabViewModelFactory {
            BudgetsViewModel(container.planningRepository, container.categoryRepository)
        }
    )
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.expenseCategories.collectAsState()
    var editing by remember { mutableStateOf<BudgetEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<BudgetEntity?>(null) }

    Scaffold(
        topBar = { HisabTopBar(title = "বাজেট", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন বাজেট যোগ করুন")
            }
        }
    ) { padding ->
        if (budgets.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.PieChart,
                title = "কোনো বাজেট তৈরি হয়নি",
                subtitle = "ক্যাটাগরি অনুযায়ী মাসিক খরচের সীমা ঠিক করুন — সীমা ছাড়ালে বাজেট লাল হয়ে যাবে।",
                actionLabel = "বাজেট যোগ করুন",
                onAction = { editing = null; showEditor = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(budgets, key = { it.budget.id }) { item ->
                    val budget = item.budget
                    val overLimit = item.remainingPoisha < 0
                    val percent = (item.usageFraction * 10000).toInt()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                (item.category?.name ?: "সব খরচ") + " • " + budget.period.bnLabel() +
                                    if (!budget.active) " (বন্ধ)" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                BengaliNumber.formatPercentBp(percent),
                                color = if (overLimit) Danger600 else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LabeledProgress(
                            fraction = item.usageFraction,
                            leftText = "খরচ ${formatAmount(item.spentPoisha)}",
                            rightText = "বাজেট ${formatAmount(budget.amountPoisha)}",
                            overLimit = overLimit
                        )
                        Text(
                            if (overLimit) "সীমা ছাড়িয়েছে ${formatAmount(-item.remainingPoisha)}"
                            else "বাকি আছে ${formatAmount(item.remainingPoisha)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overLimit) Danger600
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "সময়কাল: ${Dates.formatShortDate(item.periodFromMs)} — ${Dates.formatShortDate(item.periodToMs - 1)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("সক্রিয়", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = budget.active,
                                onCheckedChange = { checked ->
                                    viewModel.update(budget.copy(active = checked))
                                }
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { editing = budget; showEditor = true }) {
                                Text("বদলান")
                            }
                            TextButton(onClick = { deleting = budget }) {
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
        var categoryId by remember { mutableStateOf(editing?.categoryId) }
        var period by remember { mutableStateOf(editing?.period ?: BudgetPeriod.MONTHLY) }
        var amountText by remember {
            mutableStateOf(editing?.let { poishaToInputText(it.amountPoisha) } ?: "")
        }
        var anchorText by remember {
            mutableStateOf((editing?.anchorDay ?: 1).toString())
        }
        var dialogError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "নতুন বাজেট" else "বাজেট বদলান") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectionField(
                        label = "খরচের ক্যাটাগরি",
                        selectedText = categories.firstOrNull { it.id == categoryId }?.name ?: "সব খরচ",
                        options = categories,
                        optionLabel = { it.name },
                        onSelect = { categoryId = it.id }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BudgetPeriod.entries.forEach { p ->
                            FilterChip(
                                selected = period == p,
                                onClick = { period = p },
                                label = { Text(p.bnLabel()) }
                            )
                        }
                    }
                    MoneyInputField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "বাজেটের পরিমাণ (টাকা)",
                        error = dialogError
                    )
                    if (period == BudgetPeriod.MONTHLY) {
                        OutlinedTextField(
                            value = anchorText,
                            onValueChange = { anchorText = it },
                            label = { Text("মাস শুরুর তারিখ (১–২৮)") },
                            supportingText = { Text("যেমন বেতন পেলে মাস শুরু ধরা হলে সেই তারিখ দিন") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (period == BudgetPeriod.WEEKLY) {
                        Text(
                            "সাপ্তাহিক বাজেট শনিবার থেকে শুরু ধরা হয়।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = BengaliNumber.parsePoisha(amountText)
                    val anchor = BengaliNumber.toAsciiDigits(anchorText).trim()
                        .toIntOrNull()?.coerceIn(1, 28) ?: 1
                    if (amount == null || amount <= 0) {
                        dialogError = "সঠিক পরিমাণ দিন"
                    } else {
                        val existing = editing
                        if (existing == null) {
                            viewModel.add(categoryId, period, amount, anchor)
                        } else {
                            viewModel.update(
                                existing.copy(
                                    categoryId = categoryId,
                                    period = period,
                                    amountPoisha = amount,
                                    anchorDay = anchor
                                )
                            )
                        }
                        showEditor = false
                    }
                }) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("বাতিল") }
            }
        )
    }

    deleting?.let { budget ->
        ConfirmDialog(
            title = "বাজেট মুছে ফেলবেন?",
            message = "বাজেটটি মুছে যাবে; আপনার লেনদেন অপরিবর্তিত থাকবে।",
            confirmText = "মুছে ফেলুন",
            onConfirm = { viewModel.delete(budget); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

// ---------------- reminders ----------------

class RemindersViewModel(
    private val planning: PlanningRepository,
    categories: CategoryRepository
) : ViewModel() {

    val reminders: StateFlow<List<ReminderEntity>> = planning.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> =
        categories.observeByKind(CategoryKind.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(reminder: ReminderEntity) {
        viewModelScope.launch { planning.addReminder(reminder) }
    }

    fun update(reminder: ReminderEntity) {
        viewModelScope.launch { planning.updateReminder(reminder) }
    }

    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch { planning.deleteReminder(reminder) }
    }

    fun markDone(reminder: ReminderEntity) {
        viewModelScope.launch { planning.markReminderDone(reminder) }
    }
}

@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: RemindersViewModel = viewModel(
        factory = hisabViewModelFactory {
            RemindersViewModel(container.planningRepository, container.categoryRepository)
        }
    )
    val reminders by viewModel.reminders.collectAsState()
    val categories by viewModel.expenseCategories.collectAsState()
    var editing by remember { mutableStateOf<ReminderEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ReminderEntity?>(null) }

    val context = LocalContext.current
    var notificationGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationGranted = granted }

    val todayMs = Dates.startOfDayMs(Dates.nowMs())

    Scaffold(
        topBar = { HisabTopBar(title = "রিমাইন্ডার", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন রিমাইন্ডার যোগ করুন")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!notificationGranted) {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "সময়মতো জানান পেতে নোটিফিকেশনের অনুমতি দিন। অনুমতি ছাড়াও রিমাইন্ডার অ্যাপের ভেতরে দেখা যাবে।",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }) { Text("নোটিফিকেশনের অনুমতি দিন") }
                    }
                }
            }
            if (reminders.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Alarm,
                    title = "কোনো রিমাইন্ডার নেই",
                    subtitle = "বিল, ভাড়া, কিস্তি — যেকোনো তারিখের কথা মনে করিয়ে দেওয়ার জন্য রিমাইন্ডার দিন।",
                    actionLabel = "রিমাইন্ডার যোগ করুন",
                    onAction = { editing = null; showEditor = true }
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(reminders, key = { it.id }) { reminder ->
                        val overdue = reminder.enabled && reminder.doneAt == null &&
                            reminder.dueDate < todayMs
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    reminder.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                reminder.amountPoisha?.let {
                                    Text(formatAmount(it), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Text(
                                "${Dates.formatShortDate(reminder.dueDate)} • ${reminder.repeat.bnLabel()}" +
                                    when {
                                        overdue -> " • তারিখ পেরিয়েছে!"
                                        reminder.doneAt != null -> " • সম্পন্ন"
                                        !reminder.enabled -> " • বন্ধ আছে"
                                        else -> ""
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overdue) Danger600
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            reminder.note?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("চালু", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = reminder.enabled,
                                    onCheckedChange = { checked ->
                                        viewModel.update(reminder.copy(enabled = checked))
                                    }
                                )
                                Spacer(Modifier.weight(1f))
                                if (reminder.doneAt == null) {
                                    TextButton(onClick = { viewModel.markDone(reminder) }) {
                                        Text("হয়ে গেছে")
                                    }
                                }
                                TextButton(onClick = { editing = reminder; showEditor = true }) {
                                    Text("বদলান")
                                }
                                TextButton(onClick = { deleting = reminder }) {
                                    Text("মুছুন", color = Danger600)
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }

    if (showEditor) {
        var title by remember { mutableStateOf(editing?.title ?: "") }
        var amountText by remember {
            mutableStateOf(editing?.amountPoisha?.let { poishaToInputText(it) } ?: "")
        }
        var dueDate by remember {
            mutableStateOf(editing?.let { Dates.toLocalDate(it.dueDate) } ?: Dates.today())
        }
        var repeat by remember { mutableStateOf(editing?.repeat ?: RepeatRule.NONE) }
        var categoryId by remember { mutableStateOf(editing?.categoryId) }
        var noteText by remember { mutableStateOf(editing?.note ?: "") }
        var dialogError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "নতুন রিমাইন্ডার" else "রিমাইন্ডার বদলান") },
            text = {
                Column(
                    Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("কিসের রিমাইন্ডার?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MoneyInputField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "পরিমাণ (থাকলে)",
                        error = dialogError
                    )
                    DateField(date = dueDate, onDateChange = { dueDate = it }, label = "তারিখ")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepeatRule.entries.forEach { rule ->
                            FilterChip(
                                selected = repeat == rule,
                                onClick = { repeat = rule },
                                label = { Text(rule.bnLabel()) }
                            )
                        }
                    }
                    SelectionField(
                        label = "ক্যাটাগরি (ঐচ্ছিক)",
                        selectedText = categories.firstOrNull { it.id == categoryId }?.name ?: "কোনোটি নয়",
                        options = categories,
                        optionLabel = { it.name },
                        onSelect = { categoryId = it.id }
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
                    val amount = if (amountText.isBlank()) null
                    else BengaliNumber.parsePoisha(amountText)
                    when {
                        title.isBlank() -> dialogError = "শিরোনাম দিন"
                        amountText.isNotBlank() && amount == null ->
                            dialogError = "পরিমাণ সঠিক নয়"
                        else -> {
                            val now = Dates.nowMs()
                            val existing = editing
                            if (existing == null) {
                                viewModel.add(
                                    ReminderEntity(
                                        title = title.trim(),
                                        amountPoisha = amount,
                                        dueDate = Dates.startOfDayMs(dueDate),
                                        repeat = repeat,
                                        categoryId = categoryId,
                                        note = noteText.ifBlank { null },
                                        createdAt = now, updatedAt = now
                                    )
                                )
                            } else {
                                viewModel.update(
                                    existing.copy(
                                        title = title.trim(),
                                        amountPoisha = amount,
                                        dueDate = Dates.startOfDayMs(dueDate),
                                        repeat = repeat,
                                        categoryId = categoryId,
                                        note = noteText.ifBlank { null },
                                        doneAt = null
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

    deleting?.let { reminder ->
        ConfirmDialog(
            title = "রিমাইন্ডার মুছে ফেলবেন?",
            message = "\"${reminder.title}\" রিমাইন্ডারটি মুছে যাবে।",
            confirmText = "মুছে ফেলুন",
            onConfirm = { viewModel.delete(reminder); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}
