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

    Scaffold(
        topBar = { HisabTopBar(title = "দোকান ও বাকির হিসাব") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddShop) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন দোকান যোগ করুন")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("সব দোকানে মোট বকেয়া", style = MaterialTheme.typography.bodyMedium)
                    AmountText(
                        totalDue,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (totalDue > 0) Amber700 else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(Modifier.padding(horizontal = 16.dp)) {
                FilterChip(
                    selected = showArchived,
                    onClick = { viewModel.showArchived.value = !showArchived },
                    label = { Text("আর্কাইভ করাগুলোও দেখান") }
                )
            }
            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Storefront,
                    title = "কোনো দোকান যোগ হয়নি",
                    subtitle = "যে দোকান থেকে বাকিতে কেনেন, সেটি যোগ করে খাতা রাখুন।",
                    actionLabel = "দোকান যোগ করুন",
                    onAction = onAddShop
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.shop.id }) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenShop(item.shop.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    item.shop.name + if (item.shop.archivedAt != null) " (আর্কাইভ)" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    listOfNotNull(item.shop.merchantName, item.shop.shopType)
                                        .joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (item.duePoisha < 0) "অগ্রিম আছে" else "বকেয়া",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AmountText(
                                    kotlin.math.abs(item.duePoisha),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (item.duePoisha > 0) Amber700 else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 52.dp))
                    }
                }
            }
        }
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

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (state.isEdit) "দোকান সম্পাদনা" else "নতুন দোকান",
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
            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = { Text("দোকানের নাম") },
                placeholder = { Text("যেমন: রহিম স্টোর") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.merchantName,
                onValueChange = { v -> viewModel.update { it.copy(merchantName = v) } },
                label = { Text("দোকানদারের নাম (ঐচ্ছিক)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                label = { Text("মোবাইল নম্বর (ঐচ্ছিক)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.address,
                onValueChange = { v -> viewModel.update { it.copy(address = v) } },
                label = { Text("ঠিকানা (ঐচ্ছিক)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.shopType,
                onValueChange = { v -> viewModel.update { it.copy(shopType = v) } },
                label = { Text("দোকানের ধরন (ঐচ্ছিক)") },
                placeholder = { Text("মুদি, ফার্মেসি, চালের আড়ত…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (!state.isEdit) {
                MoneyInputField(
                    value = state.openingDueText,
                    onValueChange = { v -> viewModel.update { it.copy(openingDueText = v) } },
                    label = "আগের বকেয়া (যদি থাকে)"
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
                Text("সংরক্ষণ করুন")
            }
            if (state.isEdit) {
                OutlinedButton(
                    onClick = { viewModel.setArchived(!state.archived) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "দোকান আর্কাইভ করুন")
                }
            }
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

    Scaffold(
        topBar = {
            HisabTopBar(
                title = shop?.name ?: "দোকান",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onEdit(shopId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "দোকান সম্পাদনা")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    if (state.duePoisha >= 0) {
                        Text("বর্তমান বকেয়া", style = MaterialTheme.typography.bodyMedium)
                        AmountText(
                            state.duePoisha,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (state.duePoisha > 0) Amber700 else MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text("দোকানে আপনার অগ্রিম আছে", style = MaterialTheme.typography.bodyMedium)
                        AmountText(
                            -state.duePoisha,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    shop?.let {
                        Spacer(Modifier.height(6.dp))
                        it.merchantName?.let { v -> KvRow("দোকানদার", v) }
                        it.phone?.let { v -> KvRow("মোবাইল", BengaliNumber.toBengaliDigits(v)) }
                        it.address?.let { v -> KvRow("ঠিকানা", v) }
                        it.shopType?.let { v -> KvRow("ধরন", v) }
                        it.note?.let { v -> KvRow("মন্তব্য", v) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onNewPurchase(shopId) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                            Text("  নতুন ক্রয়")
                        }
                        OutlinedButton(onClick = { onNewPayment(shopId) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Payments, contentDescription = null)
                            Text("  পরিশোধ")
                        }
                    }
                }
            }
            Text(
                "দোকানের খাতা (লেনদেনের ইতিহাস)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            if (state.transactions.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Storefront,
                    title = "এখনো কোনো লেনদেন নেই",
                    subtitle = "নতুন ক্রয় যোগ করলে এখানে দেখা যাবে।"
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.transactions, key = { it.id }) { txn ->
                        TransactionRow(txn, onClick = { onOpenTransaction(txn.id) })
                        HorizontalDivider(Modifier.padding(start = 70.dp))
                    }
                }
            }
        }
    }
}

/** Opening-due editor helper re-exported for tests. */
internal fun shopOpeningPreview(text: String): String =
    BengaliNumber.parsePoisha(text)?.let { poishaToInputText(it) } ?: ""
