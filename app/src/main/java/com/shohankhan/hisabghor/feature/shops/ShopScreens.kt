package com.shohankhan.hisabghor.feature.shops

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.database.ShopEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Amber700
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.MoneyInputField
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.ShopRepository
import com.shohankhan.hisabghor.data.ShopWithDue
import com.shohankhan.hisabghor.data.TransactionRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.TransactionRow
import com.shohankhan.hisabghor.feature.transactions.poishaToInputText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExtendedFloatingActionButton
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.FormSectionLabel
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgChip
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

// ---------------- list ----------------

@OptIn(ExperimentalCoroutinesApi::class)
class ShopListViewModel(shops: ShopRepository) : ViewModel() {
    val showArchived = MutableStateFlow(false)
    val items: StateFlow<List<ShopWithDue>> = showArchived
        .flatMapLatest { shops.observeShopsWithDue(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalDue: StateFlow<Long> = shops.observeTotalShopDue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
}

@Composable
fun ShopListScreen(
    onAddShop: () -> Unit,
    onOpenShop: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: ShopListViewModel = viewModel(
        factory = hisabViewModelFactory { ShopListViewModel(container.shopRepository) }
    )
    val items by viewModel.items.collectAsState()
    val totalDue by viewModel.totalDue.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()

    HgScreen(
        title = "দোকানের খাতা",
        subtitle = "বাকি কেনা ও পরিশোধ",
        insetBottom = false,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddShop,
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন দোকান", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                HgCard(
                    Modifier.padding(horizontal = ScreenPadding, vertical = Space.md),
                    padding = PaddingValues(Space.xl)
                ) {
                    Eyebrow("সব দোকানে মোট বাকি")
                    Spacer(Modifier.height(Space.xs))
                    AmountText(
                        totalDue,
                        style = HgAmountType.large,
                        color = if (totalDue > 0) Hg.colors.warning else Hg.colors.ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "এই টাকা দোকানকে দিতে হবে",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPadding, vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "দোকান",
                        style = MaterialTheme.typography.titleMedium,
                        color = Hg.colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    HgChip(
                        text = "আর্কাইভ",
                        selected = showArchived,
                        onClick = { viewModel.showArchived.value = !showArchived }
                    )
                }
            }
            if (items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Storefront,
                        title = "কোনো দোকান যোগ হয়নি",
                        subtitle = "যে দোকান থেকে বাকিতে কেনেন, সেটি যোগ করে খাতা রাখুন।",
                        actionLabel = "দোকান যোগ করুন",
                        onAction = onAddShop
                    )
                }
            } else {
                item {
                    HgCard(
                        Modifier.padding(horizontal = ScreenPadding),
                        padding = PaddingValues(vertical = Space.xs)
                    ) {
                        items.forEachIndexed { index, item ->
                            HgListRow(
                                title = item.shop.name,
                                subtitle = listOfNotNull(
                                    item.shop.merchantName,
                                    item.shop.shopType,
                                    if (item.shop.archivedAt != null) "আর্কাইভ" else null
                                ).joinToString(" • "),
                                leading = { IconChip(Icons.Filled.Storefront) },
                                trailing = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            if (item.duePoisha < 0) "অগ্রিম" else "বাকি",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Hg.colors.inkTertiary
                                        )
                                        AmountText(
                                            kotlin.math.abs(item.duePoisha),
                                            style = HgAmountType.small,
                                            color = when {
                                                item.duePoisha > 0 -> Hg.colors.warning
                                                item.duePoisha < 0 -> Hg.colors.income
                                                else -> Hg.colors.ink
                                            }
                                        )
                                    }
                                },
                                onClick = { onOpenShop(item.shop.id) }
                            )
                            if (index != items.lastIndex) HgDivider(startInset = 68)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopEditorScreen(
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: ShopEditorViewModel = viewModel(
        key = "shop_editor_$editId",
        factory = hisabViewModelFactory { ShopEditorViewModel(editId, container.shopRepository) }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    HgScreen(
        title = if (state.isEdit) "দোকান সম্পাদনা" else "নতুন দোকান",
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
            HgTextField(
                value = state.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = "দোকানের নাম",
                placeholder = "যেমন: রহিম স্টোর"
            )
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.merchantName,
                onValueChange = { v -> viewModel.update { it.copy(merchantName = v) } },
                label = "দোকানদারের নাম (ঐচ্ছিক)"
            )
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.phone,
                onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                label = "মোবাইল নম্বর (ঐচ্ছিক)",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.address,
                onValueChange = { v -> viewModel.update { it.copy(address = v) } },
                label = "ঠিকানা (ঐচ্ছিক)"
            )
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.shopType,
                onValueChange = { v -> viewModel.update { it.copy(shopType = v) } },
                label = "দোকানের ধরন (ঐচ্ছিক)",
                placeholder = "মুদি, ফার্মেসি, চালের আড়ত…"
            )
            if (!state.isEdit) {
                Spacer(Modifier.height(Space.lg))
                MoneyInputField(
                    value = state.openingDueText,
                    onValueChange = { v -> viewModel.update { it.copy(openingDueText = v) } },
                    label = "আগের বকেয়া (যদি থাকে)"
                )
                Spacer(Modifier.height(Space.sm))
                HgNotice(
                    title = "আগের বকেয়া খরচ নয়",
                    message = "আগে থেকেই যে টাকা বাকি ছিল, তা নতুন খরচ হিসেবে ধরা হবে না — শুধু দেনা হিসেবে দেখাবে।",
                    tone = HgTone.Neutral
                )
            }
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
            if (state.isEdit) {
                Spacer(Modifier.height(Space.lg))
                HgButton(
                    text = if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "দোকান আর্কাইভ করুন",
                    onClick = { viewModel.setArchived(!state.archived) },
                    style = HgButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
fun ShopDetailScreen(
    shopId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onNewPurchase: (Long) -> Unit,
    onNewPayment: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: ShopDetailViewModel = viewModel(
        key = "shop_detail_$shopId",
        factory = hisabViewModelFactory {
            ShopDetailViewModel(
                shopId, container.shopRepository,
                container.transactionRepository, container.ledgerRepository
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val shop = state.shop

    HgScreen(
        title = shop?.name ?: "দোকান",
        subtitle = "দোকানের খাতা",
        onBack = onBack,
        actions = {
            IconButton(onClick = { onEdit(shopId) }) {
                Icon(Icons.Filled.Edit, contentDescription = "দোকান সম্পাদনা", tint = Hg.colors.ink)
            }
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@HgScreen
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = Space.xxl)
        ) {
            item {
                Column(Modifier.padding(horizontal = ScreenPadding, vertical = Space.md)) {
                    Eyebrow(if (state.duePoisha >= 0) "বর্তমান বকেয়া" else "দোকানে আপনার অগ্রিম")
                    Spacer(Modifier.height(Space.xs))
                    AmountText(
                        kotlin.math.abs(state.duePoisha),
                        style = HgAmountType.hero,
                        color = when {
                            state.duePoisha > 0 -> Hg.colors.warning
                            state.duePoisha < 0 -> Hg.colors.income
                            else -> Hg.colors.ink
                        }
                    )
                    Spacer(Modifier.height(Space.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        HgButton(
                            text = "নতুন বাকি ক্রয়",
                            onClick = { onNewPurchase(shopId) },
                            icon = Icons.Filled.ShoppingCart,
                            modifier = Modifier.weight(1f)
                        )
                        HgButton(
                            text = "পরিশোধ",
                            onClick = { onNewPayment(shopId) },
                            style = HgButtonStyle.Secondary,
                            icon = Icons.Filled.Payments,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            shop?.let { s ->
                item {
                    HgCard(
                        Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm),
                        padding = PaddingValues(vertical = Space.xs)
                    ) {
                        s.merchantName?.let { DetailLine("দোকানদার", it) }
                        s.phone?.let { DetailLine("মোবাইল", BengaliNumber.toBengaliDigits(it)) }
                        s.address?.let { DetailLine("ঠিকানা", it) }
                        s.shopType?.let { DetailLine("ধরন", it) }
                        s.note?.takeIf { it.isNotBlank() }?.let { DetailLine("মন্তব্য", it) }
                    }
                }
            }
            item {
                Text(
                    "লেনদেনের ইতিহাস",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.md)
                )
            }
            if (state.transactions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Storefront,
                        title = "এখনো কোনো লেনদেন নেই",
                        subtitle = "নতুন বাকি ক্রয় যোগ করলে এখানে দেখা যাবে।"
                    )
                }
            } else {
                items(state.transactions, key = { it.id }) { txn ->
                    Column(Modifier.padding(horizontal = ScreenPadding)) {
                        TransactionRow(txn, onClick = { onOpenTransaction(txn.id) })
                        HgDivider(startInset = 52)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
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
            modifier = Modifier.weight(1.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

// ---------------- editor ----------------

data class ShopEditorUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val merchantName: String = "",
    val phone: String = "",
    val address: String = "",
    val shopType: String = "",
    val note: String = "",
    val openingDueText: String = "",
    val archived: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class ShopEditorViewModel(
    private val editId: Long,
    private val shops: ShopRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ShopEditorUiState(isEdit = editId > 0))
    val state: StateFlow<ShopEditorUiState> = _state
    private var existing: ShopEntity? = null

    init {
        viewModelScope.launch {
            if (editId > 0) {
                val shop = shops.getShop(editId)
                existing = shop
                _state.update {
                    it.copy(
                        loading = false,
                        name = shop?.name ?: "",
                        merchantName = shop?.merchantName ?: "",
                        phone = shop?.phone ?: "",
                        address = shop?.address ?: "",
                        shopType = shop?.shopType ?: "",
                        note = shop?.note ?: "",
                        archived = shop?.archivedAt != null
                    )
                }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun update(change: (ShopEditorUiState) -> ShopEditorUiState) = _state.update(change)

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "দোকানের নাম দিন") }
            return
        }
        val openingDue = BengaliNumber.parsePoisha(s.openingDueText.ifBlank { "0" })
        if (openingDue == null || openingDue < 0) {
            _state.update { it.copy(error = "আগের বকেয়ার পরিমাণ সঠিক নয়") }
            return
        }
        viewModelScope.launch {
            try {
                if (s.isEdit) {
                    existing?.let {
                        shops.updateShop(
                            it.copy(
                                name = s.name.trim(),
                                merchantName = s.merchantName.trim().ifBlank { null },
                                phone = s.phone.trim().ifBlank { null },
                                address = s.address.trim().ifBlank { null },
                                shopType = s.shopType.trim().ifBlank { null },
                                note = s.note.trim().ifBlank { null }
                            )
                        )
                    }
                } else {
                    shops.createShop(
                        s.name, s.merchantName, s.phone, s.address,
                        s.shopType, s.note, openingDue
                    )
                }
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "সংরক্ষণ করা যায়নি — আবার চেষ্টা করুন") }
            }
        }
    }

    fun setArchived(archived: Boolean) {
        val shop = existing ?: return
        viewModelScope.launch {
            shops.setArchived(shop, archived)
            existing = shops.getShop(editId)
            _state.update { it.copy(archived = archived) }
        }
    }
}

// ---------------- detail (ledger) ----------------

data class ShopDetailUiState(
    val loading: Boolean = true,
    val shop: ShopEntity? = null,
    val duePoisha: Long = 0,
    val transactions: List<TransactionEntity> = emptyList()
)

class ShopDetailViewModel(
    shopId: Long,
    shops: ShopRepository,
    transactions: TransactionRepository,
    ledger: com.shohankhan.hisabghor.data.LedgerRepository
) : ViewModel() {

    val uiState: StateFlow<ShopDetailUiState> = combine(
        shops.observeShop(shopId),
        ledger.shopDue(shopId),
        transactions.observeFiltered(shopId = shopId)
    ) { shop, due, txns ->
        ShopDetailUiState(loading = false, shop = shop, duePoisha = due, transactions = txns)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopDetailUiState())
}

/** Opening-due editor helper re-exported for tests. */
internal fun shopOpeningPreview(text: String): String =
    BengaliNumber.parsePoisha(text)?.let { poishaToInputText(it) } ?: ""
