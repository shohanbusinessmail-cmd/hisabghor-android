package com.shohankhan.hisabghor.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE (:includeArchived OR archivedAt IS NULL) ORDER BY createdAt")
    fun observeAll(includeArchived: Boolean = false): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun observeById(id: Long): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals ORDER BY id")
    suspend fun getAllForBackup(): List<SavingsGoalEntity>

    @Insert
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY createdAt")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY id")
    suspend fun getAllForBackup(): List<BudgetEntity>

    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY dueDate")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND doneAt IS NULL AND dueDate <= :toMs ORDER BY dueDate")
    suspend fun getEnabledDueUpTo(toMs: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY id")
    suspend fun getAllForBackup(): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)
}

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules ORDER BY nextRunDate")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE enabled = 1 AND nextRunDate <= :nowMs ORDER BY nextRunDate")
    suspend fun getDueEnabled(nowMs: Long): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules ORDER BY id")
    suspend fun getAllForBackup(): List<RecurringRuleEntity>

    @Insert
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)
}

@Dao
interface NetWorthItemDao {
    @Query("SELECT * FROM net_worth_items ORDER BY kind, name")
    fun observeAll(): Flow<List<NetWorthItemEntity>>

    @Query("SELECT COALESCE(SUM(valuePoisha), 0) FROM net_worth_items WHERE kind = 'ASSET'")
    fun observeAssetTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(valuePoisha), 0) FROM net_worth_items WHERE kind = 'LIABILITY'")
    fun observeLiabilityTotal(): Flow<Long>

    @Query("SELECT * FROM net_worth_items ORDER BY id")
    suspend fun getAllForBackup(): List<NetWorthItemEntity>

    @Insert
    suspend fun insert(item: NetWorthItemEntity): Long

    @Update
    suspend fun update(item: NetWorthItemEntity)

    @Delete
    suspend fun delete(item: NetWorthItemEntity)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_events ORDER BY atEpochMs DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events ORDER BY id")
    suspend fun getAllForBackup(): List<AuditEventEntity>

    @Insert
    suspend fun insert(event: AuditEventEntity): Long
}
