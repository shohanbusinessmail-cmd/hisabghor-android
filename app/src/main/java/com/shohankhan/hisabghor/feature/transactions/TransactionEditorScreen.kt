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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.FormSectionLabel
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountInput
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgFormBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgSegmented
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space
import com.shohankhan.hisabghor.core.designsystem.Stroke

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
    val itemsTotal = state.itemsTotalPoisha
    val amountPreview = itemsTotal ?: BengaliNumber.parsePoisha(state.amountText)
    HgScreen(
        title = if (state.isEdit) "লেনদেন সম্পাদনা" else {
            when (state.editorKind) {
                EditorKind.INCOME -> "আয় যোগ করুন"
                EditorKind.EXPENSE -> "খরচ যোগ করুন"
                EditorKind.REFUND -> "রিফান্ড যোগ করুন"
            }
        },
        onBack = onBack,
        bottomBar = {
            HgFormBottomBar {
                Column(Modifier.weight(1f)) {
                    Eyebrow("সংরক্ষণ হবে")
                    Text(
                        amountPreview?.let { formatAmount(it) } ?: "৳০",
                        style = HgAmountType.medium,
                        color = Hg.colors.ink
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
        androidx.compose.foundation.layout.Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding)
        ) {
            Spacer(Modifier.height(Space.md))
            if (!state.isEdit) {
                HgSegmented(
                    options = EditorKind.entries,
                    selected = state.editorKind,
                    label = { it.label },
                    onSelect = viewModel::setKind
                )
                Spacer(Modifier.height(Space.lg))
            }
            if (state.items.isEmpty()) {
                HgAmountInput(
                    value = state.amountText,
                    onValueChange = viewModel::setAmount,
                    error = state.errors["amount"]
                )
                Spacer(Modifier.height(Space.sm))
                QuickAmounts(onAdd = { delta ->
                    val current = BengaliNumber.parsePoisha(state.amountText) ?: 0L
                    viewModel.setAmount(poishaToInputText(current + delta))
                })
            }
            Spacer(Modifier.height(Space.lg))
            HgTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = "শিরোনাম",
                placeholder = "যেমন: মাসের বাজার",
                helper = "খালি রাখলে ক্যাটাগরির নাম ব্যবহার হবে"
            )
            Spacer(Modifier.height(Space.md))
            FormSectionLabel("ক্যাটাগরি")
            CategoryChipGrid(
                categories = state.categories,
                selectedId = state.categoryId,
                onSelect = viewModel::setCategory
            )
            state.errors["category"]?.let {
                Text(it, color = Hg.colors.expense, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(Space.md))
            SelectionField(
                label = when (state.editorKind) {
                    EditorKind.INCOME -> "যে অ্যাকাউন্টে টাকা এসেছে"
                    EditorKind.REFUND -> "যে অ্যাকাউন্টে টাকা ফিরেছে"
                    EditorKind.EXPENSE -> "যে অ্যাকাউন্ট থেকে টাকা গেছে"
                },
                selectedText = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "বেছে নিন",
                options = state.accounts,
                optionLabel = { it.name },
                onSelect = { viewModel.setAccount(it.id) },
                error = state.errors["account"]
            )
            Spacer(Modifier.height(Space.md))
            DateField(
                date = state.date,
                onDateChange = viewModel::setDate,
                label = "তারিখ"
            )
            if (state.editorKind == EditorKind.EXPENSE) {
                Spacer(Modifier.height(Space.lg))
                ItemsSection(state, viewModel)
            }
            Spacer(Modifier.height(Space.lg))
            HgTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = "মন্তব্য (ঐচ্ছিক)",
                singleLine = false,
                minLines = 2
            )
            state.errors["save"]?.let {
                Spacer(Modifier.height(Space.md))
                com.shohankhan.hisabghor.core.designsystem.HgNotice(
                    title = it,
                    tone = com.shohankhan.hisabghor.core.designsystem.HgTone.Expense
                )
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}
@Composable
private fun QuickAmounts(onAdd: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        listOf(100L, 500L, 1000L, 5000L).forEach { taka ->
            Box(
                Modifier
                    .clip(Radius.chip)
                    .background(Hg.colors.surfaceMuted)
                    .clickable { onAdd(taka * 100) }
                    .padding(horizontal = Space.md, vertical = Space.xs)
            ) {
                Text(
                    "+${BengaliNumber.formatPoisha(taka * 100, withSymbol = false)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Hg.colors.inkSecondary
                )
            }
        }
    }
}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipGrid(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedId
            Box(
                Modifier
                    .clip(Radius.chip)
                    .background(if (selected) Hg.colors.brandTint else Hg.colors.surface)
                    .border(
                        Stroke.hairline,
                        if (selected) Hg.colors.brand else Hg.colors.hairlineStrong,
                        Radius.chip
                    )
                    .clickable { onSelect(category.id) }
                    .padding(horizontal = Space.md, vertical = Space.sm)
            ) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Hg.colors.onBrandTint else Hg.colors.inkSecondary
                )
            }
        }
    }
    if (categories.isEmpty()) {
        Text(
            "কোনো ক্যাটাগরি নেই — ‘আরও’ থেকে ক্যাটাগরি যোগ করুন।",
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.inkSecondary
        )
    }
}
@Composable
private fun ItemsSection(state: TransactionEditorUiState, viewModel: TransactionEditorViewModel) {
    var expanded by remember { mutableStateOf(state.items.isNotEmpty()) }
    HgCard(padding = PaddingValues(Space.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "পণ্যের তালিকা",
                    style = MaterialTheme.typography.titleSmall,
                    color = Hg.colors.ink
                )
                Text(
                    if (state.items.isEmpty()) "ঐচ্ছিক — বাজার বা বিল ভাগ করে লিখুন"
                    else "${state.items.size}টি পণ্য যোগ করা হয়েছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary
                )
            }
            TextButton(onClick = {
                expanded = true
                viewModel.addItem()
            }) {
                Text("যোগ করুন", color = Hg.colors.brand)
            }
        }
        if (expanded && state.items.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            state.items.forEach { item ->
                HgItemEditor(item, viewModel)
                Spacer(Modifier.height(Space.sm))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                MoneyInputField(
                    value = state.extraChargeText,
                    onValueChange = viewModel::setExtraCharge,
                    label = "অতিরিক্ত চার্জ",
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
                Spacer(Modifier.height(Space.sm))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(Radius.mdShape)
                        .background(Hg.colors.surfaceMuted)
                        .padding(Space.md),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("মোট খরচ", style = MaterialTheme.typography.titleSmall, color = Hg.colors.ink)
                    Text(formatAmount(total), style = HgAmountType.medium, color = Hg.colors.ink)
                }
            }
        }
        state.errors["items"]?.let {
            Spacer(Modifier.height(Space.sm))
            Text(it, color = Hg.colors.expense, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
private fun HgItemEditor(item: ItemUiState, viewModel: TransactionEditorViewModel) {
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
                placeholder = "যেমন: চাল",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.removeItem(item.key) }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "পণ্য সরান",
                    tint = Hg.colors.inkSecondary
                )
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
        item.toDraft()?.let { draft ->
            Text(
                "লাইন মোট ${formatAmount(draft.lineTotalPoisha)}",
                style = MaterialTheme.typography.bodySmall,
                color = Hg.colors.inkSecondary
            )
        }
    }
}