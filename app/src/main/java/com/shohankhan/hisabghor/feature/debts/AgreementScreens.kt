package com.shohankhan.hisabghor.feature.debts

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.LoanEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.designsystem.ConfirmDialog
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.Emerald600
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.LoanStatus
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.core.money.LoanMath
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.LoanRepository
import com.shohankhan.hisabghor.data.PartyRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.TransactionRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountInput
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgBadge
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgFormBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgListRow
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

// ---------------- agreement editor ----------------

data class AgreementEditorUiState(
    val loading: Boolean = true,
    val personName: String = "",
    val amountText: String = "",
    val accountId: Long? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val moneyMovedNow: Boolean = true,
    val date: LocalDate = Dates.today(),
    val hasDueDate: Boolean = false,
    val dueDate: LocalDate = Dates.today().plusMonths(1),
    val interestRateText: String = "",
    val purpose: String = "",
    val note: String = "",
    val error: String? = null,
    val saved: Boolean = false
)

class AgreementEditorViewModel(
    private val personId: Long,
    private val kind: LoanKind,
    parties: PartyRepository,
    private val accounts: AccountRepository,
    private val loans: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AgreementEditorUiState())
    val state: StateFlow<AgreementEditorUiState> = _state

    init {
        viewModelScope.launch {
            val party = parties.getById(personId)
            val accountList = accounts.observeAccountsWithBalances().first().map { it.account }
            _state.update {
                it.copy(
                    loading = false,
                    personName = party?.name ?: "",
                    accounts = accountList,
                    accountId = accountList.firstOrNull()?.id
                )
            }
        }
    }

    fun update(change: (AgreementEditorUiState) -> AgreementEditorUiState) = _state.update(change)

    fun save() {
        val s = _state.value
        val amount = BengaliNumber.parsePoisha(s.amountText)
        if (amount == null || amount <= 0) {
            _state.update { it.copy(error = "সঠিক পরিমাণ দিন") }
            return
        }
        if (s.moneyMovedNow && s.accountId == null) {
            _state.update { it.copy(error = "অ্যাকাউন্ট বেছে নিন") }
            return
        }
        val rateBp = if (s.interestRateText.isBlank()) 0
        else BengaliNumber.parsePercentToBp(s.interestRateText) ?: run {
            _state.update { it.copy(error = "সুদের হার সঠিক নয় (যেমন: ১০ বা ১২.৫)") }
            return
        }
        viewModelScope.launch {
            try {
                loans.createPersonalAgreement(
                    kind = kind,
                    partyId = personId,
                    displayName = s.personName,
                    accountId = s.accountId,
                    principalPoisha = amount,
                    startDateMs = Dates.startOfDayMs(s.date) + 12 * 3600_000L,
                    dueDateMs = if (s.hasDueDate) Dates.startOfDayMs(s.dueDate) else null,
                    interestRateBp = rateBp,
                    purpose = s.purpose.ifBlank { null },
                    note = s.note.ifBlank { null },
                    moneyMovedNow = s.moneyMovedNow
                )
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "সংরক্ষণ করা যায়নি — আবার চেষ্টা করুন") }
            }
        }
    }
}

