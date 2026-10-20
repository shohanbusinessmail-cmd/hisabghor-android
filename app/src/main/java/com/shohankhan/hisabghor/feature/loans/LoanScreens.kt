package com.shohankhan.hisabghor.feature.loans

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.InstallmentEntity
import com.shohankhan.hisabghor.core.database.LoanEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.ConfirmDialog
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.InstallmentFrequency
import com.shohankhan.hisabghor.core.model.InterestMethod
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.LoanStatus
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.core.money.LoanMath
import com.shohankhan.hisabghor.core.money.ScheduledInstallment
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.LoanRepository
import com.shohankhan.hisabghor.data.LoanWithBalance
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.TransactionRow
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

// ---------------- loan list ----------------

class LoanListViewModel(loans: LoanRepository) : ViewModel() {
    val organizationLoans: StateFlow<List<LoanWithBalance>> =
        loans.observeLoansWithBalances().map { list ->
            list.filter { it.loan.kind == LoanKind.NGO || it.loan.kind == LoanKind.BANK }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun LoanListScreen(
    onNewLoan: (kind: String) -> Unit,
    onOpenLoan: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: LoanListViewModel = viewModel(
        factory = hisabViewModelFactory { LoanListViewModel(container.loanRepository) }
    )
    val loans by viewModel.organizationLoans.collectAsState()
    var showKindDialog by remember { mutableStateOf(false) }
    val totalOutstanding = loans
        .filter { it.loan.status == LoanStatus.ACTIVE }
        .sumOf { it.outstandingPoisha }

    Scaffold(
        topBar = { HisabTopBar(title = "এনজিও ও ব্যাংক ঋণ") },
        floatingActionButton = {
            FloatingActionButton(onClick = { showKindDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন ঋণ যোগ করুন")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("মোট আসল বাকি", style = MaterialTheme.typography.bodyMedium)
                    AmountText(
                        totalOutstanding,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Danger600
                    )
                }
            }
            if (loans.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.AccountBalance,
                    title = "কোনো ঋণ নেই",
                    subtitle = "এনজিও বা ব্যাংক থেকে নেওয়া কিস্তির ঋণ এখানে রাখুন।",
                    actionLabel = "ঋণ যোগ করুন",
                    onAction = { showKindDialog = true }
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(loans, key = { it.loan.id }) { item ->
                        val loan = item.loan
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLoan(loan.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    loan.organizationName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    loan.kind.bnLabel() +
                                        (loan.branch?.let { " • $it" } ?: "") +
                                        " • আসল ${formatAmount(loan.principalPoisha)}" +
                                        if (loan.status == LoanStatus.CLOSED) " • পরিশোধিত" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("আসল বাকি", style = MaterialTheme.typography.bodySmall)
                                AmountText(item.outstandingPoisha, color = Danger600)
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 52.dp))
                    }
                }
            }
        }
    }

    if (showKindDialog) {
        AlertDialog(
            onDismissRequest = { showKindDialog = false },
            title = { Text("কোন ধরনের ঋণ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showKindDialog = false; onNewLoan("NGO") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("এনজিও ঋণ") }
                    OutlinedButton(
                        onClick = { showKindDialog = false; onNewLoan("BANK") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("ব্যাংক ঋণ") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showKindDialog = false }) { Text("বাতিল") }
            }
        )
    }
}

// ---------------- loan editor ----------------

enum class FlatInputMode { TOTAL, PER_INSTALLMENT, RATE }

data class LoanEditorUiState(
    val loading: Boolean = true,
    val orgName: String = "",
    val branch: String = "",
    val maskedRef: String = "",
    val principalText: String = "",
    val disbursedText: String = "",
    val method: InterestMethod = InterestMethod.FLAT,
    val flatMode: FlatInputMode = FlatInputMode.TOTAL,
    val totalRepayableText: String = "",
    val perInstallmentText: String = "",
    val rateText: String = "",
    val countText: String = "",
    val frequency: InstallmentFrequency = InstallmentFrequency.MONTHLY,
    val startDate: LocalDate = Dates.today(),
    val firstDueDate: LocalDate = Dates.today().plusMonths(1),
    val moneyMovedNow: Boolean = true,
    val accountId: Long? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val note: String = "",
    val error: String? = null,
    val saved: Boolean = false
)

class LoanEditorViewModel(
    private val kind: LoanKind,
    private val accounts: AccountRepository,
    private val loans: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoanEditorUiState())
    val state: StateFlow<LoanEditorUiState> = _state

