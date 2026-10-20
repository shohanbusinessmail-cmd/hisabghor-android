package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.accounting.PostingFactory
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.SavingsGoalEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.model.SavingsProductType
import com.shohankhan.hisabghor.core.model.TransactionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Savings accounts are ordinary owned accounts (type SAVINGS); deposits and
 * withdrawals are internal transfers — never income or expenses. Goals are a
 * memo allocation tagged onto those transfers (see ACCOUNTING_RULES.md).
 */
class SavingsRepository(
    private val db: HisabGhorDatabase,
    private val ledger: LedgerRepository
) {

    private val goalDao get() = db.savingsGoalDao()

    fun observeGoalsWithProgress(includeArchived: Boolean = false): Flow<List<GoalWithProgress>> =
        combine(
            goalDao.observeAll(includeArchived),
            db.transactionDao().observeAllGoalProgress()
        ) { goals, progressRows ->
            val progress = progressRows.associate { it.ledgerId to it.totalPoisha }
            goals.map { GoalWithProgress(it, progress[it.id] ?: 0L) }
        }

    fun observeGoal(id: Long): Flow<SavingsGoalEntity?> = goalDao.observeById(id)

    fun observeGoalProgress(goalId: Long): Flow<Long> = db.transactionDao().observeGoalProgress(goalId)

    suspend fun getGoal(id: Long): SavingsGoalEntity? = goalDao.getById(id)

    suspend fun createGoal(
        name: String,
        targetPoisha: Long,
        targetDateMs: Long?,
        accountId: Long?,
        productType: SavingsProductType,
        note: String?
    ): Long {
        val now = Dates.nowMs()
        return goalDao.insert(
            SavingsGoalEntity(
                name = name.trim(),
                targetPoisha = targetPoisha,
                targetDate = targetDateMs,
                accountId = accountId,
                productType = productType,
                note = note?.trim()?.ifBlank { null },
                createdAt = now, updatedAt = now
            )
        )
    }

    suspend fun updateGoal(goal: SavingsGoalEntity) {
        goalDao.update(goal.copy(updatedAt = Dates.nowMs()))
    }

    suspend fun setArchived(goal: SavingsGoalEntity, archived: Boolean) {
        goalDao.update(
            goal.copy(
                archivedAt = if (archived) Dates.nowMs() else null,
                updatedAt = Dates.nowMs()
            )
        )
    }

    /** Move money from an owned account into a savings account (optionally goal-tagged). */
    suspend fun deposit(
        fromAccountId: Long,
        savingsAccountId: Long,
        goalId: Long?,
        amountPoisha: Long,
        occurredAt: Long,
        note: String?,
        replacesTransactionId: Long? = null
    ): Long {
        val toAccount = db.accountDao().getById(savingsAccountId)
        val goal = goalId?.let { goalDao.getById(it) }
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.SAVINGS_DEPOSIT,
                occurredAt = occurredAt,
                title = "সঞ্চয়ে জমা — ${goal?.name ?: toAccount?.name ?: ""}",
                note = note?.trim()?.ifBlank { null },
                amountPoisha = amountPoisha,
                accountId = fromAccountId,
                counterAccountId = savingsAccountId,
                savingsGoalId = goalId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.transfer(fromAccountId, savingsAccountId, amountPoisha),
            replacesTransactionId = replacesTransactionId,
            auditSummary = "সঞ্চয়ে জমা: ${goal?.name ?: toAccount?.name}"
        )
    }

    /** Move money back out of a savings account (still not income/expense). */
    suspend fun withdraw(
        savingsAccountId: Long,
        toAccountId: Long,
        goalId: Long?,
        amountPoisha: Long,
        occurredAt: Long,
        note: String?,
        replacesTransactionId: Long? = null
    ): Long {
        val fromAccount = db.accountDao().getById(savingsAccountId)
        val goal = goalId?.let { goalDao.getById(it) }
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.SAVINGS_WITHDRAWAL,
                occurredAt = occurredAt,
                title = "সঞ্চয় থেকে উত্তোলন — ${goal?.name ?: fromAccount?.name ?: ""}",
                note = note?.trim()?.ifBlank { null },
                amountPoisha = amountPoisha,
                accountId = savingsAccountId,
                counterAccountId = toAccountId,
                savingsGoalId = goalId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.transfer(savingsAccountId, toAccountId, amountPoisha),
            replacesTransactionId = replacesTransactionId,
            auditSummary = "সঞ্চয় উত্তোলন: ${goal?.name ?: fromAccount?.name}"
        )
    }
}