@Composable
fun AgreementEditorScreen(
    personId: Long,
    kindArg: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val kind = if (kindArg == "PERSONAL_LEND") LoanKind.PERSONAL_LEND else LoanKind.PERSONAL_BORROW
    val isLend = kind == LoanKind.PERSONAL_LEND
    val viewModel: AgreementEditorViewModel = viewModel(
        key = "agreement_editor_${personId}_$kindArg",
        factory = hisabViewModelFactory {
            AgreementEditorViewModel(
                personId, kind, container.partyRepository,
                container.accountRepository, container.loanRepository
            )
        }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    HgScreen(
        title = if (isLend) "ধার দিলাম" else "ধার নিলাম",
        subtitle = state.personName.ifBlank { null },
        onBack = onBack,
        bottomBar = {
            HgFormBottomBar {
                HgButton(
                    text = "চুক্তি তৈরি করুন",
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
            HgAmountInput(
                value = state.amountText,
                onValueChange = { v -> viewModel.update { it.copy(amountText = v) } },
                error = state.error
            )
            Spacer(Modifier.height(Space.sm))
            HgNotice(
                title = if (isLend) "ধার দেওয়া টাকা খরচ নয়" else "ধার নেওয়া টাকা আয় নয়",
                message = if (isLend) "এটি আপনার পাওনা হিসেবে থাকবে, খরচ হিসেবে নয়।"
                else "এটি আপনার দেনা হিসেবে থাকবে, আয় হিসেবে নয়।",
                tone = HgTone.Neutral
            )

            Spacer(Modifier.height(Space.lg))
            HgCard(padding = PaddingValues(Space.lg)) {
                SwitchRow(
                    title = "টাকা এখনই হাত বদলেছে",
                    subtitle = if (state.moneyMovedNow) "অ্যাকাউন্টের ব্যালান্স এখনই বদলাবে"
                    else "আগের হিসাব — শুধু বাকি হিসেবে যোগ হবে",
                    checked = state.moneyMovedNow,
                    onCheckedChange = { checked -> viewModel.update { it.copy(moneyMovedNow = checked) } }
                )
                if (state.moneyMovedNow) {
                    Spacer(Modifier.height(Space.md))
                    SelectionField(
                        label = if (isLend) "যে অ্যাকাউন্ট থেকে দিলেন" else "যে অ্যাকাউন্টে টাকা এল",
                        selectedText = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "বেছে নিন",
                        options = state.accounts,
                        optionLabel = { it.name },
                        onSelect = { selected -> viewModel.update { it.copy(accountId = selected.id) } }
                    )
                }
                Spacer(Modifier.height(Space.md))
                DateField(
                    date = state.date,
                    onDateChange = { v -> viewModel.update { it.copy(date = v) } },
                    label = "তারিখ"
                )
            }

            Spacer(Modifier.height(Space.md))
            HgCard(padding = PaddingValues(Space.lg)) {
                SwitchRow(
                    title = "ফেরতের মেয়াদ আছে",
                    subtitle = "মেয়াদ শেষ হলে মনে করিয়ে দেওয়া হবে",
                    checked = state.hasDueDate,
                    onCheckedChange = { checked -> viewModel.update { it.copy(hasDueDate = checked) } }
                )
                if (state.hasDueDate) {
                    Spacer(Modifier.height(Space.md))
                    DateField(
                        date = state.dueDate,
                        onDateChange = { v -> viewModel.update { it.copy(dueDate = v) } },
                        label = "ফেরতের তারিখ"
                    )
                }
                Spacer(Modifier.height(Space.md))
                HgTextField(
                    value = state.interestRateText,
                    onValueChange = { v -> viewModel.update { it.copy(interestRateText = v) } },
                    label = "বার্ষিক সুদের হার % (যদি থাকে)",
                    placeholder = "না থাকলে খালি রাখুন",
                    keyboardType = KeyboardType.Decimal
                )
            }

            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.purpose,
                onValueChange = { v -> viewModel.update { it.copy(purpose = v) } },
                label = "উদ্দেশ্য (ঐচ্ছিক)"
            )
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
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Hg.colors.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary)
        }
        Spacer(Modifier.width(Space.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

@Composable
fun AgreementDetailScreen(
    loanId: Long,
    onBack: () -> Unit,
    onRecordPayment: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: AgreementDetailViewModel = viewModel(
        key = "agreement_detail_$loanId",
        factory = hisabViewModelFactory {
            AgreementDetailViewModel(
                loanId, container.loanRepository,
                container.transactionRepository, container.accountRepository
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val loan = state.loan
    var showMoreDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showCloseConfirm by remember { mutableStateOf(false) }

    HgScreen(
        title = loan?.organizationName ?: "চুক্তি",
        subtitle = loan?.kind?.bnLabel(),
        onBack = onBack
    ) { padding ->
        if (state.loading || loan == null) {
            LoadingState(Modifier.padding(padding))
            return@HgScreen
        }
        val isLend = loan.kind == LoanKind.PERSONAL_LEND
        val overdue = loan.dueDate != null && loan.dueDate < Dates.startOfDayMs(Dates.nowMs()) &&
            state.outstandingPoisha > 0 && loan.status == LoanStatus.ACTIVE
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = Space.xxl)
        ) {
            item {
                Column(Modifier.padding(horizontal = ScreenPadding, vertical = Space.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow(if (isLend) "পাওনা বাকি" else "দেনা বাকি")
                        Spacer(Modifier.width(Space.sm))
                        HgBadge(
                            text = if (loan.status == LoanStatus.ACTIVE) "চলমান" else "পরিশোধিত",
                            tone = if (loan.status == LoanStatus.ACTIVE) HgTone.Brand else HgTone.Neutral
                        )
                    }
                    Spacer(Modifier.height(Space.xs))
                    AmountText(
                        state.outstandingPoisha,
                        style = HgAmountType.hero,
                        color = if (isLend) Hg.colors.income else Hg.colors.expense
                    )
                    if (overdue) {
                        Spacer(Modifier.height(Space.md))
                        HgNotice(
                            title = "ফেরতের মেয়াদ পেরিয়ে গেছে",
                            message = loan.dueDate?.let { Dates.formatShortDate(it) },
                            tone = HgTone.Warning
                        )
                    }
                    Spacer(Modifier.height(Space.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        HgButton(
                            text = if (isLend) "ফেরত পেলাম" else "পরিশোধ করুন",
                            onClick = { onRecordPayment(loanId) },
                            modifier = Modifier.weight(1f)
                        )
                        HgButton(
                            text = if (isLend) "আরও ধার" else "আরও ধার",
                            onClick = { showMoreDialog = true },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm),
                    padding = PaddingValues(vertical = Space.xs)
                ) {
                    TermLine("মোট আসল", formatAmount(loan.principalPoisha))
                    TermLine("মোট ফেরত/পরিশোধ", formatAmount(state.totalRepaidPoisha))
                    TermLine("শুরুর তারিখ", Dates.formatShortDate(loan.startDate))
                    loan.dueDate?.let { TermLine("ফেরতের মেয়াদ", Dates.formatShortDate(it)) }
                    if (loan.interestRateBp > 0) {
                        TermLine("সুদের হার", BengaliNumber.formatPercentBp(loan.interestRateBp) + " বার্ষিক")
                        val days = ChronoUnit.DAYS.between(
                            Dates.toLocalDate(loan.startDate), Dates.today()
                        ).coerceAtLeast(0)
                        val estimate = LoanMath.simpleInterestPoisha(
                            state.outstandingPoisha, loan.interestRateBp, days
                        )
                        TermLine("আনুমানিক সুদ", formatAmount(estimate))
                    }
                    loan.purpose?.let { TermLine("উদ্দেশ্য", it) }
                    loan.note?.takeIf { it.isNotBlank() }?.let { TermLine("মন্তব্য", it) }
                    TermLine("অবস্থা", if (loan.status == LoanStatus.ACTIVE) "চলমান" else "পরিশোধিত/বন্ধ")
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPadding, vertical = Space.md),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    HgButton(
                        text = "শর্ত বদলান",
                        onClick = { showTermsDialog = true },
                        style = HgButtonStyle.Ghost,
                        modifier = Modifier.weight(1f)
                    )
                    HgButton(
                        text = if (loan.status == LoanStatus.ACTIVE) "চুক্তি বন্ধ করুন" else "আবার চালু করুন",
                        onClick = { showCloseConfirm = true },
                        style = HgButtonStyle.Ghost,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    "লেনদেনের ইতিহাস",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
                )
            }
            if (state.transactions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Handshake,
                        title = "কোনো লেনদেন নেই",
                        subtitle = "ফেরত বা পরিশোধ করলে এখানে দেখা যাবে।"
                    )
                }
            }
            items(state.transactions, key = { it.id }) { txn ->
                Column(Modifier.padding(horizontal = ScreenPadding)) {
                    TransactionRow(txn, onClick = { onOpenTransaction(txn.id) })
                    HgDivider(startInset = 52)
                }
            }
        }
    }

    if (showMoreDialog) {
        var amountText by remember { mutableStateOf("") }
        var accountId by remember { mutableStateOf(state.accounts.firstOrNull()?.id) }
        var date by remember { mutableStateOf(Dates.today()) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showMoreDialog = false },
            containerColor = Hg.colors.surface,
            shape = Radius.panel,
            title = { Text(if (loan?.kind == LoanKind.PERSONAL_LEND) "আরও ধার দিন" else "আরও ধার নিন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    MoneyInputField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = "পরিমাণ (টাকা)",
                        error = error
                    )
                    SelectionField(
                        label = "অ্যাকাউন্ট",
                        selectedText = state.accounts.firstOrNull { it.id == accountId }?.name ?: "বেছে নিন",
                        options = state.accounts,
                        optionLabel = { it.name },
                        onSelect = { accountId = it.id }
                    )
                    DateField(date = date, onDateChange = { date = it }, label = "তারিখ")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = BengaliNumber.parsePoisha(amountText)
                    val account = accountId
                    if (amount == null || amount <= 0 || account == null) {
                        error = "পরিমাণ ও অ্যাকাউন্ট ঠিক করুন"
                    } else {
                        viewModel.addMorePrincipal(account, amount, date, null)
                        showMoreDialog = false
                    }
                }) { Text("যোগ করুন", color = Hg.colors.brand) }
            },
            dismissButton = {
                TextButton(onClick = { showMoreDialog = false }) {
                    Text("বাতিল", color = Hg.colors.inkSecondary)
                }
            }
        )
    }

    if (showTermsDialog && loan != null) {
        var hasDue by remember { mutableStateOf(loan.dueDate != null) }
        var due by remember {
            mutableStateOf(loan.dueDate?.let { Dates.toLocalDate(it) } ?: Dates.today().plusMonths(1))
        }
        var rateText by remember {
            mutableStateOf(
                if (loan.interestRateBp > 0)
                    BengaliNumber.toAsciiDigits(BengaliNumber.formatPercentBp(loan.interestRateBp)).removeSuffix("%")
                else ""
            )
        }
        var purpose by remember { mutableStateOf(loan.purpose ?: "") }
        var note by remember { mutableStateOf(loan.note ?: "") }
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = Hg.colors.surface,
            shape = Radius.panel,
            title = { Text("চুক্তির শর্ত বদলান") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    SwitchRow(
                        title = "ফেরতের মেয়াদ আছে",
                        subtitle = "মেয়াদ শেষ হলে মনে করানো হবে",
                        checked = hasDue,
                        onCheckedChange = { hasDue = it }
                    )
                    if (hasDue) {
                        DateField(date = due, onDateChange = { due = it }, label = "ফেরতের তারিখ")
                    }
                    HgTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = "বার্ষিক সুদের হার % (না থাকলে খালি)",
                        keyboardType = KeyboardType.Decimal
                    )
                    HgTextField(value = purpose, onValueChange = { purpose = it }, label = "উদ্দেশ্য")
                    HgTextField(value = note, onValueChange = { note = it }, label = "মন্তব্য")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val bp = if (rateText.isBlank()) 0
                    else BengaliNumber.parsePercentToBp(rateText) ?: 0
                    viewModel.updateTerms(
                        dueDateMs = if (hasDue) Dates.startOfDayMs(due) else null,
                        purpose = purpose,
                        note = note,
                        rateBp = bp
                    )
                    showTermsDialog = false
                }) { Text("সংরক্ষণ করুন", color = Hg.colors.brand) }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("বাতিল", color = Hg.colors.inkSecondary)
                }
            }
        )
    }

    if (showCloseConfirm && loan != null) {
        val closing = loan.status == LoanStatus.ACTIVE
        ConfirmDialog(
            title = if (closing) "চুক্তি বন্ধ করবেন?" else "চুক্তি আবার চালু করবেন?",
            message = if (closing)
                "বন্ধ করলে চুক্তিটি আর চলমান তালিকায় থাকবে না। বাকি থাকলে হিসাবের অঙ্ক বদলাবে না — শুধু অবস্থা বদলাবে।"
            else "চুক্তিটি আবার চলমান হিসেবে দেখানো হবে।",
            confirmText = if (closing) "বন্ধ করুন" else "চালু করুন",
            onConfirm = {
                viewModel.setStatus(if (closing) LoanStatus.CLOSED else LoanStatus.ACTIVE)
                showCloseConfirm = false
            },
            onDismiss = { showCloseConfirm = false }
        )
    }
}

