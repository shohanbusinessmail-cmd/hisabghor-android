package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.LoanEntity
import com.shohankhan.hisabghor.core.database.PartyEntity
import com.shohankhan.hisabghor.core.database.ShopEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import kotlinx.coroutines.flow.first

data class SearchResults(
    val transactions: List<TransactionEntity> = emptyList(),
    val shops: List<ShopEntity> = emptyList(),
    val parties: List<PartyEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
) {
    val isEmpty: Boolean
        get() = transactions.isEmpty() && shops.isEmpty() && parties.isEmpty() &&
            loans.isEmpty() && accounts.isEmpty() && categories.isEmpty()
}

/** Global search across every searchable module. Runs fully on-device. */
class SearchRepository(private val db: HisabGhorDatabase) {

    suspend fun search(query: String): SearchResults {
        val q = query.trim()
        if (q.length < 2) return SearchResults()
        val allAccounts = db.accountDao().observeAll(false).first()
        return SearchResults(
            transactions = db.transactionDao().search(q),
            shops = db.shopDao().search(q),
            parties = db.partyDao().search(q),
            loans = db.loanDao().search(q),
            accounts = allAccounts.filter { it.name.contains(q, ignoreCase = true) },
            categories = db.categoryDao().search(q)
        )
    }
}