    init {
        viewModelScope.launch {
            val accountList = accounts.observeAccountsWithBalances().first().map { it.account }
            _state.update {
                it.copy(
                    loading = false,
                    accounts = accountList,
                    accountId = accountList.firstOrNull()?.id
                )
            }
        }
    }

    fun update(change: (LoanEditorUiState) -> LoanEditorUiState) = _state.update(change)

    /** Schedule preview computed with the exact same math the repository uses. */
    fun previewSchedule(s: LoanEditorUiState): List<ScheduledInstallment>? {
        val principal = BengaliNumber.parsePoisha(s.principalText) ?: return null
        val count = BengaliNumber.toAsciiDigits(s.countText).trim().toIntOrNull() ?: return null
        if (principal <= 0 || count <= 0 || count > 600) return null
        return try {
            when (s.method) {
                InterestMethod.REDUCING_AMORTIZED -> {
                    val bp = BengaliNumber.parsePercentToBp(s.rateText) ?: return null
                    LoanMath.amortizedSchedule(principal, bp, count, s.firstDueDate, s.frequency)
                }
                InterestMethod.FLAT -> when (s.flatMode) {
                    FlatInputMode.TOTAL -> {
                        val total = BengaliNumber.parsePoisha(s.totalRepayableText) ?: return null
                        if (total < principal) return null
                        LoanMath.flatSchedule(principal, total, count, s.firstDueDate, s.frequency)
                    }
                    FlatInputMode.PER_INSTALLMENT -> {
                        val per = BengaliNumber.parsePoisha(s.perInstallmentText) ?: return null
                        LoanMath.fixedInstallmentSchedule(principal, per, count, s.firstDueDate, s.frequency)
                    }
                    FlatInputMode.RATE -> {
                        val bp = BengaliNumber.parsePercentToBp(s.rateText) ?: return null
                        val ppy = LoanMath.periodsPerYear(s.frequency)
                        val interest = BigDecimal(principal)
                            .multiply(BigDecimal(bp))
                            .multiply(BigDecimal(count))
                            .divide(BigDecimal(10_000L * ppy), 0, RoundingMode.HALF_UP)
                            .toLong()
                        LoanMath.flatSchedule(principal, principal + interest, count, s.firstDueDate, s.frequency)
                    }
                }
                InterestMethod.SIMPLE, InterestMethod.NONE -> {
                    val total = if (s.method == InterestMethod.SIMPLE && s.totalRepayableText.isNotBlank()) {
                        BengaliNumber.parsePoisha(s.totalRepayableText) ?: return null
                    } else principal
                    if (total < principal) return null
                    LoanMath.flatSchedule(principal, total, count, s.firstDueDate, s.frequency)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun save() {
        val s = _state.value
        val principal = BengaliNumber.parsePoisha(s.principalText)
        if (s.orgName.isBlank()) {
            _state.update { it.copy(error = "প্রতিষ্ঠানের নাম দিন") }
            return
        }
        if (principal == null || principal <= 0) {
            _state.update { it.copy(error = "আসলের সঠিক পরিমাণ দিন") }
            return
        }
        val count = BengaliNumber.toAsciiDigits(s.countText).trim().toIntOrNull()
        if (count == null || count <= 0 || count > 600) {
            _state.update { it.copy(error = "কিস্তির সংখ্যা ঠিক করে দিন") }
            return
        }
        if (previewSchedule(s) == null) {
            _state.update { it.copy(error = "সুদ/কিস্তির তথ্য ঠিক করুন — পূর্বরূপ তৈরি হচ্ছে না") }
            return
        }
        if (s.moneyMovedNow && s.accountId == null) {
            _state.update { it.copy(error = "অ্যাকাউন্ট বেছে নিন") }
            return
        }
        val disbursed = if (s.disbursedText.isBlank()) principal
        else BengaliNumber.parsePoisha(s.disbursedText) ?: run {
            _state.update { it.copy(error = "হাতে পাওয়া টাকার পরিমাণ সঠিক নয়") }
            return
        }
        if (disbursed < 0 || disbursed > principal) {
            _state.update { it.copy(error = "হাতে পাওয়া টাকা আসলের বেশি হতে পারে না") }
            return
        }
        val rateBp = when (s.method) {
            InterestMethod.REDUCING_AMORTIZED -> BengaliNumber.parsePercentToBp(s.rateText) ?: 0
            InterestMethod.FLAT -> if (s.flatMode == FlatInputMode.RATE)
                BengaliNumber.parsePercentToBp(s.rateText) ?: 0 else 0
            else -> 0
        }
        val totalContractual = when {
            s.method == InterestMethod.FLAT && s.flatMode == FlatInputMode.TOTAL ->
                BengaliNumber.parsePoisha(s.totalRepayableText)
            s.method == InterestMethod.SIMPLE && s.totalRepayableText.isNotBlank() ->
                BengaliNumber.parsePoisha(s.totalRepayableText)
            else -> null
        }
        val perInstallment = if (s.method == InterestMethod.FLAT && s.flatMode == FlatInputMode.PER_INSTALLMENT)
            BengaliNumber.parsePoisha(s.perInstallmentText) else null
        viewModelScope.launch {
            try {
                loans.createInstallmentLoan(
                    kind = kind,
                    organizationName = s.orgName,
                    branch = s.branch.ifBlank { null },
                    maskedRef = s.maskedRef.ifBlank { null },
                    accountId = s.accountId,
                    principalPoisha = principal,
                    disbursedPoisha = disbursed,
                    interestMethod = s.method,
                    interestRateBp = rateBp,
                    totalContractualPoisha = totalContractual,
                    installmentAmountPoisha = perInstallment,
                    installmentCount = count,
                    frequency = s.frequency,
                    startDateMs = Dates.startOfDayMs(s.startDate) + 12 * 3600_000L,
                    firstDueDateMs = Dates.startOfDayMs(s.firstDueDate),
                    note = s.note.ifBlank { null },
                    moneyMovedNow = s.moneyMovedNow
                )
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "সংরক্ষণ করা যায়নি — তথ্য যাচাই করে আবার চেষ্টা করুন") }
            }
        }
    }
}

@Composable
fun LoanEditorScreen(
    kindArg: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val kind = if (kindArg == "BANK") LoanKind.BANK else LoanKind.NGO
    val viewModel: LoanEditorViewModel = viewModel(
        key = "loan_editor_$kindArg",
        factory = hisabViewModelFactory {
            LoanEditorViewModel(kind, container.accountRepository, container.loanRepository)
        }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    val preview = remember(state) { viewModel.previewSchedule(state) }

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (kind == LoanKind.BANK) "নতুন ব্যাংক ঋণ" else "নতুন এনজিও ঋণ",
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
                "ঋণের আসল টাকা আয় নয় — এটি আপনার দেনা হিসেবে থাকবে; শুধু সুদ/চার্জ খরচ হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.orgName,
                onValueChange = { v -> viewModel.update { it.copy(orgName = v) } },
                label = { Text("প্রতিষ্ঠানের নাম") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.branch,
                onValueChange = { v -> viewModel.update { it.copy(branch = v) } },
                label = { Text("শাখা (ঐচ্ছিক)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.maskedRef,
                onValueChange = { v -> viewModel.update { it.copy(maskedRef = v) } },
                label = { Text("ঋণ/সদস্য নম্বর (ঐচ্ছিক)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            MoneyInputField(
                value = state.principalText,
                onValueChange = { v -> viewModel.update { it.copy(principalText = v) } },
                label = "ঋণের আসল পরিমাণ (টাকা)",
                error = state.error
            )
            MoneyInputField(
                value = state.disbursedText,
                onValueChange = { v -> viewModel.update { it.copy(disbursedText = v) } },
                label = "আসলে হাতে পেয়েছেন (কম হলে লিখুন)"
            )
            Text(
                "হাতে পাওয়া টাকা আসলের কম হলে পার্থক্যটা প্রসেসিং চার্জ হিসেবে খরচ দেখানো হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("সুদের পদ্ধতি", style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InterestMethod.entries.forEach { method ->
                    FilterChip(
                        selected = state.method == method,
                        onClick = { viewModel.update { it.copy(method = method) } },
                        label = { Text(method.bnLabel()) }
                    )
                }
            }
            when (state.method) {
                InterestMethod.REDUCING_AMORTIZED -> {
                    OutlinedTextField(
                        value = state.rateText,
                        onValueChange = { v -> viewModel.update { it.copy(rateText = v) } },
                        label = { Text("বার্ষিক সুদের হার %") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                InterestMethod.FLAT -> {
                    Text("কীভাবে হিসাব করবেন?", style = MaterialTheme.typography.titleSmall)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.flatMode == FlatInputMode.TOTAL,
                            onClick = { viewModel.update { it.copy(flatMode = FlatInputMode.TOTAL) } },
                            label = { Text("মোট ফেরতযোগ্য") }
                        )
                        FilterChip(
                            selected = state.flatMode == FlatInputMode.PER_INSTALLMENT,
                            onClick = { viewModel.update { it.copy(flatMode = FlatInputMode.PER_INSTALLMENT) } },
                            label = { Text("প্রতি কিস্তি") }
                        )
                        FilterChip(
                            selected = state.flatMode == FlatInputMode.RATE,
                            onClick = { viewModel.update { it.copy(flatMode = FlatInputMode.RATE) } },
                            label = { Text("বার্ষিক হার") }
                        )
                    }
                    when (state.flatMode) {
                        FlatInputMode.TOTAL -> MoneyInputField(
                            value = state.totalRepayableText,
                            onValueChange = { v -> viewModel.update { it.copy(totalRepayableText = v) } },
                            label = "মোট ফেরতযোগ্য (আসল + সুদ)"
                        )
                        FlatInputMode.PER_INSTALLMENT -> MoneyInputField(
                            value = state.perInstallmentText,
                            onValueChange = { v -> viewModel.update { it.copy(perInstallmentText = v) } },
                            label = "প্রতি কিস্তির পরিমাণ"
                        )
                        FlatInputMode.RATE -> OutlinedTextField(
                            value = state.rateText,
                            onValueChange = { v -> viewModel.update { it.copy(rateText = v) } },
                            label = { Text("বার্ষিক সুদের হার %") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                InterestMethod.SIMPLE -> MoneyInputField(
                    value = state.totalRepayableText,
                    onValueChange = { v -> viewModel.update { it.copy(totalRepayableText = v) } },
                    label = "মোট ফেরতযোগ্য (আসল + সুদ)"
                )
                InterestMethod.NONE -> Text(
                    "সুদ ছাড়া — শুধু আসল সমান কিস্তিতে ভাগ হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = state.countText,
                onValueChange = { v -> viewModel.update { it.copy(countText = v) } },
                label = { Text("কিস্তির সংখ্যা") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text("কিস্তির ব্যবধান", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(InstallmentFrequency.WEEKLY, InstallmentFrequency.MONTHLY).forEach { freq ->
                    FilterChip(
                        selected = state.frequency == freq,
                        onClick = { viewModel.update { it.copy(frequency = freq) } },
                        label = { Text(freq.bnLabel()) }
                    )
                }
            }
            DateField(
                date = state.startDate,
                onDateChange = { v -> viewModel.update { it.copy(startDate = v) } },
                label = "ঋণ নেওয়ার তারিখ"
            )
            DateField(
                date = state.firstDueDate,
                onDateChange = { v -> viewModel.update { it.copy(firstDueDate = v) } },
                label = "প্রথম কিস্তির তারিখ"
            )
            if (preview != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("কিস্তির পূর্বরূপ", style = MaterialTheme.typography.titleSmall)
                        KvRow(
                            "প্রতি কিস্তি (সাধারণত)",
                            formatAmount(preview.firstOrNull()?.amountPoisha ?: 0)
                        )
                        KvRow("মোট সুদ", formatAmount(preview.sumOf { it.interestPoisha }))
                        KvRow("মোট ফেরতযোগ্য", formatAmount(preview.sumOf { it.amountPoisha }))
                        KvRow(
                            "শেষ কিস্তির তারিখ",
                            preview.lastOrNull()?.let { Dates.formatShortDate(Dates.startOfDayMs(it.dueDate)) } ?: "-"
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("টাকা এখনই পেয়েছেন", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (state.moneyMovedNow) "অ্যাকাউন্টে টাকা এখনই যোগ হবে"
                        else "আগের ঋণ — শুধু বাকি হিসেবে যোগ হবে",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.moneyMovedNow,
                    onCheckedChange = { checked -> viewModel.update { it.copy(moneyMovedNow = checked) } }
                )
            }
            if (state.moneyMovedNow) {
                SelectionField(
                    label = "যে অ্যাকাউন্টে টাকা এল",
                    selectedText = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "বেছে নিন",
                    options = state.accounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(accountId = selected.id) } }
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
                Text("ঋণ তৈরি করুন")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------------- loan detail ----------------

data class LoanDetailUiState(
    val loading: Boolean = true,
    val loan: LoanEntity? = null,
    val outstandingPoisha: Long = 0,
    val totalRepaidPoisha: Long = 0,
    val installments: List<InstallmentEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class LoanDetailViewModel(
    private val loanId: Long,
    private val loans: LoanRepository,
    transactions: TransactionRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LoanDetailUiState> = combine(
        loans.observeLoan(loanId),
        loans.observeInstallments(loanId),
        transactions.observeFiltered(loanId = loanId),
        _message
    ) { loan, installments, txns, message ->
        LoanDetailUiState(
            loading = false,
            loan = loan,
            installments = installments,
            transactions = txns,
            message = message
        )
    }.let { base ->
        combine(base, balanceFlows()) { state, balances ->
            state.copy(
                outstandingPoisha = balances.first,
                totalRepaidPoisha = balances.second
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoanDetailUiState())

    private fun balanceFlows() = loans.observeLoan(loanId).flatMapLatest { loan ->
        if (loan == null) kotlinx.coroutines.flow.flowOf(0L to 0L)
        else combine(
            loans.observeOutstanding(loan),
            loans.observeTotalCashRepaid(loanId, loan.kind)
        ) { outstanding, repaid -> outstanding to repaid }
    }

    fun clearMessage() { _message.value = null }

    fun setStatus(status: LoanStatus) {
        viewModelScope.launch { loans.setLoanStatus(loanId, status) }
    }

    fun regenerateSchedule() {
        viewModelScope.launch {
            val ok = loans.regenerateSchedule(loanId)
            _message.value = if (ok) "কিস্তির তালিকা আবার তৈরি করা হয়েছে"
            else "কিস্তির তালিকা বদলানো যায়নি (হয়তো কিছু পরিশোধ হয়ে গেছে)"
        }
    }

    fun updateInstallment(installment: InstallmentEntity) {
        viewModelScope.launch { loans.updateInstallment(installment) }
    }

    fun deleteIfUnused(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val ok = loans.deleteLoanIfUnused(loanId)
            if (ok) onDeleted()
            else _message.value = "এই ঋণের লেনদেন থাকায় মুছে ফেলা যাচ্ছে না"
        }
    }
}

@Composable
fun LoanDetailScreen(
    loanId: Long,
    onBack: () -> Unit,
    onRecordPayment: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: LoanDetailViewModel = viewModel(
        key = "loan_detail_$loanId",
        factory = hisabViewModelFactory {
            LoanDetailViewModel(loanId, container.loanRepository, container.transactionRepository)
        }
    )
    val state by viewModel.uiState.collectAsState()
    val loan = state.loan
    var showCloseConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingInstallment by remember { mutableStateOf<InstallmentEntity?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            HisabTopBar(title = loan?.organizationName ?: "ঋণ", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        if (state.loading || loan == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val todayMs = Dates.startOfDayMs(Dates.nowMs())
        val totalScheduled = state.installments.sumOf { it.amountPoisha }
        val paidScheduled = state.installments.sumOf { it.paidTotalPoisha }
        val nextDue = state.installments.firstOrNull { it.remainingPoisha > 0 }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("আসল বাকি", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatAmount(state.outstandingPoisha),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Danger600
                        )
                        Spacer(Modifier.height(6.dp))
                        KvRow("মোট আসল", formatAmount(loan.principalPoisha))
                        if (loan.disbursedPoisha != loan.principalPoisha) {
                            KvRow("হাতে পেয়েছিলেন", formatAmount(loan.disbursedPoisha))
                        }
                        KvRow("মোট পরিশোধ (নগদ)", formatAmount(state.totalRepaidPoisha))
                        KvRow(
                            "কিস্তি অনুযায়ী পরিশোধ",
                            "${formatAmount(paidScheduled)} / ${formatAmount(totalScheduled)}"
                        )
                        loan.branch?.let { KvRow("শাখা", it) }
                        loan.maskedRef?.let { KvRow("ঋণ/সদস্য নম্বর", it) }
                        KvRow("সুদের পদ্ধতি", loan.interestMethod.bnLabel())
                        if (loan.interestRateBp > 0) {
                            KvRow("সুদের হার", BengaliNumber.formatPercentBp(loan.interestRateBp) + " (বার্ষিক)")
                        }
                        nextDue?.let {
                            KvRow(
                                "পরের কিস্তি",
                                "${Dates.formatShortDate(it.dueDate)} — ${formatAmount(it.remainingPoisha)}"
                            )
                        }
                        KvRow(
                            "অবস্থা",
                            if (loan.status == LoanStatus.ACTIVE) "চলমান" else "পরিশোধিত/বন্ধ"
                        )
                        loan.note?.let { KvRow("মন্তব্য", it) }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onRecordPayment(loanId) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("কিস্তি পরিশোধ করুন") }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = viewModel::regenerateSchedule,
                                modifier = Modifier.weight(1f)
                            ) { Text("কিস্তি পুনঃতৈরি") }
                            OutlinedButton(
                                onClick = { showCloseConfirm = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (loan.status == LoanStatus.ACTIVE) "ঋণ বন্ধ করুন" else "আবার চালু করুন")
                            }
                        }
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("ঋণটি মুছে ফেলুন", color = Danger600)
                        }
                    }
                }
                Text(
                    "কিস্তির তালিকা (বদলাতে কোনো কিস্তিতে চাপ দিন)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            items(state.installments, key = { it.id }) { inst ->
                val statusText = when {
                    inst.remainingPoisha <= 0 -> "পরিশোধিত"
                    inst.dueDate < todayMs -> "মেয়াদ পেরিয়েছে!"
                    inst.paidTotalPoisha > 0 -> "আংশিক পরিশোধ"
                    else -> "আসন্ন"
                }
                val statusColor = when {
                    inst.remainingPoisha <= 0 -> MaterialTheme.colorScheme.primary
                    inst.dueDate < todayMs -> Danger600
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editingInstallment = inst }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "কিস্তি ${BengaliNumber.formatCount(inst.seq.toLong())} • ${Dates.formatShortDate(inst.dueDate)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "আসল ${formatAmount(inst.principalPartPoisha)} + সুদ ${formatAmount(inst.interestPartPoisha)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        AmountText(inst.amountPoisha)
                        Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                        if (inst.paidTotalPoisha > 0 && inst.remainingPoisha > 0) {
                            Text(
                                "বাকি ${formatAmount(inst.remainingPoisha)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
            }
            item {
                Text(
                    "লেনদেনের ইতিহাস",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(state.transactions, key = { it.id }) { txn ->
                TransactionRow(txn, onClick = { onOpenTransaction(txn.id) })
                HorizontalDivider(Modifier.padding(start = 70.dp))
            }
        }
    }

    if (showCloseConfirm && loan != null) {
        val closing = loan.status == LoanStatus.ACTIVE
        ConfirmDialog(
            title = if (closing) "ঋণ বন্ধ করবেন?" else "ঋণ আবার চালু করবেন?",
            message = if (closing) "বন্ধ করলে ঋণটি চলমান তালিকায় থাকবে না। হিসাবের অঙ্ক বদলাবে না।"
            else "ঋণটি আবার চলমান হিসেবে দেখানো হবে।",
            confirmText = if (closing) "বন্ধ করুন" else "চালু করুন",
            onConfirm = {
                viewModel.setStatus(if (closing) LoanStatus.CLOSED else LoanStatus.ACTIVE)
                showCloseConfirm = false
            },
            onDismiss = { showCloseConfirm = false }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "ঋণ মুছে ফেলবেন?",
            message = "শুধু কোনো লেনদেন না থাকলে মুছে ফেলা যাবে।",
            confirmText = "মুছে ফেলুন",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteIfUnused(onDeleted = onBack)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    editingInstallment?.let { inst ->
        var dueDate by remember(inst.id) { mutableStateOf(Dates.toLocalDate(inst.dueDate)) }
        var principalText by remember(inst.id) { mutableStateOf(poishaToInputText(inst.principalPartPoisha)) }
        var interestText by remember(inst.id) { mutableStateOf(poishaToInputText(inst.interestPartPoisha)) }
        var dialogError by remember(inst.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { editingInstallment = null },
            title = { Text("কিস্তি ${BengaliNumber.formatCount(inst.seq.toLong())} বদলান") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField(date = dueDate, onDateChange = { dueDate = it }, label = "কিস্তির তারিখ")
                    MoneyInputField(
                        value = principalText,
                        onValueChange = { principalText = it },
                        label = "আসল অংশ (টাকা)",
                        error = dialogError
                    )
                    MoneyInputField(
                        value = interestText,
                        onValueChange = { interestText = it },
                        label = "সুদ অংশ (টাকা)"
                    )
                    Text(
                        "ইতিমধ্যে পরিশোধিত অংশের চেয়ে কমানো যাবে না।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = BengaliNumber.parsePoisha(principalText)
                    val i = BengaliNumber.parsePoisha(interestText)
                    if (p == null || i == null || p < inst.paidPrincipalPoisha || i < inst.paidInterestPoisha) {
                        dialogError = "অঙ্ক ঠিক করুন (পরিশোধিতের কম হতে পারবে না)"
                    } else {
                        viewModel.updateInstallment(
                            inst.copy(
                                dueDate = Dates.startOfDayMs(dueDate),
                                principalPartPoisha = p,
                                interestPartPoisha = i,
                                amountPoisha = p + i + inst.feePartPoisha
                            )
                        )
                        editingInstallment = null
                    }
                }) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { editingInstallment = null }) { Text("বাতিল") }
            }
        )
    }
}
