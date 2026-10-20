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
import androidx.compose.foundation.layout.PaddingValues
import com.shohankhan.hisabghor.core.designsystem.EmptyState
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgListRow
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgTextField
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

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

    HgScreen(
        title = "খুঁজুন",
        subtitle = "লেনদেন, দোকান, ব্যক্তি, ঋণ",
        onBack = onBack
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = ScreenPadding, vertical = Space.md)) {
                HgTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    label = "লেনদেন, দোকান, ব্যক্তি, ঋণ খুঁজুন",
                    leadingIcon = Icons.Filled.Search
                )
            }
            when {
                query.trim().length < 2 -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = ScreenPadding)
                ) {
                    Spacer(Modifier.height(Space.md))
                    Text(
                        "কমপক্ষে ২টি অক্ষর লিখুন — নাম, শিরোনাম বা মন্তব্য দিয়ে খুঁজতে পারবেন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hg.colors.inkSecondary
                    )
                }
                results.isEmpty -> EmptyState(
                    icon = Icons.Filled.Search,
                    title = "কিছু পাওয়া যায়নি",
                    subtitle = "\"$query\" দিয়ে কোনো লেনদেন, দোকান, ব্যক্তি বা ঋণ মেলেনি।"
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Space.xxl)
                ) {
                    if (results.transactions.isNotEmpty()) {
                        item { SearchSectionTitle("লেনদেন") }
                        items(results.transactions, key = { "txn_${it.id}" }) { txn ->
                            Column(Modifier.padding(horizontal = ScreenPadding)) {
                                TransactionRow(txn, onClick = { onOpenTransaction(txn.id) })
                                HgDivider(startInset = 52)
                            }
                        }
                    }
                    if (results.shops.isNotEmpty()) {
                        item { SearchSectionTitle("দোকান") }
                        item {
                            HgCard(
                                Modifier.padding(horizontal = ScreenPadding),
                                padding = PaddingValues(vertical = Space.xs)
                            ) {
                                results.shops.forEachIndexed { index, shop ->
                                    HgListRow(
                                        title = shop.name,
                                        subtitle = shop.merchantName ?: shop.address ?: "দোকানের খাতা",
                                        leading = { IconChip(Icons.Filled.Storefront) },
                                        onClick = { onOpenShop(shop.id) }
                                    )
                                    if (index != results.shops.lastIndex) HgDivider(startInset = 68)
                                }
                            }
                        }
                    }
                    if (results.parties.isNotEmpty()) {
                        item { SearchSectionTitle("ব্যক্তি") }
                        item {
                            HgCard(
                                Modifier.padding(horizontal = ScreenPadding),
                                padding = PaddingValues(vertical = Space.xs)
                            ) {
                                results.parties.forEachIndexed { index, party ->
                                    HgListRow(
                                        title = party.name,
                                        subtitle = party.phone ?: "ধার ও পাওনা",
                                        leading = { IconChip(Icons.Filled.Person) },
                                        onClick = { onOpenPerson(party.id) }
                                    )
                                    if (index != results.parties.lastIndex) HgDivider(startInset = 68)
                                }
                            }
                        }
                    }
                    if (results.loans.isNotEmpty()) {
                        item { SearchSectionTitle("ঋণ ও ধার") }
                        item {
                            HgCard(
                                Modifier.padding(horizontal = ScreenPadding),
                                padding = PaddingValues(vertical = Space.xs)
                            ) {
                                results.loans.forEachIndexed { index, loan ->
                                    HgListRow(
                                        title = loan.organizationName,
                                        subtitle = loan.kind.bnLabel(),
                                        leading = { IconChip(Icons.Filled.Handshake) },
                                        onClick = {
                                            if (loan.kind == LoanKind.NGO || loan.kind == LoanKind.BANK) {
                                                onOpenLoan(loan.id)
                                            } else {
                                                onOpenAgreement(loan.id)
                                            }
                                        }
                                    )
                                    if (index != results.loans.lastIndex) HgDivider(startInset = 68)
                                }
                            }
                        }
                    }
                    if (results.accounts.isNotEmpty()) {
                        item { SearchSectionTitle("অ্যাকাউন্ট") }
                        item {
                            HgCard(
                                Modifier.padding(horizontal = ScreenPadding),
                                padding = PaddingValues(vertical = Space.xs)
                            ) {
                                results.accounts.forEachIndexed { index, account ->
                                    HgListRow(
                                        title = account.name,
                                        subtitle = account.type.bnLabel(),
                                        leading = { IconChip(Icons.Filled.AccountBalanceWallet) },
                                        onClick = { onOpenAccount(account.id) }
                                    )
                                    if (index != results.accounts.lastIndex) HgDivider(startInset = 68)
                                }
                            }
                        }
                    }
                    if (results.categories.isNotEmpty()) {
                        item { SearchSectionTitle("ক্যাটাগরি") }
                        item {
                            HgCard(
                                Modifier.padding(horizontal = ScreenPadding),
                                padding = PaddingValues(vertical = Space.xs)
                            ) {
                                results.categories.forEachIndexed { index, category ->
                                    HgListRow(
                                        title = category.name,
                                        subtitle = category.kind.bnLabel(),
                                        leading = { IconChip(Icons.Filled.Category) },
                                        onClick = { onOpenCategory(category.id) }
                                    )
                                    if (index != results.categories.lastIndex) HgDivider(startInset = 68)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = Hg.colors.ink,
        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = Space.sm)
    )
}
