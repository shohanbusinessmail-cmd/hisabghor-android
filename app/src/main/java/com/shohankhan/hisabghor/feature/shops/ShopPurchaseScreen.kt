package com.shohankhan.hisabghor.feature.shops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.ItemDraft
import com.shohankhan.hisabghor.data.ShopRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.ItemUiState
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountInput
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgFormBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

data class ShopPurchaseUiState(
    val loading: Boolean = true,
    val shopName: String = "",
    val title: String = "",
    val amountText: String = "",
    val paidNowText: String = "",
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val items: List<ItemUiState> = emptyList(),
    val extraChargeText: String = "",
    val discountText: String = "",
    val date: LocalDate = Dates.today(),
    val note: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false
) {
    val totalPoisha: Long?
        get() {
            if (items.isNotEmpty()) {
                var sum = 0L
                for (item in items) {
                    sum += item.toDraftOrNull()?.lineTotalPoisha ?: return null
                }
                val extra = BengaliNumber.parsePoisha(extraChargeText.ifBlank { "0" }) ?: return null
                val discount = BengaliNumber.parsePoisha(discountText.ifBlank { "0" }) ?: return null
                return (sum + extra - discount).coerceAtLeast(0)
            }
            return BengaliNumber.parsePoisha(amountText)
        }
    val paidNowPoisha: Long? get() = BengaliNumber.parsePoisha(paidNowText.ifBlank { "0" })
    val dueAfterPoisha: Long?
        get() {
            val total = totalPoisha ?: return null
            val paid = paidNowPoisha ?: return null
            return total - paid
        }
}

fun ItemUiState.toDraftOrNull(): ItemDraft? {
    if (name.isBlank()) return null
    val qty = BengaliNumber.parseQuantityMilli(quantity.ifBlank { "1" }) ?: return null
    val price = BengaliNumber.parsePoisha(unitPrice) ?: return null
    val discount = BengaliNumber.parsePoisha(discount.ifBlank { "0" }) ?: return null
    if (qty <= 0 || price < 0) return null
    return ItemDraft(name.trim(), qty, unit.trim().ifBlank { null }, price, discount)
}

