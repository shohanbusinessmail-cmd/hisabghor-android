package com.shohankhan.hisabghor.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The single local database. Version 1 is the initial schema; future changes
 * must ship explicit migrations (never destructive fallbacks) so user data
 * survives upgrades. Schemas are exported to `app/schemas` for migration tests.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        PartyEntity::class,
        ShopEntity::class,
        LoanEntity::class,
        InstallmentEntity::class,
        TransactionEntity::class,
        PostingEntity::class,
        TransactionItemEntity::class,
        InstallmentAllocationEntity::class,
        SavingsGoalEntity::class,
        BudgetEntity::class,
        ReminderEntity::class,
        RecurringRuleEntity::class,
        NetWorthItemEntity::class,
        AttachmentEntity::class,
        AuditEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class HisabGhorDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun postingDao(): PostingDao
    abstract fun partyDao(): PartyDao
    abstract fun shopDao(): ShopDao
    abstract fun loanDao(): LoanDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun budgetDao(): BudgetDao
    abstract fun reminderDao(): ReminderDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun netWorthItemDao(): NetWorthItemDao
    abstract fun auditDao(): AuditDao

    companion object {
        fun create(context: Context): HisabGhorDatabase =
            Room.databaseBuilder(context, HisabGhorDatabase::class.java, "hisabghor.db")
                // No fallbackToDestructiveMigration: upgrades must never wipe data.
                .build()
    }
}
