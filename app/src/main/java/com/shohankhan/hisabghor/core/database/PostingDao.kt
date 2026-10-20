package com.shohankhan.hisabghor.core.database

import androidx.room.Dao
import androidx.room.Query
import com.shohankhan.hisabghor.core.model.LedgerType
import kotlinx.coroutines.flow.Flow

/**
 * Every balance and total in the app is computed here from journal postings
 * of non-voided transactions — the SQL twin of [com.shohankhan.hisabghor.core.accounting.LedgerCalculator].
 * Raw signed sums are returned; callers negate for income/liability display.
 */
@Dao
interface PostingDao {

    /** Signed balance of one ledger (e.g. one account). */
    @Query(
        """
        SELECT COALESCE(SUM(p.signedAmountPoisha), 0) FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType = :ledgerType AND p.ledgerId = :ledgerId
        """
    )
    fun observeLedgerBalance(ledgerType: LedgerType, ledgerId: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(p.signedAmountPoisha), 0) FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType = :ledgerType AND p.ledgerId = :ledgerId
        """
    )
    suspend fun getLedgerBalance(ledgerType: LedgerType, ledgerId: Long): Long

    /** Signed balances for every ledger of a type (all accounts, all shops...). */
    @Query(
        """
        SELECT p.ledgerId AS ledgerId, COALESCE(SUM(p.signedAmountPoisha), 0) AS totalPoisha
        FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType = :ledgerType
        GROUP BY p.ledgerId
        """
    )
    fun observeBalancesByLedger(ledgerType: LedgerType): Flow<List<LedgerBalanceRow>>

    /** Raw signed sum over a period [fromMs, toMs) for a ledger type. */
    @Query(
        """
        SELECT COALESCE(SUM(p.signedAmountPoisha), 0) FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType = :ledgerType AND t.occurredAt >= :fromMs AND t.occurredAt < :toMs
        """
    )
    fun observePeriodTotal(ledgerType: LedgerType, fromMs: Long, toMs: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(p.signedAmountPoisha), 0) FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType = :ledgerType AND t.occurredAt >= :fromMs AND t.occurredAt < :toMs
        """
    )
    suspend fun getPeriodTotal(ledgerType: LedgerType, fromMs: Long, toMs: Long): Long

    /** Per-category signed totals in a period (charts & reports). */
    @Query(
        """
        SELECT p.ledgerId AS ledgerId, COALESCE(SUM(p.signedAmountPoisha), 0) AS totalPoisha
        FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType = :ledgerType AND t.occurredAt >= :fromMs AND t.occurredAt < :toMs
        GROUP BY p.ledgerId
        """
    )
    fun observePeriodTotalsByLedger(
        ledgerType: LedgerType,
        fromMs: Long,
        toMs: Long
    ): Flow<List<LedgerBalanceRow>>

    /** Budget usage: expense postings for one category (or all if categoryId null) in a period. */
    @Query(
        """
        SELECT COALESCE(SUM(p.signedAmountPoisha), 0) FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType = 'EXPENSE_CATEGORY'
          AND (:categoryId IS NULL OR p.ledgerId = :categoryId)
          AND t.occurredAt >= :fromMs AND t.occurredAt < :toMs
        """
    )
    fun observeExpenseForBudget(categoryId: Long?, fromMs: Long, toMs: Long): Flow<Long>

    /** Account statement lines, oldest first (running balance computed by caller). */
    @Query(
        """
        SELECT t.id AS transactionId, t.occurredAt AS occurredAt, t.title AS title,
               t.kind AS kind, p.signedAmountPoisha AS signedAmountPoisha
        FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType = 'ACCOUNT' AND p.ledgerId = :accountId
        ORDER BY t.occurredAt ASC, t.id ASC
        """
    )
    fun observeAccountLedger(accountId: Long): Flow<List<AccountLedgerRow>>

    @Query(
        """
        SELECT * FROM postings WHERE transactionId = :transactionId ORDER BY id
        """
    )
    suspend fun getPostingsForTransaction(transactionId: Long): List<PostingEntity>

    /** Category usage guard (safe deletion): postings in live transactions. */
    @Query(
        """
        SELECT COUNT(*) FROM postings p
        INNER JOIN transactions t ON t.id = p.transactionId AND t.voidedAt IS NULL
        WHERE p.ledgerType IN ('INCOME_CATEGORY', 'EXPENSE_CATEGORY') AND p.ledgerId = :categoryId
        """
    )
    suspend fun countLiveCategoryPostings(categoryId: Long): Int

    /** Integrity check: number of transactions whose postings do not sum to 0. Must be 0. */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT transactionId FROM postings GROUP BY transactionId
            HAVING SUM(signedAmountPoisha) != 0
        )
        """
    )
    suspend fun countUnbalancedTransactions(): Int

    /** Whole-ledger trial balance. Must be 0 in a healthy database. */
    @Query("SELECT COALESCE(SUM(signedAmountPoisha), 0) FROM postings")
    suspend fun trialBalance(): Long
}