@Composable
private fun TermLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.sm)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

// ---------------- agreement detail ----------------

data class AgreementDetailUiState(
    val loading: Boolean = true,
    val loan: LoanEntity? = null,
    val outstandingPoisha: Long = 0,
    val totalRepaidPoisha: Long = 0,
    val transactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AgreementDetailViewModel(
    private val loanId: Long,
    private val loans: LoanRepository,
    transactions: TransactionRepository,
    accounts: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<AgreementDetailUiState> = combine(
        loans.observeLoan(loanId),
        transactions.observeFiltered(loanId = loanId),
        accounts.observeAccountsWithBalances()
    ) { loan, txns, accountList ->
        Triple(loan, txns, accountList.map { it.account })
    }.flatMapLatest { (loan, txns, accountList) ->
        if (loan == null) {
            kotlinx.coroutines.flow.flowOf(AgreementDetailUiState(loading = false))
        } else {
            combine(
                loans.observeOutstanding(loan),
                loans.observeTotalCashRepaid(loanId, loan.kind)
            ) { outstanding, repaid ->
                AgreementDetailUiState(
                    loading = false,
                    loan = loan,
                    outstandingPoisha = outstanding,
                    totalRepaidPoisha = repaid,
                    transactions = txns,
                    accounts = accountList
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgreementDetailUiState())

    fun addMorePrincipal(accountId: Long, amountPoisha: Long, date: LocalDate, note: String?) {
        viewModelScope.launch {
            runCatching {
                loans.recordAdditionalPrincipal(
                    loanId, accountId, amountPoisha,
                    Dates.startOfDayMs(date) + 12 * 3600_000L, note
                )
            }
        }
    }

    fun setStatus(status: LoanStatus) {
        viewModelScope.launch { loans.setLoanStatus(loanId, status) }
    }

    fun updateTerms(dueDateMs: Long?, purpose: String?, note: String?, rateBp: Int) {
        val loan = uiState.value.loan ?: return
        viewModelScope.launch {
            loans.updateLoan(
                loan.copy(
                    dueDate = dueDateMs,
                    purpose = purpose?.ifBlank { null },
                    note = note?.ifBlank { null },
                    interestRateBp = rateBp
                )
            )
        }
    }
}