class ShopPurchaseViewModel(
    private val shopId: Long,
    private val editId: Long,
    private val db: HisabGhorDatabase,
    private val shops: ShopRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ShopPurchaseUiState())
    val state: StateFlow<ShopPurchaseUiState> = _state
    private var nextItemKey = 1

    init {
        viewModelScope.launch {
            val shop = shops.getShop(shopId)
            val accountList = accounts.observeAccountsWithBalances().first().map { it.account }
            val expenseCats = categories.observeByKind(CategoryKind.EXPENSE).first()
            _state.update {
                it.copy(
                    loading = false,
                    shopName = shop?.name ?: "",
                    accounts = accountList,
                    categories = expenseCats,
                    accountId = accountList.firstOrNull()?.id
                )
            }
            if (editId > 0) loadForEdit()
        }
    }

    private suspend fun loadForEdit() {
        val txn = transactions.getById(editId) ?: return
        val postings = db.postingDao().getPostingsForTransaction(editId)
        val paidNow = -postings.filter { it.ledgerType == LedgerType.ACCOUNT }
            .sumOf { it.signedAmountPoisha }
        val items = transactions.getItems(editId).map { entity ->
            ItemUiState(
                key = nextItemKey++,
                name = entity.name,
                quantity = BengaliNumber.toAsciiDigits(BengaliNumber.formatQuantityMilli(entity.quantityMilli)),
                unit = entity.unit ?: "",
                unitPrice = poishaToInputText(entity.unitPricePoisha),
                discount = if (entity.discountPoisha > 0) poishaToInputText(entity.discountPoisha) else ""
            )
        }
        _state.update {
            it.copy(
                title = txn.title,
                amountText = poishaToInputText(txn.amountPoisha),
                paidNowText = if (paidNow > 0) poishaToInputText(paidNow) else "",
                accountId = txn.accountId ?: it.accountId,
                categoryId = txn.categoryId,
                items = items,
                extraChargeText = if (txn.extraChargePoisha > 0) poishaToInputText(txn.extraChargePoisha) else "",
                discountText = if (txn.discountPoisha > 0) poishaToInputText(txn.discountPoisha) else "",
                date = Dates.toLocalDate(txn.occurredAt),
                note = txn.note ?: ""
            )
        }
    }

    fun update(change: (ShopPurchaseUiState) -> ShopPurchaseUiState) = _state.update(change)
    fun addItem() = _state.update { it.copy(items = it.items + ItemUiState(key = nextItemKey++)) }
    fun removeItem(key: Int) = _state.update { s -> s.copy(items = s.items.filterNot { it.key == key }) }
    fun updateItem(key: Int, change: (ItemUiState) -> ItemUiState) =
        _state.update { s -> s.copy(items = s.items.map { if (it.key == key) change(it) else it }) }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, String>()
        val total = s.totalPoisha
        val paidNow = s.paidNowPoisha
        val categoryId = s.categoryId ?: run { errors["category"] = "খরচের ক্যাটাগরি বেছে নিন"; null }
        if (s.items.isNotEmpty() && s.items.any { it.toDraftOrNull() == null }) {
            errors["items"] = "প্রতিটি পণ্যের নাম, পরিমাণ ও দাম ঠিক করে দিন"
        }
        if (total == null || total <= 0) errors["amount"] = "মোট পরিমাণ সঠিক নয়"
        if (paidNow == null || paidNow < 0) errors["paidNow"] = "পরিশোধের পরিমাণ সঠিক নয়"
        if (total != null && paidNow != null && paidNow > total) {
            errors["paidNow"] = "এখন পরিশোধ মোটের বেশি হতে পারে না"
        }
        if (paidNow != null && paidNow > 0 && s.accountId == null) {
            errors["account"] = "কোন অ্যাকাউন্ট থেকে দিলেন বেছে নিন"
        }
        if (errors.isNotEmpty() || categoryId == null || total == null || paidNow == null) {
            _state.update { it.copy(errors = errors) }
            return
        }
        val occurredAt = if (s.date == Dates.today()) Dates.nowMs()
        else Dates.startOfDayMs(s.date) + 12 * 3600_000L
        _state.update { it.copy(saving = true, errors = emptyMap()) }
        viewModelScope.launch {
            try {
                shops.recordCreditPurchase(
                    shopId = shopId,
                    categoryId = categoryId,
                    amountPoisha = BengaliNumber.parsePoisha(s.amountText) ?: 0L,
                    items = s.items.mapNotNull { it.toDraftOrNull() },
                    extraChargePoisha = BengaliNumber.parsePoisha(s.extraChargeText.ifBlank { "0" }) ?: 0L,
                    discountPoisha = BengaliNumber.parsePoisha(s.discountText.ifBlank { "0" }) ?: 0L,
                    paidNowPoisha = paidNow,
                    accountId = if (paidNow > 0) s.accountId else null,
                    occurredAt = occurredAt,
                    title = s.title.trim(),
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
fun ShopPurchaseScreen(
    shopId: Long,
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: ShopPurchaseViewModel = viewModel(
        key = "shop_purchase_${shopId}_$editId",
        factory = hisabViewModelFactory {
            ShopPurchaseViewModel(
                shopId, editId, container.database, container.shopRepository,
                container.accountRepository, container.categoryRepository,
                container.transactionRepository
            )
        }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    HgScreen(
        title = if (editId > 0) "ক্রয় সম্পাদনা" else "নতুন বাকি ক্রয়",
        subtitle = state.shopName.ifBlank { null },
        onBack = onBack,
        bottomBar = {
            HgFormBottomBar {
                Column(Modifier.weight(1f)) {
                    Eyebrow("বকেয়া থাকবে")
                    Text(
                        state.dueAfterPoisha?.let { formatAmount(it) } ?: "৳০",
                        style = HgAmountType.medium,
                        color = Hg.colors.warning
                    )
                }
                HgButton(
                    text = if (state.saving) "সংরক্ষণ হচ্ছে…" else "সংরক্ষণ করুন",
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.weight(1.2f)
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
            if (state.items.isEmpty()) {
                HgAmountInput(
                    value = state.amountText,
                    onValueChange = { v -> viewModel.update { it.copy(amountText = v) } },
                    error = state.errors["amount"]
                )
            }
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.title,
                onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                label = "শিরোনাম (ঐচ্ছিক)",
                placeholder = "যেমন: সাপ্তাহিক বাজার"
            )
            Spacer(Modifier.height(Space.md))
            SelectionField(
                label = "খরচের ক্যাটাগরি",
                selectedText = state.categories.firstOrNull { it.id == state.categoryId }?.name ?: "বেছে নিন",
                options = state.categories,
                optionLabel = { it.name },
                onSelect = { selected -> viewModel.update { it.copy(categoryId = selected.id) } },
                error = state.errors["category"]
            )

            Spacer(Modifier.height(Space.lg))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "পণ্যের বিবরণ",
                        style = MaterialTheme.typography.titleSmall,
                        color = Hg.colors.ink
                    )
                    Text(
                        "ঐচ্ছিক — পণ্য অনুযায়ী ভাগ করে লিখতে পারেন",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
                TextButton(onClick = viewModel::addItem) {
                    Text("যোগ করুন", color = Hg.colors.brand)
                }
            }
            state.items.forEach { item ->
                Spacer(Modifier.height(Space.sm))
                HgPurchaseItem(item, viewModel)
            }
            state.errors["items"]?.let {
                Spacer(Modifier.height(Space.sm))
                Text(it, color = Hg.colors.expense, style = MaterialTheme.typography.bodySmall)
            }

            if (state.items.isNotEmpty()) {
                Spacer(Modifier.height(Space.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    MoneyInputField(
                        value = state.extraChargeText,
                        onValueChange = { v -> viewModel.update { it.copy(extraChargeText = v) } },
                        label = "অতিরিক্ত চার্জ",
                        modifier = Modifier.weight(1f)
                    )
                    MoneyInputField(
                        value = state.discountText,
                        onValueChange = { v -> viewModel.update { it.copy(discountText = v) } },
                        label = "মোট ছাড়",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(Space.lg))
            MoneyInputField(
                value = state.paidNowText,
                onValueChange = { v -> viewModel.update { it.copy(paidNowText = v) } },
                label = "এখন পরিশোধ করলেন (টাকা)",
                error = state.errors["paidNow"]
            )
            if ((state.paidNowPoisha ?: 0L) > 0) {
                Spacer(Modifier.height(Space.md))
                SelectionField(
                    label = "যে অ্যাকাউন্ট থেকে দিলেন",
                    selectedText = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "বেছে নিন",
                    options = state.accounts,
                    optionLabel = { it.name },
                    onSelect = { selected -> viewModel.update { it.copy(accountId = selected.id) } },
                    error = state.errors["account"]
                )
            }

            Spacer(Modifier.height(Space.md))
            HgCard(padding = PaddingValues(Space.lg)) {
                SummaryAmountRow("মোট ক্রয়", state.totalPoisha)
                SummaryAmountRow("এখন পরিশোধ", state.paidNowPoisha)
                HgDivider()
                Row(
                    Modifier.fillMaxWidth().padding(top = Space.sm),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("বকেয়া থাকবে", style = MaterialTheme.typography.titleSmall, color = Hg.colors.ink)
                    Text(
                        state.dueAfterPoisha?.let { formatAmount(it) } ?: "—",
                        style = HgAmountType.medium,
                        color = Hg.colors.warning
                    )
                }
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
            state.errors["save"]?.let {
                Spacer(Modifier.height(Space.md))
                HgNotice(title = it, tone = HgTone.Expense)
            }
            Spacer(Modifier.height(Space.lg))
            HgNotice(
                title = "পুরো ক্রয়ই এখন খরচ",
                message = "যেটুকু এখন দিলেন না, সেটুকু দোকানের বকেয়া থাকবে। পরে পরিশোধ করলে আবার খরচ ধরা হবে না।",
                tone = HgTone.Neutral
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
private fun SummaryAmountRow(label: String, amount: Long?) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Hg.colors.inkSecondary)
        Text(
            amount?.let { formatAmount(it) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.ink
        )
    }
}

@Composable
private fun HgPurchaseItem(item: ItemUiState, viewModel: ShopPurchaseViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Radius.mdShape)
            .background(Hg.colors.surfaceMuted)
            .padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HgTextField(
                value = item.name,
                onValueChange = { v -> viewModel.updateItem(item.key) { it.copy(name = v) } },
                label = "পণ্যের নাম",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.removeItem(item.key) }) {
                Icon(Icons.Filled.Close, contentDescription = "পণ্য সরান", tint = Hg.colors.inkSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            HgTextField(
                value = item.quantity,
                onValueChange = { v -> viewModel.updateItem(item.key) { it.copy(quantity = v) } },
                label = "পরিমাণ",
                placeholder = "১",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            HgTextField(
                value = item.unit,
                onValueChange = { v -> viewModel.updateItem(item.key) { it.copy(unit = v) } },
                label = "একক",
                placeholder = "কেজি",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            MoneyInputField(
                value = item.unitPrice,
                onValueChange = { v -> viewModel.updateItem(item.key) { it.copy(unitPrice = v) } },
                label = "একক দাম",
                modifier = Modifier.weight(1f)
            )
            MoneyInputField(
                value = item.discount,
                onValueChange = { v -> viewModel.updateItem(item.key) { it.copy(discount = v) } },
                label = "ছাড়",
                modifier = Modifier.weight(1f)
            )
        }
        item.toDraftOrNull()?.let { draft ->
            Text(
                "লাইন মোট ${formatAmount(draft.lineTotalPoisha)}",
                style = MaterialTheme.typography.bodySmall,
                color = Hg.colors.inkSecondary
            )
        }
    }
}
