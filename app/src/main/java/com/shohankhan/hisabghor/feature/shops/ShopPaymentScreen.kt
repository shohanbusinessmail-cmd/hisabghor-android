package com.shohankhan.hisabghor.feature.shops

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.LedgerRepository
import com.shohankhan.hisabghor.data.ShopRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountInput
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgFormBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

data class ShopPaymentUiState(
    val loading: Boolean = true,
    val shopName: String = "",
    val currentDuePoisha: Long = 0,
    val amountText: String = "",
    val accountId: Long? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val date: LocalDate = Dates.today(),
    val note: String = "",
    val error: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false
)

class ShopPaymentViewModel(
    private val shopId: Long,
    private val editId: Long,
    private val shops: ShopRepository,
    private val accounts: AccountRepository,
    private val ledger: LedgerRepository,
    private val transactions: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ShopPaymentUiState())
    val state: StateFlow<ShopPaymentUiState> = _state

    init {
        viewModelScope.launch {
            val shop = shops.getShop(shopId)
            val accountList = accounts.observeAccountsWithBalances().first().map { it.account }
            var due = ledger.shopDue(shopId).first()
            _state.update {
                it.copy(
                    loading = false,
                    shopName = shop?.name ?: "",
                    accounts = accountList,
                    accountId = accountList.firstOrNull()?.id
                )
            }
            if (editId > 0) {
                transactions.getById(editId)?.let { txn ->
                    // When editing, the due shown should include this payment back.
                    due += txn.amountPoisha
                    _state.update {
                        it.copy(
                            amountText = poishaToInputText(txn.amountPoisha),
                            accountId = txn.accountId ?: it.accountId,
                            date = Dates.toLocalDate(txn.occurredAt),
                            note = txn.note ?: ""
                        )
                    }
                }
            }
            _state.update { it.copy(currentDuePoisha = due) }
        }
    }

    fun update(change: (ShopPaymentUiState) -> ShopPaymentUiState) = _state.update(change)

    fun fillFullDue() {
        val due = _state.value.currentDuePoisha
        if (due > 0) _state.update { it.copy(amountText = poishaToInputText(due)) }
    }

    fun save() {
        val s = _state.value
        val amount = BengaliNumber.parsePoisha(s.amountText)
        if (amount == null || amount <= 0) {
            _state.update { it.copy(error = "সঠিক পরিমাণ দিন") }
            return
        }
        val accountId = s.accountId
        if (accountId == null) {
            _state.update { it.copy(error = "কোন অ্যাকাউন্ট থেকে দিলেন বেছে নিন") }
            return
        }
        val occurredAt = if (s.date == Dates.today()) Dates.nowMs()
        else Dates.startOfDayMs(s.date) + 12 * 3600_000L
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                shops.recordPayment(
                    shopId, accountId, amount, occurredAt, s.note,
                    replacesTransactionId = if (editId > 0) editId else null
                )
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "সংরক্ষণ করা যায়নি — আবার চেষ্টা করুন") }
            }
        }
    }
}

@Composable
fun ShopPaymentScreen(
    shopId: Long,
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: ShopPaymentViewModel = viewModel(
        key = "shop_payment_${shopId}_$editId",
        factory = hisabViewModelFactory {
            ShopPaymentViewModel(
                shopId, editId, container.shopRepository,
                container.accountRepository, container.ledgerRepository,
                container.transactionRepository
            )
        }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    HgScreen(
        title = if (editId > 0) "পরিশোধ সম্পাদনা" else "বকেয়া পরিশোধ",
        subtitle = state.shopName.ifBlank { null },
        onBack = onBack,
        bottomBar = {
            HgFormBottomBar {
                HgButton(
                    text = if (state.saving) "হচ্ছে…" else "পরিশোধ করুন",
                    onClick = viewModel::save,
                    enabled = !state.saving,
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
            HgCard(padding = androidx.compose.foundation.layout.PaddingValues(Space.lg)) {
                Eyebrow("বর্তমান বকেয়া")
                Spacer(Modifier.height(Space.xs))
                Text(
                    formatAmount(state.currentDuePoisha),
                    style = HgAmountType.large,
                    color = if (state.currentDuePoisha > 0) Hg.colors.warning else Hg.colors.ink
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    "পরিশোধ করলে শুধু বকেয়া কমবে — নতুন করে খরচ ধরা হবে না।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary
                )
                if (state.currentDuePoisha > 0) {
                    Spacer(Modifier.height(Space.md))
                    HgButton(
                        text = "পুরো বকেয়া বসিয়ে দিন",
                        onClick = viewModel::fillFullDue,
                        style = HgButtonStyle.Secondary
                    )
                }
            }
            Spacer(Modifier.height(Space.lg))
            HgAmountInput(
                value = state.amountText,
                onValueChange = { v -> viewModel.update { it.copy(amountText = v) } },
                error = state.error
            )
            Spacer(Modifier.height(Space.lg))
            SelectionField(
                label = "যে অ্যাকাউন্ট থেকে দিলেন",
                selectedText = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "বেছে নিন",
                options = state.accounts,
                optionLabel = { it.name },
                onSelect = { selected -> viewModel.update { it.copy(accountId = selected.id) } }
            )
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
            Spacer(Modifier.height(Space.lg))
            HgNotice(
                title = "খরচ আগেই ধরা হয়েছে",
                message = "বাকি কেনার সময় পুরো টাকা খরচ হিসেবে গণনা হয়েছে। এই পরিশোধ শুধু দেনা কমায়।",
                tone = HgTone.Neutral
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
