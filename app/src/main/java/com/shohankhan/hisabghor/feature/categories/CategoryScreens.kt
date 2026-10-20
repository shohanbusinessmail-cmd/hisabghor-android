package com.shohankhan.hisabghor.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.designsystem.ConfirmDialog
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.categoryColorOptions
import com.shohankhan.hisabghor.core.designsystem.colorFromHex
import com.shohankhan.hisabghor.core.designsystem.iconForKey
import com.shohankhan.hisabghor.core.designsystem.iconOptions
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- list ----------------

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModel(categories: CategoryRepository) : ViewModel() {
    val kind = MutableStateFlow(CategoryKind.EXPENSE)
    val showArchived = MutableStateFlow(false)

    val items: StateFlow<List<CategoryEntity>> = kotlinx.coroutines.flow.combine(
        kind, showArchived
    ) { k, archived -> k to archived }.flatMapLatest { (k, archived) ->
        categories.observeByKind(k, archived)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun CategoryListScreen(
    onBack: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: CategoryListViewModel = viewModel(
        factory = hisabViewModelFactory { CategoryListViewModel(container.categoryRepository) }
    )
    val kind by viewModel.kind.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = { HisabTopBar(title = "ক্যাটাগরি", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCategory) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন ক্যাটাগরি")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (kind == CategoryKind.EXPENSE) 0 else 1) {
                Tab(
                    selected = kind == CategoryKind.EXPENSE,
                    onClick = { viewModel.kind.value = CategoryKind.EXPENSE },
                    text = { Text("খরচের ক্যাটাগরি") }
                )
                Tab(
                    selected = kind == CategoryKind.INCOME,
                    onClick = { viewModel.kind.value = CategoryKind.INCOME },
                    text = { Text("আয়ের ক্যাটাগরি") }
                )
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(
                    selected = showArchived,
                    onClick = { viewModel.showArchived.value = !showArchived },
                    label = { Text("আর্কাইভ করাগুলোও দেখান") }
                )
            }
            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Category,
                    title = "এই ধরনের কোনো ক্যাটাগরি নেই",
                    actionLabel = "ক্যাটাগরি যোগ করুন",
                    onAction = onAddCategory
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { category ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onEditCategory(category.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colorFromHex(category.colorHex).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    iconForKey(category.iconKey),
                                    contentDescription = null,
                                    tint = colorFromHex(category.colorHex)
                                )
                            }
                            Spacer(Modifier.height(0.dp))
                            Text(
                                category.name + if (category.archivedAt != null) " (আর্কাইভ)" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        HorizontalDivider(Modifier.padding(start = 68.dp))
                    }
                }
            }
        }
    }
}

// ---------------- editor ----------------

data class CategoryEditorUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val iconKey: String = "label",
    val colorHex: Long = 0xFF0B7A4B,
    val archived: Boolean = false,
    val usageCount: Int = 0,
    val message: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false
)

class CategoryEditorViewModel(
    private val editId: Long,
    private val categories: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryEditorUiState(isEdit = editId > 0))
    val state: StateFlow<CategoryEditorUiState> = _state
    private var existing: CategoryEntity? = null

    init {
        viewModelScope.launch {
            if (editId > 0) {
                val category = categories.getById(editId)
                existing = category
                _state.update {
                    it.copy(
                        loading = false,
                        name = category?.name ?: "",
                        kind = category?.kind ?: CategoryKind.EXPENSE,
                        iconKey = category?.iconKey ?: "label",
                        colorHex = category?.colorHex ?: 0xFF0B7A4B,
                        archived = category?.archivedAt != null,
                        usageCount = categories.usageCount(editId)
                    )
                }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setKind(v: CategoryKind) {
        if (!_state.value.isEdit) _state.update { it.copy(kind = v) }
    }
    fun setIcon(v: String) = _state.update { it.copy(iconKey = v) }
    fun setColor(v: Long) = _state.update { it.copy(colorHex = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(message = "ক্যাটাগরির নাম দিন") }
            return
        }
        viewModelScope.launch {
            if (s.isEdit) {
                existing?.let {
                    categories.update(
                        it.copy(name = s.name.trim(), iconKey = s.iconKey, colorHex = s.colorHex)
                    )
                }
            } else {
                categories.create(s.name, s.kind, s.iconKey, s.colorHex)
            }
            _state.update { it.copy(saved = true) }
        }
    }

    fun setArchived(archived: Boolean) {
        val category = existing ?: return
        viewModelScope.launch {
            categories.setArchived(category, archived)
            existing = categories.getById(editId)
            _state.update { it.copy(archived = archived) }
        }
    }

    fun delete() {
        val category = existing ?: return
        viewModelScope.launch {
            val deleted = categories.deleteIfUnused(category)
            if (deleted) {
                _state.update { it.copy(deleted = true) }
            } else {
                _state.update {
                    it.copy(message = "এই ক্যাটাগরিতে লেনদেন আছে, তাই মুছে ফেলা যাবে না। আর্কাইভ করে রাখুন।")
                }
            }
        }
    }
}

@Composable
fun CategoryEditorScreen(
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: CategoryEditorViewModel = viewModel(
        key = "category_editor_$editId",
        factory = hisabViewModelFactory { CategoryEditorViewModel(editId, container.categoryRepository) }
    )
    val state by viewModel.state.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onSaved()
    }

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (state.isEdit) "ক্যাটাগরি সম্পাদনা" else "নতুন ক্যাটাগরি",
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
                onValueChange = viewModel::setName,
                label = { Text("ক্যাটাগরির নাম") },
                placeholder = { Text("যেমন: বাচ্চাদের পড়াশোনা") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (!state.isEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryKind.entries.forEach { kind ->
                        FilterChip(
                            selected = state.kind == kind,
                            onClick = { viewModel.setKind(kind) },
                            label = { Text(kind.bnLabel()) }
                        )
                    }
                }
            }

            Text("আইকন বেছে নিন", style = MaterialTheme.typography.titleSmall)
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(iconOptions) { (key, icon) ->
                    val selected = state.iconKey == key
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) colorFromHex(state.colorHex).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) colorFromHex(state.colorHex) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setIcon(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = key, tint = colorFromHex(state.colorHex))
                    }
                }
            }

            Text("রং বেছে নিন", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryColorOptions.take(6).forEach { color -> ColorDot(color, state, viewModel) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryColorOptions.drop(6).forEach { color -> ColorDot(color, state, viewModel) }
            }

            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("সংরক্ষণ করুন")
            }

            if (state.isEdit) {
                Text(
                    "এই ক্যাটাগরিতে ${BengaliNumber.formatCount(state.usageCount.toLong())}টি হিসাব-এন্ট্রি আছে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { viewModel.setArchived(!state.archived) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "আর্কাইভ করুন")
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("স্থায়ীভাবে মুছুন", color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "লেনদেন থাকলে মুছে ফেলা যাবে না — ইতিহাস রক্ষার জন্য আর্কাইভ করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "ক্যাটাগরি মুছে ফেলবেন?",
            message = "এটি আর ফেরত আনা যাবে না। (লেনদেন থাকলে মোছা হবে না।)",
            confirmText = "মুছে ফেলুন",
            destructive = true,
            onConfirm = {
                viewModel.delete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun ColorDot(color: Color, state: CategoryEditorUiState, viewModel: CategoryEditorViewModel) {
    val hex = color.toArgb().toLong() and 0xFFFFFFFFL
    val selected = state.colorHex == hex
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .clickable { viewModel.setColor(hex) }
    )
}
