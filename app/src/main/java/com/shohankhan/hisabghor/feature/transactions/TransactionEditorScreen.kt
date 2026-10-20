package com.shohankhan.hisabghor.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.shohankhan.hisabghor.core.designsystem.DateField
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.designsystem.SelectionField
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.TransactionKind
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.ItemDraft
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class EditorKind(val label: String) {
    INCOME("আয়"), EXPENSE("খরচ"), REFUND("রিফান্ড")
}

data class ItemUiState(
    val key: Int,
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
    val unitPrice: String = "",
    val discount: String = ""
)

data class TransactionEditorUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val editorKind: EditorKind = EditorKind.EXPENSE,
    val title: String = "",
    val amountText: String = "",
    val note: String = "",
    val date: LocalDate = Dates.today(),
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val items: List<ItemUiState> = emptyList(),
    val extraChargeText: String = "",
    val discountText: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false
) {
    /** Live-computed items total (poisha), or null when no valid items yet. */
    val itemsTotalPoisha: Long?
        get() {
            if (items.isEmpty()) return null
            var sum = 0L
            for (item in items) {
                val draft = item.toDraft() ?: return null
                sum += draft.lineTotalPoisha
            }
            val extra = BengaliNumber.parsePoisha(extraChargeText.ifBlank { "0" }) ?: return null
            val discount = BengaliNumber.parsePoisha(discountText.ifBlank { "0" }) ?: return null
            return (sum + extra - discount).coerceAtLeast(0)
        }
}

private fun ItemUiState.toDraft(): ItemDraft? {
    if (name.isBlank()) return null
    val qty = BengaliNumber.parseQuantityMilli(quantity.ifBlank { "1" }) ?: return null
    val price = BengaliNumber.parsePoisha(unitPrice) ?: return null
    val discount = BengaliNumber.parsePoisha(discount.ifBlank { "0" }) ?: return null
    if (qty <= 0 || price < 0) return null
    return ItemDraft(name.trim(), qty, unit.trim().ifBlank { null }, price, discount)
}

fun poishaToInputText(poisha: Long): String {
    val taka = poisha / 100
    val rem = poisha % 100
    return if (rem == 0L) taka.toString() else "$taka.${rem.toString().padStart(2, '0')}"
}

