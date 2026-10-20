package com.shohankhan.hisabghor.data

import androidx.room.withTransaction
import com.shohankhan.hisabghor.core.accounting.PostingDraft
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.InstallmentAllocationEntity
import com.shohankhan.hisabghor.core.database.LedgerBalanceRow
import com.shohankhan.hisabghor.core.database.PostingEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.database.TransactionItemEntity
import com.shohankhan.hisabghor.core.model.LedgerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single write path for all financial events.
 *
 * Guarantees:
 *  - Transaction + postings + items + installment allocations are written in
 *    ONE database transaction (all-or-nothing).
 *  - Postings must balance to zero (re-checked here, not just in PostingFactory).
 *  - Editing = void old + insert corrected copy (linked), atomically, with
 *    installment paid-amounts reversed and re-applied.
 *  - Every mutation leaves an audit event.
 */
class LedgerRepository(
    private val db: HisabGhorDatabase,
    private val audit: AuditLogger
) {

    private val txnDao get() = db.transactionDao()
    private val postingDao get() = db.postingDao()
    private val installmentDao get() = db.installmentDao()

    /**
     * Records a financial event. When [replacesTransactionId] is given, the old
     * transaction is voided first (edit flow) inside the same DB transaction.
     * [items]/[allocations] may carry transactionId = 0; it is patched here.
     */
    suspend fun record(
        transaction: TransactionEntity,
        postings: List<PostingDraft>,
        items: List<TransactionItemEntity> = emptyList(),
        allocations: List<InstallmentAllocationEntity> = emptyList(),
        replacesTransactionId: Long? = null,
        auditSummary: String,
        /** Extra writes that must commit or roll back together with this event. */
        afterInsert: suspend (transactionId: Long) -> Unit = {}
    ): Long {
        val sum = postings.fold(0L) { acc, p -> Math.addExact(acc, p.signedAmountPoisha) }
        require(sum == 0L) { "postings must balance to zero" }
        require(postings.isNotEmpty()) { "postings required" }

        return db.withTransaction {
            if (replacesTransactionId != null) {
                reverseAllocations(replacesTransactionId)
                txnDao.voidTransaction(replacesTransactionId, Dates.nowMs())
            }
            val txnId = txnDao.insertTransaction(
                transaction.copy(replacesTransactionId = replacesTransactionId)
            )
            txnDao.insertPostings(postings.map {
                PostingEntity(
                    transactionId = txnId,
                    ledgerType = it.ledgerType,
                    ledgerId = it.ledgerId,
                    signedAmountPoisha = it.signedAmountPoisha
                )
            })
            if (items.isNotEmpty()) {
                txnDao.insertItems(items.map { it.copy(transactionId = txnId) })
            }
            if (allocations.isNotEmpty()) {
                val patched = allocations.map { it.copy(transactionId = txnId) }
                txnDao.insertAllocations(patched)
                applyAllocations(patched)
            }
            afterInsert(txnId)
            audit.log(
                if (replacesTransactionId != null) AuditLogger.EDIT else AuditLogger.CREATE,
                "transaction", txnId, auditSummary
            )
            txnId
        }
    }

    /** Voids a transaction (safe delete), reversing installment allocations. */
    suspend fun voidTransaction(transactionId: Long, auditSummary: String) {
        db.withTransaction {
            reverseAllocations(transactionId)
            txnDao.voidTransaction(transactionId, Dates.nowMs())
            audit.log(AuditLogger.VOID, "transaction", transactionId, auditSummary)
        }
    }

    private suspend fun applyAllocations(allocations: List<InstallmentAllocationEntity>) {
        for (alloc in allocations) {
            val inst = installmentDao.getById(alloc.installmentId) ?: continue
            installmentDao.update(
                inst.copy(
                    paidPrincipalPoisha = inst.paidPrincipalPoisha + alloc.principalPaidPoisha,
                    paidInterestPoisha = inst.paidInterestPoisha + alloc.interestPaidPoisha,
                    paidFeePoisha = inst.paidFeePoisha + alloc.feePaidPoisha,
                    updatedAt = Dates.nowMs()
                )
            )
        }
    }

    private suspend fun reverseAllocations(transactionId: Long) {
        for (alloc in txnDao.getAllocations(transactionId)) {
            val inst = installmentDao.getById(alloc.installmentId) ?: continue
            installmentDao.update(
                inst.copy(
                    paidPrincipalPoisha = (inst.paidPrincipalPoisha - alloc.principalPaidPoisha).coerceAtLeast(0),
                    paidInterestPoisha = (inst.paidInterestPoisha - alloc.interestPaidPoisha).coerceAtLeast(0),
                    paidFeePoisha = (inst.paidFeePoisha - alloc.feePaidPoisha).coerceAtLeast(0),
                    updatedAt = Dates.nowMs()
                )
            )
        }
    }

    // ---------------- Derived balances (read side) ----------------

    fun accountBalance(accountId: Long): Flow<Long> =
        postingDao.observeLedgerBalance(LedgerType.ACCOUNT, accountId)

    fun shopDue(shopId: Long): Flow<Long> =
        postingDao.observeLedgerBalance(LedgerType.SHOP_LIABILITY, shopId).map { -it }

    fun loanOutstanding(loanId: Long): Flow<Long> =
        postingDao.observeLedgerBalance(LedgerType.LOAN_LIABILITY, loanId).map { -it }

    fun receivableOutstanding(loanId: Long): Flow<Long> =
        postingDao.observeLedgerBalance(LedgerType.RECEIVABLE, loanId)

    /** ledgerId → raw signed balance for every ledger of [type]. */
    fun balancesByLedger(type: LedgerType): Flow<Map<Long, Long>> =
        postingDao.observeBalancesByLedger(type).map { rows: List<LedgerBalanceRow> ->
            rows.associate { it.ledgerId to it.totalPoisha }
        }

    fun periodTotal(type: LedgerType, fromMs: Long, toMs: Long): Flow<Long> =
        postingDao.observePeriodTotal(type, fromMs, toMs)

    suspend fun periodTotalOnce(type: LedgerType, fromMs: Long, toMs: Long): Long =
        postingDao.getPeriodTotal(type, fromMs, toMs)

    suspend fun ledgerBalanceOnce(type: LedgerType, ledgerId: Long): Long =
        postingDao.getLedgerBalance(type, ledgerId)

    /** Integrity check shown in Settings: unbalanced transactions must be 0. */
    suspend fun countUnbalanced(): Int = postingDao.countUnbalancedTransactions()
}
