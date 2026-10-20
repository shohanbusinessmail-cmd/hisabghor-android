package com.shohankhan.hisabghor.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransferUiState(
    val loading: Boolean = true,
    val accounts: List<AccountEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    val amountText: String = "",
    val feeText: String = "",
    val feeCategoryId: Long? = null,
    val date: LocalDate = Dates.today(),
    val note: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false
)

class TransferViewModel(
    private val editId: Long,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val transactions: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _state

    init {
        viewModelScope.launch {
            val accountList = accountRepo.observeAccountsWithBalances().first().map { it.account }
            val expenseCats = categoryRepo.observeByKind(CategoryKind.EXPENSE).first()
            _state.update {
                it.copy(
                    loading = false,
                    accounts = accountList,
                    expenseCategories = expenseCats,
                    fromAccountId = accountList.getOrNull(0)?.id,
                    toAccountId = accountList.getOrNull(1)?.id,
                    feeCategoryId = expenseCats.firstOrNull { c ->
                        c.name == CategoryRepository.NAME_TRANSFER_FEE
                    }?.id
                )
            }
            if (editId > 0) {
                transactions.getById(editId)?.let { txn ->
                    _state.update {
                        it.copy(
                            fromAccountId = txn.accountId,
                            toAccountId = txn.counterAccountId,
                            amountText = poishaToInputText(txn.amountPoisha),
                            feeText = if (txn.feePoisha > 0) poishaToInputText(txn.feePoisha) else "",
                            date = Dates.toLocalDate(txn.occurredAt),
                            note = txn.note ?: ""
                        )
                    }
                }
            }
        }
    }

    fun setFrom(id: Long) = _state.update { it.copy(fromAccountId = id) }
    fun setTo(id: Long) = _state.update { it.copy(toAccountId = id) }
    fun setAmount(v: String) = _state.update { it.copy(amountText = v) }
    fun setFee(v: String) = _state.update { it.copy(feeText = v) }
    fun setFeeCategory(id: Long) = _state.update { it.copy(feeCategoryId = id) }
    fun setDate(v: LocalDate) = _state.update { it.copy(date = v) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, String>()
        val amount = BengaliNumber.parsePoisha(s.amountText)
        if (amount == null || amount <= 0) errors["amount"] = "সঠিক পরিমাণ দিন"
        val fee = BengaliNumber.parsePoisha(s.feeText.ifBlank { "0" })
        if (fee == null || fee < 0) errors["fee"] = "ফি সঠিক নয়"
        if (s.fromAccountId == null) errors["from"] = "অ্যাকাউন্ট বেছে নিন"
        if (s.toAccountId == null) errors["to"] = "অ্যাকাউন্ট বেছে নিন"
        if (s.fromAccountId != null && s.fromAccountId == s.toAccountId) {
            errors["to"] = "একই অ্যাকাউন্টে ট্রান্সফার করা যায় না"
        }
        if (fee != null && fee > 0 && s.feeCategoryId == null) {
            errors["feeCategory"] = "ফির ক্যাটাগরি বেছে নিন"
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        val occurredAt = if (s.date == Dates.today()) Dates.nowMs()
        else Dates.startOfDayMs(s.date) + 12 * 3600_000L
        _state.update { it.copy(saving = true, errors = emptyMap()) }
        viewModelScope.launch {
            try {
                accountRepo.transfer(
                    fromAccountId = s.fromAccountId!!,
                    toAccountId = s.toAccountId!!,
                    amountPoisha = amount!!,
                    feePoisha = fee ?: 0L,
                    feeCategoryId = s.feeCategoryId,
                    occurredAt = occurredAt,
                    note = s.note,
                    replacesTransactionId = if (editId > 0) editId else null
                )
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, errors = mapOf("save" to "সংরক্ষণ করা যায়নি — আবার চেষ্টা করুন"))
                }
            }
        }
    }
}

@Composable
fun TransferScreen(
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: TransferViewModel = viewModel(
        key = "transfer_$editId",
        factory = hisabViewModelFactory {
            TransferViewModel(
                editId, container.accountRepository,
                container.categoryRepository, container.transactionRepository
            )
        }
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (editId > 0) "ট্রান্সফার সম্পাদনা" else "অ্যাকাউন্ট ট্রান্সফার",
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
                "নিজের এক অ্যাকাউন্ট থেকে আরেক অ্যাকাউন্টে টাকা সরানো আয় বা খরচ নয় — শুধু ফি থাকলে তা খরচ হিসেবে ধরা হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SelectionField(
                label = "যেখান থেকে (From)",
                selectedText = state.accounts.firstOrNull { it.id == state.fromAccountId }?.name ?: "বেছে নিন",
                options = state.accounts,
                optionLabel = { it.name },
                onSelect = { viewModel.setFrom(it.id) },
                error = state.errors["from"]
            )
            SelectionField(
                label = "যেখানে যাবে (To)",
                selectedText = state.accounts.firstOrNull { it.id == state.toAccountId }?.name ?: "বেছে নিন",
                options = state.accounts,
                optionLabel = { it.name },
                onSelect = { viewModel.setTo(it.id) },
                error = state.errors["to"]
            )
            MoneyInputField(
                value = state.amountText,
                onValueChange = viewModel::setAmount,
                label = "পরিমাণ (টাকা)",
                error = state.errors["amount"]
            )
            MoneyInputField(
                value = state.feeText,
                onValueChange = viewModel::setFee,
                label = "ফি/চার্জ (যদি থাকে)",
                error = state.errors["fee"]
            )
            if ((BengaliNumber.parsePoisha(state.feeText) ?: 0L) > 0) {
                SelectionField(
                    label = "ফির ক্যাটাগরি",
                    selectedText = state.expenseCategories.firstOrNull { it.id == state.feeCategoryId }?.name ?: "বেছে নিন",
                    options = state.expenseCategories,
                    optionLabel = { it.name },
                    onSelect = { viewModel.setFeeCategory(it.id) },
                    error = state.errors["feeCategory"]
                )
            }
            DateField(date = state.date, onDateChange = viewModel::setDate, label = "তারিখ")
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("মন্তব্য (ঐচ্ছিক)") },
                modifier = Modifier.fillMaxWidth()
            )
            state.errors["save"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.saving) "হচ্ছে…" else "ট্রান্সফার করুন")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
