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
import androidx.compose.foundation.layout.width
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
import com.shohankhan.hisabghor.core.designsystem.AmountText
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExtendedFloatingActionButton
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgBadge
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgSegmented
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

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
    val overLimitCount = budgets.count { it.remainingPoisha < 0 }

    HgScreen(
        title = "বাজেট",
        subtitle = "খরচের সীমা",
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন বাজেট", style = MaterialTheme.typography.labelLarge) }
            )
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
            return@HgScreen
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Row(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.md)
                ) {
                    HgBadge("${BengaliNumber.formatCount(budgets.size.toLong())}টি বাজেট")
                    if (overLimitCount > 0) {
                        Spacer(Modifier.width(Space.sm))
                        HgBadge("$overLimitCountটি সীমা ছাড়িয়েছে", tone = HgTone.Expense)
                    }
                }
            }
            items(budgets, key = { it.budget.id }) { item ->
                val budget = item.budget
                val overLimit = item.remainingPoisha < 0
                val percent = (item.usageFraction * 10000).toInt()
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs),
                    padding = PaddingValues(Space.lg)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconChip(Icons.Filled.PieChart)
                        Spacer(Modifier.width(Space.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.category?.name ?: "সব খরচ",
                                style = MaterialTheme.typography.titleMedium,
                                color = Hg.colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                budget.period.bnLabel() + if (!budget.active) " • বন্ধ" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Hg.colors.inkSecondary
                            )
                        }
                        Text(
                            BengaliNumber.formatPercentBp(percent),
                            color = if (overLimit) Hg.colors.expense else Hg.colors.brand,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.height(Space.md))
                    LabeledProgress(
                        fraction = item.usageFraction,
                        leftText = "খরচ ${formatAmount(item.spentPoisha)}",
                        rightText = "বাজেট ${formatAmount(budget.amountPoisha)}",
                        overLimit = overLimit
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        if (overLimit) "সীমা ছাড়িয়েছে ${formatAmount(-item.remainingPoisha)}"
                        else "বাকি আছে ${formatAmount(item.remainingPoisha)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overLimit) Hg.colors.expense else Hg.colors.inkSecondary
                    )
                    Text(
                        "সময়কাল: ${Dates.formatShortDate(item.periodFromMs)} — ${Dates.formatShortDate(item.periodToMs - 1)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkTertiary
                    )
                    Spacer(Modifier.height(Space.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = budget.active,
                            onCheckedChange = { checked ->
                                viewModel.update(budget.copy(active = checked))
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
                            if (budget.active) "সক্রিয়" else "বন্ধ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        HgButton(
                            text = "বদলান",
                            onClick = { editing = budget; showEditor = true },
                            style = HgButtonStyle.Ghost
                        )
                        HgButton(
                            text = "মুছুন",
                            onClick = { deleting = budget },
                            style = HgButtonStyle.Ghost
                        )
                    }
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
            containerColor = Hg.colors.surface,
            shape = Radius.panel,
            title = { Text(if (editing == null) "নতুন বাজেট" else "বাজেট বদলান") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    SelectionField(
                        label = "খরচের ক্যাটাগরি",
                        selectedText = categories.firstOrNull { it.id == categoryId }?.name ?: "সব খরচ",
                        options = categories,
                        optionLabel = { it.name },
                        onSelect = { categoryId = it.id }
                    )
                    HgSegmented(
                        options = BudgetPeriod.entries,
                        selected = period,
                        label = { it.bnLabel() },
                        onSelect = { period = it }
                    )
                    MoneyInputField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "বাজেটের পরিমাণ (টাকা)",
                        error = dialogError
                    )
                    if (period == BudgetPeriod.MONTHLY) {
                        HgTextField(
                            value = anchorText,
                            onValueChange = { anchorText = it },
                            label = "মাস শুরুর তারিখ (১–২৮)",
                            helper = "বেতন পেলে মাস শুরু ধরা হলে সেই তারিখ দিন",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    if (period == BudgetPeriod.WEEKLY) {
                        Text(
                            "সাপ্তাহিক বাজেট শনিবার থেকে শুরু ধরা হয়।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
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
                }) { Text("সংরক্ষণ করুন", color = Hg.colors.brand) }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text("বাতিল", color = Hg.colors.inkSecondary)
                }
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

    HgScreen(
        title = "রিমাইন্ডার",
        subtitle = "বিল, কিস্তি ও তারিখের কথা",
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন রিমাইন্ডার", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        if (reminders.isEmpty() && notificationGranted) {
            EmptyState(
                icon = Icons.Filled.Alarm,
                title = "কোনো রিমাইন্ডার নেই",
                subtitle = "বিল, ভাড়া, কিস্তি — যেকোনো তারিখের কথা মনে করিয়ে দেওয়ার জন্য রিমাইন্ডার দিন।",
                actionLabel = "রিমাইন্ডার যোগ করুন",
                onAction = { editing = null; showEditor = true },
                modifier = Modifier.padding(padding)
            )
            return@HgScreen
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (!notificationGranted) {
                item {
                    HgNotice(
                        title = "নোটিফিকেশনের অনুমতি দিন",
                        message = "সময়মতো জানান পেতে অনুমতি দিন। অনুমতি ছাড়াও রিমাইন্ডার অ্যাপের ভেতরে দেখা যাবে।",
                        tone = HgTone.Warning,
                        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.md),
                        action = {
                            HgButton(
                                text = "অনুমতি দিন",
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                style = HgButtonStyle.Secondary
                            )
                        }
                    )
                }
            }
            if (reminders.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Alarm,
                        title = "কোনো রিমাইন্ডার নেই",
                        subtitle = "বিল, ভাড়া, কিস্তি — যেকোনো তারিখের কথা মনে করিয়ে দেওয়ার জন্য রিমাইন্ডার দিন।",
                        actionLabel = "রিমাইন্ডার যোগ করুন",
                        onAction = { editing = null; showEditor = true }
                    )
                }
            }
            items(reminders, key = { it.id }) { reminder ->
                val overdue = reminder.enabled && reminder.doneAt == null &&
                    reminder.dueDate < todayMs
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs),
                    padding = PaddingValues(Space.lg)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconChip(
                            Icons.Filled.Alarm,
                            tint = if (reminder.doneAt != null) Hg.colors.inkSecondary
                            else if (overdue) Hg.colors.warning else Hg.colors.brand,
                            background = if (reminder.doneAt != null) Hg.colors.surfaceMuted
                            else if (overdue) Hg.colors.warningTint else Hg.colors.brandTint
                        )
                        Spacer(Modifier.width(Space.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                reminder.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Hg.colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${Dates.formatShortDate(reminder.dueDate)} • ${reminder.repeat.bnLabel()}" +
                                    when {
                                        overdue -> " • তারিখ পেরিয়েছে"
                                        reminder.doneAt != null -> " • সম্পন্ন"
                                        !reminder.enabled -> " • বন্ধ আছে"
                                        else -> ""
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overdue) Hg.colors.expense else Hg.colors.inkSecondary
                            )
                        }
                        reminder.amountPoisha?.let {
                            AmountText(it, style = HgAmountType.small, color = Hg.colors.ink)
                        }
                    }
                    reminder.note?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(Space.sm))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary)
                    }
                    Spacer(Modifier.height(Space.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = reminder.enabled,
                            onCheckedChange = { checked ->
                                viewModel.update(reminder.copy(enabled = checked))
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
                            if (reminder.enabled) "চালু" else "বন্ধ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hg.colors.inkSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        if (reminder.doneAt == null) {
                            HgButton(
                                text = "হয়ে গেছে",
                                onClick = { viewModel.markDone(reminder) },
                                style = HgButtonStyle.Ghost
                            )
                        }
                        HgButton(
                            text = "বদলান",
                            onClick = { editing = reminder; showEditor = true },
                            style = HgButtonStyle.Ghost
                        )
                        HgButton(
                            text = "মুছুন",
                            onClick = { deleting = reminder },
                            style = HgButtonStyle.Ghost
                        )
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
        var repeatRule by remember { mutableStateOf(editing?.repeat ?: RepeatRule.NONE) }
        var categoryId by remember { mutableStateOf(editing?.categoryId) }
        var noteText by remember { mutableStateOf(editing?.note ?: "") }
        var dialogError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            containerColor = Hg.colors.surface,
            shape = Radius.panel,
            title = { Text(if (editing == null) "নতুন রিমাইন্ডার" else "রিমাইন্ডার বদলান") },
            text = {
                Column(
                    Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    HgTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "কিসের রিমাইন্ডার?"
                    )
                    MoneyInputField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "পরিমাণ (থাকলে)",
                        error = dialogError
                    )
                    DateField(date = dueDate, onDateChange = { dueDate = it }, label = "তারিখ")
                    HgSegmented(
                        options = RepeatRule.entries,
                        selected = repeatRule,
                        label = { it.bnLabel() },
                        onSelect = { repeatRule = it }
                    )
                    SelectionField(
                        label = "ক্যাটাগরি (ঐচ্ছিক)",
                        selectedText = categories.firstOrNull { it.id == categoryId }?.name ?: "কোনোটি নয়",
                        options = categories,
                        optionLabel = { it.name },
                        onSelect = { categoryId = it.id }
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
                                        repeat = repeatRule,
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
                                        repeat = repeatRule,
                                        categoryId = categoryId,
                                        note = noteText.ifBlank { null },
                                        doneAt = null
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

