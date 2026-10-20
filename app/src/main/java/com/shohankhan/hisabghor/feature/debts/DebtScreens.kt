package com.shohankhan.hisabghor.feature.debts

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
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.PartyEntity
import com.shohankhan.hisabghor.core.designsystem.AmountText
import com.shohankhan.hisabghor.core.designsystem.Danger600
import com.shohankhan.hisabghor.core.designsystem.Emerald600
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.designsystem.LoadingState
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.designsystem.formatAmount
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.LoanStatus
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.data.LoanRepository
import com.shohankhan.hisabghor.data.LoanWithBalance
import com.shohankhan.hisabghor.data.PartyRepository
import com.shohankhan.hisabghor.data.PartySummary
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExtendedFloatingActionButton
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgAmountType
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
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

// ---------------- people list ----------------

class DebtListViewModel(parties: PartyRepository) : ViewModel() {
    val summaries: StateFlow<List<PartySummary>> = parties.observePartySummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun DebtListScreen(
    onAddPerson: () -> Unit,
    onOpenPerson: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: DebtListViewModel = viewModel(
        factory = hisabViewModelFactory { DebtListViewModel(container.partyRepository) }
    )
    val summaries by viewModel.summaries.collectAsState()
    val totalReceivable = summaries.sumOf { it.receivablePoisha }
    val totalPayable = summaries.sumOf { it.payablePoisha }

    HgScreen(
        title = "ধার ও পাওনা",
        subtitle = "ব্যক্তিভিত্তিক হিসাব",
        insetBottom = false,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPerson,
                containerColor = Hg.colors.brand,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = Radius.control,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("নতুন ব্যক্তি", style = MaterialTheme.typography.labelLarge) }
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
                    padding = PaddingValues(Space.lg)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("আমি পাব")
                            Spacer(Modifier.height(Space.xs))
                            AmountText(totalReceivable, style = HgAmountType.medium, color = Hg.colors.income)
                        }
                        Column(Modifier.weight(1f)) {
                            Eyebrow("আমি দেব")
                            Spacer(Modifier.height(Space.xs))
                            AmountText(totalPayable, style = HgAmountType.medium, color = Hg.colors.expense)
                        }
                    }
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "ধার দেওয়া টাকা খরচ নয়, ধার নেওয়া টাকা আয় নয় — শুধু পাওনা ও দেনা।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hg.colors.inkSecondary
                    )
                }
            }
            item {
                Text(
                    "ব্যক্তি",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
                )
            }
            if (summaries.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Handshake,
                        title = "কেউ যোগ হয়নি",
                        subtitle = "যাকে ধার দেন বা যার কাছ থেকে ধার নেন, তাকে যোগ করে চুক্তি তৈরি করুন।",
                        actionLabel = "ব্যক্তি যোগ করুন",
                        onAction = onAddPerson
                    )
                }
            } else {
                item {
                    HgCard(
                        Modifier.padding(horizontal = ScreenPadding),
                        padding = PaddingValues(vertical = Space.xs)
                    ) {
                        summaries.forEachIndexed { index, summary ->
                            HgListRow(
                                title = summary.party.name,
                                subtitle = "চলমান চুক্তি ${BengaliNumber.formatCount(summary.activeAgreements.toLong())}টি",
                                leading = { IconChip(Icons.Filled.Person) },
                                trailing = {
                                    when {
                                        summary.netPoisha > 0 -> Column(horizontalAlignment = Alignment.End) {
                                            Text("পাওনা", style = MaterialTheme.typography.labelSmall, color = Hg.colors.inkTertiary)
                                            AmountText(summary.netPoisha, style = HgAmountType.small, color = Hg.colors.income)
                                        }
                                        summary.netPoisha < 0 -> Column(horizontalAlignment = Alignment.End) {
                                            Text("দেনা", style = MaterialTheme.typography.labelSmall, color = Hg.colors.inkTertiary)
                                            AmountText(-summary.netPoisha, style = HgAmountType.small, color = Hg.colors.expense)
                                        }
                                        else -> Text(
                                            "হিসাব পরিষ্কার",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Hg.colors.inkSecondary
                                        )
                                    }
                                },
                                onClick = { onOpenPerson(summary.party.id) }
                            )
                            if (index != summaries.lastIndex) HgDivider(startInset = 68)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonEditorScreen(
    editId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: PersonEditorViewModel = viewModel(
        key = "person_editor_$editId",
        factory = hisabViewModelFactory { PersonEditorViewModel(editId, container.partyRepository) }
    )
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    HgScreen(
        title = if (state.isEdit) "ব্যক্তি সম্পাদনা" else "নতুন ব্যক্তি",
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
                label = "নাম"
            )
            Spacer(Modifier.height(Space.md))
            HgTextField(
                value = state.phone,
                onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                label = "মোবাইল (ঐচ্ছিক)",
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
                    text = if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "আর্কাইভ করুন",
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
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    onEditPerson: (Long) -> Unit,
    onNewAgreement: (personId: Long, kind: String) -> Unit,
    onOpenAgreement: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: PersonDetailViewModel = viewModel(
        key = "person_detail_$personId",
        factory = hisabViewModelFactory {
            PersonDetailViewModel(
                personId, container.partyRepository,
                container.database, container.loanRepository
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    val party = state.party
    val receivable = state.agreements.filter { it.loan.kind == LoanKind.PERSONAL_LEND }
        .sumOf { it.outstandingPoisha }
    val payable = state.agreements.filter { it.loan.kind != LoanKind.PERSONAL_LEND }
        .sumOf { it.outstandingPoisha }

    HgScreen(
        title = party?.name ?: "ব্যক্তি",
        subtitle = "ধার ও চুক্তির হিসাব",
        onBack = onBack,
        actions = {
            IconButton(onClick = { onEditPerson(personId) }) {
                Icon(Icons.Filled.Edit, contentDescription = "সম্পাদনা করুন", tint = Hg.colors.ink)
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
                    Eyebrow("এই ব্যক্তির সঙ্গে")
                    Spacer(Modifier.height(Space.xs))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("পাওনা", style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary)
                            AmountText(receivable, style = HgAmountType.medium, color = Hg.colors.income)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("দেনা", style = MaterialTheme.typography.bodySmall, color = Hg.colors.inkSecondary)
                            AmountText(payable, style = HgAmountType.medium, color = Hg.colors.expense)
                        }
                    }
                    Spacer(Modifier.height(Space.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        HgButton(
                            text = "ধার দিলাম",
                            onClick = { onNewAgreement(personId, "PERSONAL_LEND") },
                            modifier = Modifier.weight(1f)
                        )
                        HgButton(
                            text = "ধার নিলাম",
                            onClick = { onNewAgreement(personId, "PERSONAL_BORROW") },
                            style = HgButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            if (party?.phone != null || party?.address != null || party?.note != null) {
                item {
                    HgCard(
                        Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm),
                        padding = PaddingValues(vertical = Space.xs)
                    ) {
                        party?.phone?.let { PersonLine("মোবাইল", BengaliNumber.toBengaliDigits(it)) }
                        party?.address?.let { PersonLine("ঠিকানা", it) }
                        party?.note?.takeIf { it.isNotBlank() }?.let { PersonLine("মন্তব্য", it) }
                    }
                }
            }
            item {
                Text(
                    "চুক্তিসমূহ",
                    style = MaterialTheme.typography.titleMedium,
                    color = Hg.colors.ink,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.md)
                )
            }
            if (state.agreements.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Handshake,
                        title = "কোনো চুক্তি নেই",
                        subtitle = "ওপরের বোতাম দিয়ে ধার দেওয়া বা নেওয়ার চুক্তি তৈরি করুন।"
                    )
                }
            }
            items(state.agreements, key = { it.loan.id }) { agreement ->
                val loan = agreement.loan
                val isLend = loan.kind == LoanKind.PERSONAL_LEND
                val overdue = loan.dueDate != null && loan.dueDate < Dates.startOfDayMs(Dates.nowMs()) &&
                    agreement.outstandingPoisha > 0 && loan.status == LoanStatus.ACTIVE
                Column(Modifier.padding(horizontal = ScreenPadding)) {
                    HgListRow(
                        title = loan.kind.bnLabel() +
                            if (loan.status == LoanStatus.CLOSED) " (পরিশোধিত)" else "",
                        subtitle = "${Dates.formatShortDate(loan.startDate)} • আসল ${formatAmount(loan.principalPoisha)}" +
                            (loan.dueDate?.let { " • মেয়াদ ${Dates.formatShortDate(it)}" } ?: "") +
                            if (overdue) " • মেয়াদ শেষ" else "",
                        leading = {
                            IconChip(
                                Icons.Filled.Handshake,
                                tint = if (isLend) Hg.colors.income else Hg.colors.expense,
                                background = if (isLend) Hg.colors.incomeTint else Hg.colors.expenseTint
                            )
                        },
                        trailing = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (isLend) "পাওনা বাকি" else "দেনা বাকি",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Hg.colors.inkTertiary
                                )
                                AmountText(
                                    agreement.outstandingPoisha,
                                    style = HgAmountType.small,
                                    color = if (isLend) Hg.colors.income else Hg.colors.expense
                                )
                            }
                        },
                        onClick = { onOpenAgreement(loan.id) }
                    )
                    HgDivider(startInset = 52)
                }
            }
        }
    }
}

@Composable
private fun PersonLine(label: String, value: String) {
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

// ---------------- person editor ----------------

data class PersonEditorUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val archived: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class PersonEditorViewModel(
    private val editId: Long,
    private val parties: PartyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PersonEditorUiState(isEdit = editId > 0))
    val state: StateFlow<PersonEditorUiState> = _state
    private var existing: PartyEntity? = null

    init {
        viewModelScope.launch {
            if (editId > 0) {
                val party = parties.getById(editId)
                existing = party
                _state.update {
                    it.copy(
                        loading = false,
                        name = party?.name ?: "",
                        phone = party?.phone ?: "",
                        address = party?.address ?: "",
                        note = party?.note ?: "",
                        archived = party?.archivedAt != null
                    )
                }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun update(change: (PersonEditorUiState) -> PersonEditorUiState) = _state.update(change)

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "নাম দিন") }
            return
        }
        viewModelScope.launch {
            try {
                if (s.isEdit) {
                    existing?.let {
                        parties.update(
                            it.copy(
                                name = s.name.trim(),
                                phone = s.phone.trim().ifBlank { null },
                                address = s.address.trim().ifBlank { null },
                                note = s.note.trim().ifBlank { null }
                            )
                        )
                    }
                } else {
                    parties.create(s.name, s.phone, s.address, s.note)
                }
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "সংরক্ষণ করা যায়নি") }
            }
        }
    }

    fun setArchived(archived: Boolean) {
        val party = existing ?: return
        viewModelScope.launch {
            parties.setArchived(party, archived)
            existing = parties.getById(editId)
            _state.update { it.copy(archived = archived) }
        }
    }
}

// ---------------- person detail ----------------

data class PersonDetailUiState(
    val loading: Boolean = true,
    val party: PartyEntity? = null,
    val agreements: List<LoanWithBalance> = emptyList()
)

class PersonDetailViewModel(
    personId: Long,
    parties: PartyRepository,
    db: HisabGhorDatabase,
    loans: LoanRepository
) : ViewModel() {

    val uiState: StateFlow<PersonDetailUiState> = combine(
        parties.observeById(personId),
        db.loanDao().observeForParty(personId),
        loans.observeLoansWithBalances()
    ) { party, partyLoans, allWithBalances ->
        val balanceById = allWithBalances.associateBy { it.loan.id }
        PersonDetailUiState(
            loading = false,
            party = party,
            agreements = partyLoans.mapNotNull { balanceById[it.id] }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())
}

