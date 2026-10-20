package com.shohankhan.hisabghor.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisabghor.core.model.TransactionKind
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertPostings(postings: List<PostingEntity>)

    @Insert
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Insert
    suspend fun insertAllocations(allocations: List<InstallmentAllocationEntity>)

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET voidedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun voidTransaction(id: Long, at: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query(
        "SELECT * FROM transactions WHERE voidedAt IS NULL ORDER BY occurredAt DESC, id DESC LIMIT :limit"
    )
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    /**
     * Filterable transaction history. Every filter is optional; pass null/empty
     * to ignore it. `q` matches title and note.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE voidedAt IS NULL
          AND (:kind IS NULL OR kind = :kind)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:accountId IS NULL OR accountId = :accountId OR counterAccountId = :accountId)
          AND (:shopId IS NULL OR shopId = :shopId)
          AND (:loanId IS NULL OR loanId = :loanId)
          AND (:partyId IS NULL OR partyId = :partyId)
          AND (:fromMs IS NULL OR occurredAt >= :fromMs)
          AND (:toMs IS NULL OR occurredAt < :toMs)
          AND (:minAmount IS NULL OR amountPoisha >= :minAmount)
          AND (:maxAmount IS NULL OR amountPoisha <= :maxAmount)
          AND (:q = '' OR title LIKE '%' || :q || '%' OR note LIKE '%' || :q || '%')
        ORDER BY occurredAt DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeFiltered(
        kind: TransactionKind?,
        categoryId: Long?,
        accountId: Long?,
        shopId: Long?,
        loanId: Long?,
        partyId: Long?,
        fromMs: Long?,
        toMs: Long?,
        minAmount: Long?,
        maxAmount: Long?,
        q: String,
        limit: Int = 500
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE voidedAt IS NULL AND (title LIKE '%' || :q || '%' OR note LIKE '%' || :q || '%')
        ORDER BY occurredAt DESC, id DESC LIMIT 50
        """
    )
    suspend fun search(q: String): List<TransactionEntity>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id")
    suspend fun getItems(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id")
    fun observeItems(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM installment_allocations WHERE transactionId = :transactionId")
    suspend fun getAllocations(transactionId: Long): List<InstallmentAllocationEntity>

    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId ORDER BY id")
    fun observeAttachments(transactionId: Long): Flow<List<AttachmentEntity>>

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachment(id: Long)

    /** Savings-goal progress: tagged deposits minus tagged withdrawals. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN kind = 'SAVINGS_DEPOSIT' THEN amountPoisha ELSE -amountPoisha END), 0)
        FROM transactions
        WHERE savingsGoalId = :goalId AND voidedAt IS NULL
          AND kind IN ('SAVINGS_DEPOSIT', 'SAVINGS_WITHDRAWAL')
        """
    )
    fun observeGoalProgress(goalId: Long): Flow<Long>

    @Query(
        """
        SELECT savingsGoalId AS ledgerId,
               COALESCE(SUM(CASE WHEN kind = 'SAVINGS_DEPOSIT' THEN amountPoisha ELSE -amountPoisha END), 0) AS totalPoisha
        FROM transactions
        WHERE savingsGoalId IS NOT NULL AND voidedAt IS NULL
          AND kind IN ('SAVINGS_DEPOSIT', 'SAVINGS_WITHDRAWAL')
        GROUP BY savingsGoalId
        """
    )
    fun observeAllGoalProgress(): Flow<List<LedgerBalanceRow>>

    // ---- full-table reads for backup export (includes voided history) ----
    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun getAllForBackup(): List<TransactionEntity>

    @Query("SELECT * FROM postings ORDER BY id")
    suspend fun getAllPostingsForBackup(): List<PostingEntity>

    @Query("SELECT * FROM transaction_items ORDER BY id")
    suspend fun getAllItemsForBackup(): List<TransactionItemEntity>

    @Query("SELECT * FROM installment_allocations ORDER BY id")
    suspend fun getAllAllocationsForBackup(): List<InstallmentAllocationEntity>

    @Query("SELECT * FROM attachments ORDER BY id")
    suspend fun getAllAttachmentsForBackup(): List<AttachmentEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE voidedAt IS NULL")
    fun observeActiveCount(): Flow<Int>

    /** The live opening-balance transaction of an account, if any. */
    @Query(
        "SELECT * FROM transactions WHERE kind = 'OPENING_BALANCE' AND accountId = :accountId " +
            "AND voidedAt IS NULL ORDER BY id DESC LIMIT 1"
    )
    suspend fun getOpeningTransaction(accountId: Long): TransactionEntity?

    /** Total cash moved by live transactions of [kind] for a loan (repaid / returned totals). */
    @Query(
        "SELECT COALESCE(SUM(amountPoisha), 0) FROM transactions " +
            "WHERE loanId = :loanId AND kind = :kind AND voidedAt IS NULL"
    )
    fun observeLoanCashTotal(loanId: Long, kind: TransactionKind): Flow<Long>
}
