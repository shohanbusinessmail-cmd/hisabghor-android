package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.accounting.PostingFactory
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.model.AccountScope
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.TransactionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AccountRepository(
    private val db: HisabGhorDatabase,
    private val ledger: LedgerRepository
) {

    private val accountDao get() = db.accountDao()

    fun observeAccountsWithBalances(includeArchived: Boolean = false): Flow<List<AccountWithBalance>> =
        combine(
            accountDao.observeAll(includeArchived),
            ledger.balancesByLedger(LedgerType.ACCOUNT)
        ) { accounts, balances ->
            accounts.map { AccountWithBalance(it, balances[it.id] ?: 0L) }
        }

    fun observeTotalBalance(): Flow<Long> =
        ledger.balancesByLedger(LedgerType.ACCOUNT).map { it.values.sum() }

    fun observeBalanceByTypes(types: Set<AccountType>): Flow<Long> =
        observeAccountsWithBalances().map { list ->
            list.filter { it.account.type in types }.sumOf { it.balancePoisha }
        }

    suspend fun getAccount(id: Long): AccountEntity? = accountDao.getById(id)

    fun observeAccount(id: Long): Flow<AccountEntity?> = accountDao.observeById(id)

    /** Creates an account; a non-zero opening balance becomes an auditable opening entry (not income). */
    suspend fun createAccount(
        name: String,
        type: AccountType,
        scope: AccountScope,
        note: String?,
        openingBalancePoisha: Long,
        occurredAt: Long = Dates.nowMs()
    ): Long {
        val now = Dates.nowMs()
        val id = accountDao.insert(
            AccountEntity(
                name = name.trim(), type = type, scope = scope,
                note = note?.trim()?.ifBlank { null },
                createdAt = now, updatedAt = now
            )
        )
        if (openingBalancePoisha != 0L) {
            ledger.record(
                transaction = TransactionEntity(
                    kind = TransactionKind.OPENING_BALANCE,
                    occurredAt = occurredAt,
                    title = "শুরুর ব্যালান্স — ${name.trim()}",
                    amountPoisha = openingBalancePoisha,
                    accountId = id,
                    createdAt = now, updatedAt = now
                ),
                postings = PostingFactory.openingAccountBalance(id, openingBalancePoisha),
                auditSummary = "অ্যাকাউন্ট তৈরি: ${name.trim()}"
            )
        }
        return id
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.update(account.copy(updatedAt = Dates.nowMs()))
    }

    /**
     * Sets the opening balance to a new value by void-and-replace of the
     * original opening entry, so every balance/report updates exactly once.
     */
    suspend fun setOpeningBalance(accountId: Long, newOpeningPoisha: Long) {
        val account = accountDao.getById(accountId) ?: return
        val existing = db.transactionDao().getOpeningTransaction(accountId)
        val now = Dates.nowMs()
        if (newOpeningPoisha == 0L) {
            if (existing != null) {
                ledger.voidTransaction(existing.id, "শুরুর ব্যালান্স সরানো: ${account.name}")
            }
            return
        }
        ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.OPENING_BALANCE,
                occurredAt = existing?.occurredAt ?: now,
                title = "শুরুর ব্যালান্স — ${account.name}",
                amountPoisha = newOpeningPoisha,
                accountId = accountId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.openingAccountBalance(accountId, newOpeningPoisha),
            replacesTransactionId = existing?.id,
            auditSummary = "শুরুর ব্যালান্স পরিবর্তন: ${account.name}"
        )
    }

    /** Reconciliation: adjust so the derived balance equals the real-world figure. */
    suspend fun adjustBalanceTo(accountId: Long, targetBalancePoisha: Long, note: String?) {
        val current = ledger.accountBalance(accountId).first()
        val delta = targetBalancePoisha - current
        require(delta != 0L) { "no adjustment needed" }
        val account = accountDao.getById(accountId)
        val now = Dates.nowMs()
        ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.ADJUSTMENT,
                occurredAt = now,
                title = "ব্যালান্স সমন্বয় — ${account?.name ?: ""}",
                note = note?.trim()?.ifBlank { null },
                amountPoisha = delta,
                accountId = accountId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.adjustment(accountId, delta),
            auditSummary = "ব্যালান্স সমন্বয়: ${account?.name}"
        )
    }

    /** Atomic transfer between two owned accounts (fee optional). */
    suspend fun transfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountPoisha: Long,
        feePoisha: Long,
        feeCategoryId: Long?,
        occurredAt: Long,
        note: String?,
        replacesTransactionId: Long? = null
    ): Long {
        val from = accountDao.getById(fromAccountId)
        val to = accountDao.getById(toAccountId)
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.TRANSFER,
                occurredAt = occurredAt,
                title = "ট্রান্সফার: ${from?.name ?: ""} → ${to?.name ?: ""}",
                note = note?.trim()?.ifBlank { null },
                amountPoisha = amountPoisha,
                feePoisha = feePoisha,
                accountId = fromAccountId,
                counterAccountId = toAccountId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.transfer(fromAccountId, toAccountId, amountPoisha, feePoisha, feeCategoryId),
            replacesTransactionId = replacesTransactionId,
            auditSummary = "ট্রান্সফার: ${from?.name} → ${to?.name}"
        )
    }

    fun observeStatement(accountId: Long) = db.postingDao().observeAccountLedger(accountId)
}
