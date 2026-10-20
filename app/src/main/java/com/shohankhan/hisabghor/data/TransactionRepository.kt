package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.accounting.PostingFactory
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.database.TransactionItemEntity
import com.shohankhan.hisabghor.core.model.TransactionKind
import kotlinx.coroutines.flow.Flow

/** Itemized bill line coming from the UI (quantities already in thousandths). */
data class ItemDraft(
    val name: String,
    val quantityMilli: Long,
    val unit: String?,
    val unitPricePoisha: Long,
    val discountPoisha: Long
) {
    /** round(qty × unit price / 1000) − discount, never negative. */
    val lineTotalPoisha: Long
        get() {
            val gross = (Math.multiplyExact(quantityMilli, unitPricePoisha) + 500) / 1000
            return (gross - discountPoisha).coerceAtLeast(0)
        }

    fun toEntity() = TransactionItemEntity(
        transactionId = 0,
        name = name.trim(),
        quantityMilli = quantityMilli,
        unit = unit?.trim()?.ifBlank { null },
        unitPricePoisha = unitPricePoisha,
        discountPoisha = discountPoisha,
        lineTotalPoisha = lineTotalPoisha
    )
}

/**
 * Income/expense/refund recording and transaction history.
 * Itemized totals rule: amount = Σ line totals + extra charges − overall
 * discount; the expense posting always equals that amount exactly.
 */
class TransactionRepository(
    private val db: HisabGhorDatabase,
    private val ledger: LedgerRepository
) {

    private val txnDao get() = db.transactionDao()

    suspend fun addIncome(
        accountId: Long,
        categoryId: Long,
        amountPoisha: Long,
        occurredAt: Long,
        title: String,
        note: String?,
        partyId: Long? = null,
        replacesTransactionId: Long? = null
    ): Long {
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.INCOME,
                occurredAt = occurredAt,
                title = title.trim(),
                note = note?.trim()?.ifBlank { null },
                amountPoisha = amountPoisha,
                categoryId = categoryId,
                accountId = accountId,
                partyId = partyId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.income(accountId, categoryId, amountPoisha),
            replacesTransactionId = replacesTransactionId,
            auditSummary = "আয়: ${title.trim()}"
        )
    }

    suspend fun addExpense(
        accountId: Long,
        categoryId: Long,
        amountPoisha: Long,
        items: List<ItemDraft>,
        extraChargePoisha: Long,
        discountPoisha: Long,
        occurredAt: Long,
        title: String,
        note: String?,
        shopId: Long? = null,
        replacesTransactionId: Long? = null
    ): Long {
        // When items exist they define the total; otherwise the typed amount does.
        val total = if (items.isNotEmpty()) {
            (items.sumOf { it.lineTotalPoisha } + extraChargePoisha - discountPoisha).coerceAtLeast(0)
        } else amountPoisha
        require(total > 0) { "expense total must be positive" }
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.EXPENSE,
                occurredAt = occurredAt,
                title = title.trim(),
                note = note?.trim()?.ifBlank { null },
                amountPoisha = total,
                discountPoisha = discountPoisha,
                extraChargePoisha = extraChargePoisha,
                categoryId = categoryId,
                accountId = accountId,
                shopId = shopId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.expense(accountId, categoryId, total),
            items = items.map { it.toEntity() },
            replacesTransactionId = replacesTransactionId,
            auditSummary = "খরচ: ${title.trim()}"
        )
    }

    suspend fun addExpenseRefund(
        accountId: Long,
        categoryId: Long,
        amountPoisha: Long,
        occurredAt: Long,
        title: String,
        note: String?,
        replacesTransactionId: Long? = null
    ): Long {
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.EXPENSE_REFUND,
                occurredAt = occurredAt,
                title = title.trim(),
                note = note?.trim()?.ifBlank { null },
                amountPoisha = amountPoisha,
                categoryId = categoryId,
                accountId = accountId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.expenseRefund(accountId, categoryId, amountPoisha),
            replacesTransactionId = replacesTransactionId,
            auditSummary = "রিফান্ড: ${title.trim()}"
        )
    }

    suspend fun voidTransaction(id: Long, titleForAudit: String) {
        ledger.voidTransaction(id, "লেনদেন বাতিল: $titleForAudit")
    }

    suspend fun getById(id: Long): TransactionEntity? = txnDao.getById(id)

    suspend fun getItems(transactionId: Long): List<TransactionItemEntity> =
        txnDao.getItems(transactionId)

    fun observeItems(transactionId: Long): Flow<List<TransactionItemEntity>> =
        txnDao.observeItems(transactionId)

    fun observeById(id: Long): Flow<TransactionEntity?> = txnDao.observeById(id)

    fun observeRecent(limit: Int = 10): Flow<List<TransactionEntity>> = txnDao.observeRecent(limit)

    // ---------------- attachments (receipt files) ----------------

    fun observeAttachments(transactionId: Long): Flow<List<com.shohankhan.hisabghor.core.database.AttachmentEntity>> =
        txnDao.observeAttachments(transactionId)

    suspend fun addAttachment(
        transactionId: Long,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long
    ): Long = txnDao.insertAttachment(
        com.shohankhan.hisabghor.core.database.AttachmentEntity(
            transactionId = transactionId,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            createdAt = Dates.nowMs()
        )
    )

    suspend fun deleteAttachment(id: Long) = txnDao.deleteAttachment(id)

    fun observeFiltered(
        kind: TransactionKind? = null,
        categoryId: Long? = null,
        accountId: Long? = null,
        shopId: Long? = null,
        loanId: Long? = null,
        partyId: Long? = null,
        fromMs: Long? = null,
        toMs: Long? = null,
        minAmount: Long? = null,
        maxAmount: Long? = null,
        q: String = ""
    ): Flow<List<TransactionEntity>> = txnDao.observeFiltered(
        kind, categoryId, accountId, shopId, loanId, partyId,
        fromMs, toMs, minAmount, maxAmount, q
    )
}
