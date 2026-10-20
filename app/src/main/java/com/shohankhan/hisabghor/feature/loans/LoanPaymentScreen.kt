package com.shohankhan.hisabghor.feature.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.LoanEntity
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.LoanRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.layout.PaddingValues
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountInput
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgFormBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

data class LoanPaymentUiState(
    val loading: Boolean = true,
    val loan: LoanEntity? = null,
    val outstandingPoisha: Long = 0,
    val nextDueText: String? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val accountId: Long? = null,
    val totalText: String = "",
    val feeText: String = "",
    val manualSplit: Boolean = false,
    val manualPrincipalText: String = "",
    val manualInterestText: String = "",
    val lendInterestText: String = "",
    val interestCategoryId: Long? = null,
    val feeCategoryId: Long? = null,
    val date: LocalDate = Dates.today(),
    val receiptNo: String = "",
    val note: String = "",
    val error: String? = null,
    val saved: Boolean = false
)

class LoanPaymentViewModel(
    private val loanId: Long,
    private val editId: Long,
    private val db: HisabGhorDatabase,
    private val loans: LoanRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoanPaymentUiState())
    val state: StateFlow<LoanPaymentUiState> = _state

    init {
        viewModelScope.launch {
            val loan = loans.getLoan(loanId)
            val accountList = accounts.observeAccountsWithBalances().first().map { it.account }
            val expenseCats = categories.observeByKind(CategoryKind.EXPENSE).first()
            val incomeCats = categories.observeByKind(CategoryKind.INCOME).first()
            var nextDueText: String? = null
            if (loan != null) {
                val installments = loans.observeInstallments(loanId).first()
                val next = installments.firstOrNull { it.remainingPoisha > 0 }
                if (next != null) {
                    nextDueText = "পরের কিস্তি: ${Dates.formatShortDate(next.dueDate)} — বাকি " +
                        formatAmount(next.remainingPoisha)
                }
            }
            var initial = LoanPaymentUiState(
                loading = false,
                loan = loan,
                nextDueText = nextDueText,
                accounts = accountList,
                expenseCategories = expenseCats,
                incomeCategories = incomeCats,
                accountId = accountList.firstOrNull()?.id
            )
            if (loan != null && editId > 0) {
                val txn = db.transactionDao().getById(editId)
                if (txn != null) {
                    val postings = db.postingDao().getPostingsForTransaction(editId)
                    initial = if (loan.kind == LoanKind.PERSONAL_LEND) {
                        val interest = -postings
                            .filter { it.ledgerType == LedgerType.INCOME_CATEGORY }
                            .sumOf { it.signedAmountPoisha }
                        initial.copy(
                            accountId = txn.accountId ?: initial.accountId,
                            totalText = poishaToInputText(txn.amountPoisha - interest),
                            lendInterestText = if (interest > 0)
                                poishaToInputText(interest) else "",
                            date = Dates.toLocalDate(txn.occurredAt),
                            note = txn.note ?: ""
                        )
                    } else {
                        val allocations = db.transactionDao().getAllocations(editId)
                        val scheduleFee = allocations.sumOf { it.feePaidPoisha }
                        val principalPaid = postings
                            .filter { it.ledgerType == LedgerType.LOAN_LIABILITY }
                            .sumOf { it.signedAmountPoisha }
                        val extraFee = (txn.feePoisha - scheduleFee).coerceAtLeast(0)
                        val interest = txn.amountPoisha - principalPaid - txn.feePoisha
                        initial.copy(
                            accountId = txn.accountId ?: initial.accountId,
                            totalText = poishaToInputText(txn.amountPoisha),
                            feeText = if (extraFee > 0)
                                poishaToInputText(extraFee) else "",
                            manualSplit = false,
                            manualPrincipalText = poishaToInputText(principalPaid),
                            manualInterestText = if (interest > 0)
                                poishaToInputText(interest) else "",
                            date = Dates.toLocalDate(txn.occurredAt),
                            receiptNo = txn.receiptNo ?: "",
                            note = txn.note ?: ""
                        )
                    }
                }
            }
            _state.value = initial
            if (loan != null) {
                launch {
                    loans.observeOutstanding(loan).collect { outstanding ->
                        _state.update { it.copy(outstandingPoisha = outstanding) }
                    }
                }
            }
        }
    }

    fun update(change: (LoanPaymentUiState) -> LoanPaymentUiState) = _state.update(change)

    fun save() {
        val s = _state.value
        val loan = s.loan ?: return
        val accountId = s.accountId
        val occurredAt = Dates.startOfDayMs(s.date) + 12 * 3600_000L
        val replaces = if (editId > 0) editId else null
        if (accountId == null) {
            _state.update { it.copy(error = "অ্যাকাউন্ট বেছে নিন") }
            return
        }
        viewModelScope.launch {
            try {
                if (loan.kind == LoanKind.PERSONAL_LEND) {
                    val principal = BengaliNumber.parsePoisha(s.totalText)
                    if (principal == null || principal <= 0) {
                        _state.update { it.copy(error = "ফেরত পাওয়া আসলের পরিমাণ দিন") }
                        return@launch
                    }
                    val interest = if (s.lendInterestText.isBlank()) 0L
                    else BengaliNumber.parsePoisha(s.lendInterestText) ?: run {
                        _state.update { it.copy(error = "সুদের পরিমাণ সঠিক নয়") }
                        return@launch
                    }
                    loans.recordLendReturn(
                        loanId = loanId,
                        accountId = accountId,
                        principalPoisha = principal,
                        interestPoisha = interest,
                        interestCategoryId = s.interestCategoryId,
                        occurredAt = occurredAt,
                        note = s.note.ifBlank { null },
                        replacesTransactionId = replaces
                    )
                } else {
                    val total = BengaliNumber.parsePoisha(s.totalText)
                    if (total == null || total <= 0) {
                        _state.update { it.copy(error = "মোট পরিশোধের পরিমাণ দিন") }
                        return@launch
                    }
                    val fee = if (s.feeText.isBlank()) 0L
                    else BengaliNumber.parsePoisha(s.feeText) ?: run {
                        _state.update { it.copy(error = "চার্জের পরিমাণ সঠিক নয়") }
                        return@launch
                    }
                    if (fee >= total) {
                        _state.update { it.copy(error = "চার্জ মোট পরিমাণের চেয়ে কম হতে হবে") }
                        return@launch
                    }
                    var manualPrincipal: Long? = null
                    var manualInterest: Long? = null
                    if (s.manualSplit) {
                        val p = BengaliNumber.parsePoisha(s.manualPrincipalText)
                        val i = if (s.manualInterestText.isBlank()) 0L
                        else BengaliNumber.parsePoisha(s.manualInterestText)
                        if (p == null || i == null || p < 0 || i < 0) {
                            _state.update { it.copy(error = "আসল/সুদের ভাগ সঠিক নয়") }
                            return@launch
                        }
                        if (p + i + fee != total) {
                            _state.update {
                                it.copy(error = "আসল + সুদ + চার্জ মিলিয়ে মোট পরিমাণের সমান হতে হবে")
                            }
                            return@launch
                        }
                        manualPrincipal = p
                        manualInterest = i
                    }
                    loans.recordRepayment(
                        loanId = loanId,
                        accountId = accountId,
                        totalAmountPoisha = total,
                        occurredAt = occurredAt,
                        interestCategoryId = s.interestCategoryId,
                        feePoisha = fee,
                        feeCategoryId = s.feeCategoryId,
                        receiptNo = s.receiptNo.ifBlank { null },
                        note = s.note.ifBlank { null },
                        manualPrincipalPoisha = manualPrincipal,
                        manualInterestPoisha = manualInterest,
                        replacesTransactionId = replaces
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
fun LoanPaymentScreen(
    loanId: Long,
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: LoanPaymentViewModel = viewModel(
        key = "loan_payment_${loanId}_$editId",
        factory = hisabViewModelFactory {
            LoanPaymentViewModel(
                loanId, editId, container.database, container.loanRepository,
                container.accountRepository, container.categoryRepository
            )
        }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    val loan = state.loan
    val isLend = loan?.kind == LoanKind.PERSONAL_LEND

    HgScreen(
        title = when {
            isLend -> "ফেরত পেলাম"
            editId > 0 -> "পরিশোধ সম্পাদনা"
            else -> "কিস্তি পরিশোধ"
        },
        subtitle = loan?.organizationName,
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
            HgCard(padding = PaddingValues(Space.lg)) {
                Eyebrow(if (isLend) "পাওনা বাকি" else "আসল বাকি")
                Spacer(Modifier.height(Space.xs))
                AmountText(
                    state.outstandingPoisha,
                    style = HgAmountType.large,
                    color = if (isLend) Hg.colors.income else Hg.colors.expense
                )
                state.nextDueText?.let {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
            }

            Spacer(Modifier.height(Space.lg))
            if (isLend) {
                HgNotice(
                    title = "আসল ফেরত আয় নয়",
                    message = "শুধু সুদ/লাভের অংশটুকু আয় হিসেবে গণনা হবে।",
                    tone = HgTone.Neutral
                )
                Spacer(Modifier.height(Space.md))
                HgAmountInput(
                    value = state.totalText,
                    onValueChange = { v -> viewModel.update { it.copy(totalText = v) } },
                    error = state.error
                )
                Spacer(Modifier.height(Space.md))
                MoneyInputField(
                    value = state.lendInterestText,
                    onValueChange = { v -> viewModel.update { it.copy(lendInterestText = v) } },
                    label = "সুদ/লাভ পেলে লিখুন (ঐচ্ছিক)"
                )
                if (state.lendInterestText.isNotBlank()) {
                    Spacer(Modifier.height(Space.md))
                    SelectionField(
                        label = "সুদের আয় ক্যাটাগরি",
                        selectedText = state.incomeCategories
                            .firstOrNull { it.id == state.interestCategoryId }?.name
                            ?: "স্বয়ংক্রিয় (সঞ্চয়ের লাভ)",
                        options = state.incomeCategories,
                        optionLabel = { it.name },
                        onSelect = { selected -> viewModel.update { it.copy(interestCategoryId = selected.id) } }
                    )
                }
                Spacer(Modifier.height(Space.md))
                SelectionField(
                    label = "যে অ্যাকাউন্টে টাকা এল",
                    selectedText = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "বেছে নিন",
                    options = state.accounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(accountId = selected.id) } }
                )
            } else {
                HgAmountInput(
                    value = state.totalText,
                    onValueChange = { v -> viewModel.update { it.copy(totalText = v) } },
                    error = state.error
                )
                Spacer(Modifier.height(Space.md))
                SelectionField(
                    label = "যে অ্যাকাউন্ট থেকে পরিশোধ করলেন",
                    selectedText = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "বেছে নিন",
                    options = state.accounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(accountId = selected.id) } }
                )
                Spacer(Modifier.height(Space.md))
                MoneyInputField(
                    value = state.feeText,
                    onValueChange = { v -> viewModel.update { it.copy(feeText = v) } },
                    label = "এর মধ্যে চার্জ/জরিমানা থাকলে (ঐচ্ছিক)"
                )
                Spacer(Modifier.height(Space.md))
                HgCard(padding = PaddingValues(Space.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "আসল/সুদের ভাগ নিজে দেব",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Hg.colors.ink
                            )
                            Text(
                                if (state.manualSplit) "নিচে আসল ও সুদ আলাদা করে দিন"
                                else "কিস্তির তালিকা অনুযায়ী স্বয়ংক্রিয় ভাগ হবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = Hg.colors.inkSecondary
                            )
                        }
                        Spacer(Modifier.width(Space.md))
                        Switch(
                            checked = state.manualSplit,
                            onCheckedChange = { checked -> viewModel.update { it.copy(manualSplit = checked) } },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                checkedTrackColor = Hg.colors.brand,
                                uncheckedThumbColor = Hg.colors.surface,
                                uncheckedTrackColor = Hg.colors.surfaceSunken,
                                uncheckedBorderColor = Hg.colors.hairlineStrong
                            )
                        )
                    }
                    if (state.manualSplit) {
                        Spacer(Modifier.height(Space.md))
                        MoneyInputField(
                            value = state.manualPrincipalText,
                            onValueChange = { v -> viewModel.update { it.copy(manualPrincipalText = v) } },
                            label = "আসল অংশ (টাকা)"
                        )
                        Spacer(Modifier.height(Space.md))
                        MoneyInputField(
                            value = state.manualInterestText,
                            onValueChange = { v -> viewModel.update { it.copy(manualInterestText = v) } },
                            label = "সুদ অংশ (টাকা)"
                        )
                    }
                }
                Spacer(Modifier.height(Space.md))
                SelectionField(
                    label = "সুদ/চার্জের খরচ ক্যাটাগরি",
                    selectedText = state.expenseCategories
                        .firstOrNull { it.id == state.interestCategoryId }?.name
                        ?: "স্বয়ংক্রিয় (ঋণের সুদ/চার্জ)",
                    options = state.expenseCategories,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(interestCategoryId = selected.id) } }
                )
                Spacer(Modifier.height(Space.md))
                HgTextField(
                    value = state.receiptNo,
                    onValueChange = { v -> viewModel.update { it.copy(receiptNo = v) } },
                    label = "রসিদ/ভাউচার নম্বর (ঐচ্ছিক)"
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
            state.error?.let {
                Spacer(Modifier.height(Space.md))
                HgNotice(title = it, tone = HgTone.Expense)
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
