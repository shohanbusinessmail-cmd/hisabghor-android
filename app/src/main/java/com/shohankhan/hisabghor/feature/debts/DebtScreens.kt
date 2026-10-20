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

    Scaffold(
        topBar = { HisabTopBar(title = "ধার ও পাওনা") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPerson) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন ব্যক্তি যোগ করুন")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("মোট পাওনা", style = MaterialTheme.typography.bodyMedium)
                        AmountText(totalReceivable, style = MaterialTheme.typography.titleLarge, color = Emerald600)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("মোট দেনা", style = MaterialTheme.typography.bodyMedium)
                        AmountText(totalPayable, style = MaterialTheme.typography.titleLarge, color = Danger600)
                    }
                }
            }
            if (summaries.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Handshake,
                    title = "কেউ যোগ হয়নি",
                    subtitle = "যাকে ধার দেন বা যার কাছ থেকে ধার নেন, তাকে যোগ করে চুক্তি তৈরি করুন।",
                    actionLabel = "ব্যক্তি যোগ করুন",
                    onAction = onAddPerson
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(summaries, key = { it.party.id }) { summary ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPerson(summary.party.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    summary.party.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "চলমান চুক্তি: ${BengaliNumber.formatCount(summary.activeAgreements.toLong())}টি",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                when {
                                    summary.netPoisha > 0 -> {
                                        Text("পাওনা", style = MaterialTheme.typography.bodySmall)
                                        AmountText(summary.netPoisha, color = Emerald600)
                                    }
                                    summary.netPoisha < 0 -> {
                                        Text("দেনা", style = MaterialTheme.typography.bodySmall)
                                        AmountText(-summary.netPoisha, color = Danger600)
                                    }
                                    else -> Text(
                                        "হিসাব পরিষ্কার",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(start = 52.dp))
                    }
                }
            }
        }
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

    Scaffold(
        topBar = {
            HisabTopBar(
                title = if (state.isEdit) "ব্যক্তি সম্পাদনা" else "নতুন ব্যক্তি",
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
                label = { Text("নাম") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                label = { Text("মোবাইল (ঐচ্ছিক)") },
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
                    Text(if (state.archived) "আর্কাইভ থেকে ফিরিয়ে আনুন" else "আর্কাইভ করুন")
                }
            }
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

    Scaffold(
        topBar = {
            HisabTopBar(
                title = party?.name ?: "ব্যক্তি",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onEditPerson(personId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "সম্পাদনা করুন")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        party?.phone?.let { KvRow("মোবাইল", BengaliNumber.toBengaliDigits(it)) }
                        party?.address?.let { KvRow("ঠিকানা", it) }
                        party?.note?.let { KvRow("মন্তব্য", it) }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onNewAgreement(personId, "PERSONAL_LEND") },
                                modifier = Modifier.weight(1f)
                            ) { Text("ধার দিলাম") }
                            OutlinedButton(
                                onClick = { onNewAgreement(personId, "PERSONAL_BORROW") },
                                modifier = Modifier.weight(1f)
                            ) { Text("ধার নিলাম") }
                        }
                    }
                }
                Text(
                    "চুক্তিসমূহ",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            if (state.agreements.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Handshake,
                        title = "কোনো চুক্তি নেই",
                        subtitle = "ওপরের বোতাম দিয়ে ধার দেওয়া/নেওয়ার চুক্তি তৈরি করুন।"
                    )
                }
            }
            items(state.agreements, key = { it.loan.id }) { agreement ->
                val loan = agreement.loan
                val isLend = loan.kind == LoanKind.PERSONAL_LEND
                val overdue = loan.dueDate != null && loan.dueDate < Dates.startOfDayMs(Dates.nowMs()) &&
                    agreement.outstandingPoisha > 0 && loan.status == LoanStatus.ACTIVE
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAgreement(loan.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            loan.kind.bnLabel() +
                                if (loan.status == LoanStatus.CLOSED) " (পরিশোধিত)" else "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "${Dates.formatShortDate(loan.startDate)} • আসল ${formatAmount(loan.principalPoisha)}" +
                                (loan.dueDate?.let { " • মেয়াদ ${Dates.formatShortDate(it)}" } ?: "") +
                                if (overdue) " • মেয়াদ শেষ!" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overdue) Danger600 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (isLend) "পাওনা বাকি" else "দেনা বাকি",
                            style = MaterialTheme.typography.bodySmall
                        )
                        AmountText(
                            agreement.outstandingPoisha,
                            color = if (isLend) Emerald600 else Danger600
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
            }
        }
    }
}