class TransactionEditorViewModel(
    private val editId: Long,
    initialKind: EditorKind,
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        TransactionEditorUiState(isEdit = editId > 0, editorKind = initialKind)
    )
    val state: StateFlow<TransactionEditorUiState> = _state

    private var nextItemKey = 1
    private var originalOccurredAt: Long? = null
    private var allCategories: List<CategoryEntity> = emptyList()

    init {
        viewModelScope.launch {
            val accountList = accounts.observeAccountsWithBalances().first().map { it.account }
            val incomeCats = categories.observeByKind(CategoryKind.INCOME).first()
            val expenseCats = categories.observeByKind(CategoryKind.EXPENSE).first()
            allCategories = incomeCats + expenseCats
            _state.update {
                it.copy(
                    loading = false,
                    accounts = accountList,
                    categories = categoriesFor(it.editorKind),
                    accountId = accountList.firstOrNull()?.id
                )
            }
            if (editId > 0) loadForEdit()
        }
    }

    private fun categoriesFor(kind: EditorKind): List<CategoryEntity> =
        allCategories.filter {
            it.kind == if (kind == EditorKind.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        }

    private suspend fun loadForEdit() {
        val txn = transactions.getById(editId) ?: return
        originalOccurredAt = txn.occurredAt
        val kind = when (txn.kind) {
            TransactionKind.INCOME -> EditorKind.INCOME
            TransactionKind.EXPENSE_REFUND -> EditorKind.REFUND
            else -> EditorKind.EXPENSE
        }
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
                editorKind = kind,
                categories = categoriesFor(kind),
                title = txn.title,
                amountText = poishaToInputText(txn.amountPoisha),
                note = txn.note ?: "",
                date = Dates.toLocalDate(txn.occurredAt),
                accountId = txn.accountId,
                categoryId = txn.categoryId,
                items = items,
                extraChargeText = if (txn.extraChargePoisha > 0) poishaToInputText(txn.extraChargePoisha) else "",
                discountText = if (txn.discountPoisha > 0) poishaToInputText(txn.discountPoisha) else ""
            )
        }
    }

    fun setKind(kind: EditorKind) {
        if (_state.value.isEdit) return
        _state.update {
            it.copy(editorKind = kind, categories = categoriesFor(kind), categoryId = null)
        }
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value) }
    fun setAmount(value: String) = _state.update { it.copy(amountText = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }
    fun setDate(value: LocalDate) = _state.update { it.copy(date = value) }
    fun setAccount(id: Long) = _state.update { it.copy(accountId = id) }
    fun setCategory(id: Long) = _state.update { it.copy(categoryId = id) }
    fun setExtraCharge(value: String) = _state.update { it.copy(extraChargeText = value) }
    fun setDiscount(value: String) = _state.update { it.copy(discountText = value) }

    fun addItem() = _state.update { it.copy(items = it.items + ItemUiState(key = nextItemKey++)) }
    fun removeItem(key: Int) = _state.update { s -> s.copy(items = s.items.filterNot { it.key == key }) }
    fun updateItem(key: Int, change: (ItemUiState) -> ItemUiState) =
        _state.update { s -> s.copy(items = s.items.map { if (it.key == key) change(it) else it }) }

    private fun occurredAt(date: LocalDate): Long {
        val original = originalOccurredAt
        if (original != null && Dates.toLocalDate(original) == date) return original
        return if (date == Dates.today()) Dates.nowMs()
        else Dates.startOfDayMs(date) + 12 * 3600_000L
    }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, String>()
        val accountId = s.accountId ?: run { errors["account"] = "অ্যাকাউন্ট বেছে নিন"; null }
        val categoryId = s.categoryId ?: run { errors["category"] = "ক্যাটাগরি বেছে নিন"; null }

        val drafts: List<ItemDraft> = if (s.editorKind == EditorKind.EXPENSE && s.items.isNotEmpty()) {
            val parsed = s.items.map { it.toDraft() }
            if (parsed.any { it == null }) {
                errors["items"] = "প্রতিটি পণ্যের নাম, পরিমাণ ও দাম ঠিক করে দিন"
                emptyList()
            } else parsed.filterNotNull()
        } else emptyList()

        val extra = BengaliNumber.parsePoisha(s.extraChargeText.ifBlank { "0" }) ?: 0L
        val discount = BengaliNumber.parsePoisha(s.discountText.ifBlank { "0" }) ?: 0L
        val amount = BengaliNumber.parsePoisha(s.amountText)
        if (drafts.isEmpty() && (amount == null || amount <= 0)) {
            errors["amount"] = "সঠিক পরিমাণ দিন"
        }
        if (errors.isNotEmpty() || accountId == null || categoryId == null) {
            _state.update { it.copy(errors = errors) }
            return
        }

        val categoryName = s.categories.firstOrNull { it.id == categoryId }?.name ?: ""
        val title = s.title.ifBlank {
            drafts.firstOrNull()?.name ?: categoryName
        }
        val occurredAt = occurredAt(s.date)
        val replaceId = if (editId > 0) editId else null

        _state.update { it.copy(saving = true, errors = emptyMap()) }
        viewModelScope.launch {
            try {
                when (s.editorKind) {
                    EditorKind.INCOME -> transactions.addIncome(
                        accountId, categoryId, amount!!, occurredAt, title, s.note,
                        replacesTransactionId = replaceId
                    )
                    EditorKind.EXPENSE -> transactions.addExpense(
                        accountId, categoryId, amount ?: 0L, drafts, extra, discount,
                        occurredAt, title, s.note, replacesTransactionId = replaceId
                    )
                    EditorKind.REFUND -> transactions.addExpenseRefund(
                        accountId, categoryId, amount!!, occurredAt, title, s.note,
                        replacesTransactionId = replaceId
                    )
                }
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
fun TransactionEditorScreen(
    kindArg: String,
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val initialKind = when (kindArg) {
        "INCOME" -> EditorKind.INCOME
        "REFUND" -> EditorKind.REFUND
        else -> EditorKind.EXPENSE
    }
    val viewModel: TransactionEditorViewModel = viewModel(
        key = "txn_editor_${kindArg}_$editId",
        factory = hisabViewModelFactory {
            TransactionEditorViewModel(
                editId, initialKind,
                container.transactionRepository,
                container.accountRepository,
                container.categoryRepository
            )
        }
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (state.isEdit) "লেনদেন সম্পাদনা" else "নতুন লেনদেন",
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
            if (!state.isEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorKind.entries.forEach { kind ->
                        FilterChip(
                            selected = state.editorKind == kind,
                            onClick = { viewModel.setKind(kind) },
                            label = { Text(kind.label) }
                        )
                    }
                }
            }

            if (state.items.isEmpty()) {
                MoneyInputField(
                    value = state.amountText,
                    onValueChange = viewModel::setAmount,
                    label = "পরিমাণ (টাকা)",
                    error = state.errors["amount"]
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("শিরোনাম") },
                placeholder = { Text("যেমন: মাসের বাজার") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SelectionField(
                label = if (state.editorKind == EditorKind.INCOME) "যে অ্যাকাউন্টে টাকা এসেছে" else "যে অ্যাকাউন্ট থেকে গেছে",
                selectedText = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "বেছে নিন",
                options = state.accounts,
                optionLabel = { it.name },
                onSelect = { viewModel.setAccount(it.id) },
                error = state.errors["account"]
            )

            SelectionField(
                label = "ক্যাটাগরি",
                selectedText = state.categories.firstOrNull { it.id == state.categoryId }?.name ?: "বেছে নিন",
                options = state.categories,
                optionLabel = { it.name },
                onSelect = { viewModel.setCategory(it.id) },
                error = state.errors["category"]
            )

            DateField(
                date = state.date,
                onDateChange = viewModel::setDate,
                label = "তারিখ"
            )

            if (state.editorKind == EditorKind.EXPENSE) {
                ItemsSection(state, viewModel)
                if (state.items.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MoneyInputField(
                            value = state.extraChargeText,
                            onValueChange = viewModel::setExtraCharge,
                            label = "অতিরিক্ত চার্জ/ভ্যাট",
                            modifier = Modifier.weight(1f)
                        )
                        MoneyInputField(
                            value = state.discountText,
                            onValueChange = viewModel::setDiscount,
                            label = "মোট ছাড়",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.itemsTotalPoisha?.let { total ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("মোট খরচ", style = MaterialTheme.typography.titleMedium)
                                Text(formatAmount(total), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                state.errors["items"]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("মন্তব্য (ঐচ্ছিক)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            state.errors["save"]?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.saving) "সংরক্ষণ হচ্ছে…" else "সংরক্ষণ করুন")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ItemsSection(state: TransactionEditorUiState, viewModel: TransactionEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("পণ্যের তালিকা (ঐচ্ছিক)", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = viewModel::addItem) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("পণ্য যোগ")
            }
        }
        state.items.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { v -> viewModel.updateItem(item.key) { it.copy(name = v) } },
                            label = { Text("পণ্যের নাম") },
                            placeholder = { Text("যেমন: চাল") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removeItem(item.key) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "পণ্য সরান")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = item.quantity,
                            onValueChange = { v -> viewModel.updateItem(item.key) { it.copy(quantity = v) } },
                            label = { Text("পরিমাণ") },
                            placeholder = { Text("১") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = item.unit,
                            onValueChange = { v -> viewModel.updateItem(item.key) { it.copy(unit = v) } },
                            label = { Text("একক") },
                            placeholder = { Text("কেজি/টি") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    item.toDraft()?.let { draft ->
                        Text(
                            "লাইন মোট: ${formatAmount(draft.lineTotalPoisha)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (state.items.isEmpty()) {
            Text(
                "পণ্য যোগ করলে মোট খরচ পণ্যের যোগফল থেকে হিসাব হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
