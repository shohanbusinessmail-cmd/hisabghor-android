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
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.TextButton
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.FormSectionLabel
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgBadge
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgChip
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgFormBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgListRow
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgSegmented
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.Radius
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

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

    HgScreen(
        title = "ক্যাটাগরি",
        subtitle = "আয় ও খরচের শ্রেণি",
        onBack = onBack,
        insetBottom = false,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCategory,
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.padding(horizontal = ScreenPadding, vertical = Space.md)) {
                HgSegmented(
                    options = CategoryKind.entries.toList(),
                    selected = kind,
                    label = { if (it == CategoryKind.EXPENSE) "খরচের ক্যাটাগরি" else "আয়ের ক্যাটাগরি" },
                    onSelect = { viewModel.kind.value = it }
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding, vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${BengaliNumber.formatCount(items.size.toLong())}টি ক্যাটাগরি",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary,
                    modifier = Modifier.weight(1f)
                )
                HgChip(
                    text = "আর্কাইভ দেখান",
                    selected = showArchived,
                    onClick = { viewModel.showArchived.value = !showArchived }
                )
            }
            if (items.isEmpty()) {
                com.shohankhan.hisabghor.core.designsystem.EmptyState(
                    icon = Icons.Filled.Category,
                    title = "এই ধরনের কোনো ক্যাটাগরি নেই",
                    subtitle = "নিজের প্রয়োজন অনুযায়ী ক্যাটাগরি তৈরি করুন।",
                    actionLabel = "ক্যাটাগরি যোগ করুন",
                    onAction = onAddCategory
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item {
                        HgCard(
                            Modifier.padding(horizontal = ScreenPadding, vertical = Space.xs),
                            padding = PaddingValues(vertical = Space.xs)
                        ) {
                            items.forEachIndexed { index, category ->
                                HgListRow(
                                    title = category.name,
                                    subtitle = if (category.archivedAt != null) "আর্কাইভ করা" else null,
                                    leading = {
                                        IconChip(
                                            icon = iconForKey(category.iconKey),
                                            tint = colorFromHex(category.colorHex),
                                            background = colorFromHex(category.colorHex).copy(alpha = 0.14f)
                                        )
                                    },
                                    onClick = { onEditCategory(category.id) }
                                )
                                if (index != items.lastIndex) HgDivider(startInset = 68)
                            }
                        }
                    }
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

    HgScreen(
        title = if (state.isEdit) "ক্যাটাগরি সম্পাদনা" else "নতুন ক্যাটাগরি",
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    iconForKey(state.iconKey),
                    tint = colorFromHex(state.colorHex),
                    background = colorFromHex(state.colorHex).copy(alpha = 0.14f),
                    size = 52
                )
                Spacer(Modifier.width(Space.md))
                HgTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = "ক্যাটাগরির নাম",
                    placeholder = "যেমন: বাচ্চাদের পড়াশোনা",
                    modifier = Modifier.weight(1f)
                )
            }
            if (!state.isEdit) {
                Spacer(Modifier.height(Space.md))
                FormSectionLabel("ধরন")
                HgSegmented(
                    options = CategoryKind.entries.toList(),
                    selected = state.kind,
                    label = { it.bnLabel() },
                    onSelect = viewModel::setKind
                )
            }

            Spacer(Modifier.height(Space.lg))
            FormSectionLabel("আইকন", helper = "সর্বোচ্চ যতটা দরকার ততটাই — সহজে চেনা যায় এমন আইকন বেছে নিন")
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                items(iconOptions) { (key, icon) ->
                    val selected = state.iconKey == key
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) colorFromHex(state.colorHex).copy(alpha = 0.18f)
                                else Hg.colors.surfaceMuted
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

            Spacer(Modifier.height(Space.md))
            FormSectionLabel("রং")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                categoryColorOptions.take(6).forEach { color -> ColorDot(color, state, viewModel) }
            }
            Spacer(Modifier.height(Space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                categoryColorOptions.drop(6).forEach { color -> ColorDot(color, state, viewModel) }
            }

            state.message?.let {
                Spacer(Modifier.height(Space.md))
                HgNotice(title = it, tone = HgTone.Expense)
            }

            if (state.isEdit) {
                Spacer(Modifier.height(Space.lg))
                HgDivider()
                Spacer(Modifier.height(Space.md))
                Text(
                    "এই ক্যাটাগরিতে ${BengaliNumber.formatCount(state.usageCount.toLong())}টি হিসাব-এন্ট্রি আছে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary
                )
                Spacer(Modifier.height(Space.sm))
                HgButton(
                    text = if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "আর্কাইভ করুন",
                    onClick = { viewModel.setArchived(!state.archived) },
                    style = HgButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.sm))
                HgButton(
                    text = "স্থায়ীভাবে মুছুন",
                    onClick = { showDeleteConfirm = true },
                    style = HgButtonStyle.Ghost,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "লেনদেন থাকলে মুছে ফেলা যাবে না — ইতিহাস রক্ষার জন্য আর্কাইভ করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkTertiary
                )
            }
            Spacer(Modifier.height(Space.xxl))
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
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) Hg.colors.ink else Color.Transparent,
                shape = CircleShape
            )
            .clickable { viewModel.setColor(hex) }
    )
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

