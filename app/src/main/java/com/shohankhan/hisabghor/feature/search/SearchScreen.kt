package com.shohankhan.hisabghor.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.bnLabel
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.data.SearchRepository
import com.shohankhan.hisabghor.data.SearchResults
import com.shohankhan.hisabghor.di.hisabViewModelFactory
import com.shohankhan.hisabghor.di.rememberAppContainer
import com.shohankhan.hisabghor.feature.transactions.TransactionRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(private val search: SearchRepository) : ViewModel() {

    val query = MutableStateFlow("")

    val results: StateFlow<SearchResults> = query
        .debounce(250)
        .flatMapLatest { q -> flow { emit(search.search(q)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    fun setQuery(value: String) { query.value = value }
}

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenShop: (Long) -> Unit,
    onOpenPerson: (Long) -> Unit,
    onOpenLoan: (Long) -> Unit,
    onOpenAgreement: (Long) -> Unit,
    onOpenAccount: (Long) -> Unit,
    onOpenCategory: (Long) -> Unit
) {
    val container = rememberAppContainer()
    val viewModel: SearchViewModel = viewModel(
        factory = hisabViewModelFactory { SearchViewModel(container.searchRepository) }
    )
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        topBar = { HisabTopBar(title = "খুঁজুন", onBack = onBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                label = { Text("লেনদেন, দোকান, ব্যক্তি, ঋণ খুঁজুন") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            when {
                query.trim().length < 2 -> Text(
                    "কমপক্ষে ২টি অক্ষর লিখুন — নাম, শিরোনাম বা মন্তব্য দিয়ে খুঁজতে পারবেন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                results.isEmpty -> Text(
                    "\"$query\" দিয়ে কিছু পাওয়া যায়নি।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    if (results.transactions.isNotEmpty()) {
                        item { SectionTitle("লেনদেন") }
                        items(results.transactions, key = { "txn_${it.id}" }) { txn ->
                            TransactionRow(txn, onClick = { onOpenTransaction(txn.id) })
                            HorizontalDivider(Modifier.padding(start = 70.dp))
                        }
                    }
                    if (results.shops.isNotEmpty()) {
                        item { SectionTitle("দোকান") }
                        items(results.shops, key = { "shop_${it.id}" }) { shop ->
                            ResultRow(
                                icon = Icons.Filled.Storefront,
                                title = shop.name,
                                subtitle = shop.merchantName ?: shop.address ?: "দোকানের খাতা",
                                onClick = { onOpenShop(shop.id) }
                            )
                        }
                    }
                    if (results.parties.isNotEmpty()) {
                        item { SectionTitle("ব্যক্তি") }
                        items(results.parties, key = { "party_${it.id}" }) { party ->
                            ResultRow(
                                icon = Icons.Filled.Person,
                                title = party.name,
                                subtitle = party.phone ?: "ধার ও পাওনা",
                                onClick = { onOpenPerson(party.id) }
                            )
                        }
                    }
                    if (results.loans.isNotEmpty()) {
                        item { SectionTitle("ঋণ ও ধার") }
                        items(results.loans, key = { "loan_${it.id}" }) { loan ->
                            ResultRow(
                                icon = Icons.Filled.Handshake,
                                title = loan.organizationName,
                                subtitle = loan.kind.bnLabel(),
                                onClick = {
                                    if (loan.kind == LoanKind.NGO || loan.kind == LoanKind.BANK) {
                                        onOpenLoan(loan.id)
                                    } else {
                                        onOpenAgreement(loan.id)
                                    }
                                }
                            )
                        }
                    }
                    if (results.accounts.isNotEmpty()) {
                        item { SectionTitle("অ্যাকাউন্ট") }
                        items(results.accounts, key = { "account_${it.id}" }) { account ->
                            ResultRow(
                                icon = Icons.Filled.AccountBalanceWallet,
                                title = account.name,
                                subtitle = account.type.bnLabel(),
                                onClick = { onOpenAccount(account.id) }
                            )
                        }
                    }
                    if (results.categories.isNotEmpty()) {
                        item { SectionTitle("ক্যাটাগরি") }
                        items(results.categories, key = { "category_${it.id}" }) { category ->
                            ResultRow(
                                icon = Icons.Filled.Category,
                                title = category.name,
                                subtitle = category.kind.bnLabel(),
                                onClick = { onOpenCategory(category.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ResultRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(Modifier.padding(start = 52.dp))
}
