package com.shohankhan.hisabghor.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shohankhan.hisabghor.core.model.BudgetPeriod
import com.shohankhan.hisabghor.core.model.NetWorthItemKind
import com.shohankhan.hisabghor.core.model.RepeatRule
import com.shohankhan.hisabghor.core.model.SavingsProductType

/**
 * A savings goal linked to a real savings account. Progress is the sum of
 * goal-tagged deposits minus withdrawals — a memo allocation inside the
 * account's actual balance, so net worth never double-counts savings.
 */
@Entity(
    tableName = "savings_goals",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("accountId")]
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetPoisha: Long,
    val targetDate: Long? = null,
    val accountId: Long? = null,
    val productType: SavingsProductType = SavingsProductType.HOME_CASH,
    val note: String? = null,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** Spending budget; `categoryId = null` means overall expenses. */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("categoryId")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val amountPoisha: Long,
    val anchorDay: Int = 1,
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

/** User-defined bill/custom reminder (loan & debt reminders are derived). */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("dueDate"), Index("categoryId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountPoisha: Long? = null,
    val dueDate: Long,
    val repeat: RepeatRule = RepeatRule.NONE,
    val categoryId: Long? = null,
    val note: String? = null,
    val enabled: Boolean = true,
    val lastNotifiedAt: Long? = null,
    val doneAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** Rule that creates (or suggests) a recurring income/expense entry. */
@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("nextRunDate"), Index("categoryId"), Index("accountId")]
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isIncome: Boolean,
    val title: String,
    val amountPoisha: Long,
    val categoryId: Long,
    val accountId: Long,
    val frequency: RepeatRule,
    val nextRunDate: Long,
    val endDate: Long? = null,
    val enabled: Boolean = true,
    val autoCreate: Boolean = true,
    val lastRunAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** Manual net-worth item outside the account ledgers (land, gold, deposits...). */
@Entity(tableName = "net_worth_items")
data class NetWorthItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: NetWorthItemKind,
    val valuePoisha: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** Append-only audit trail for sensitive operations (edits, voids, restores...). */
@Entity(tableName = "audit_events", indices = [Index("atEpochMs")])
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atEpochMs: Long,
    val eventType: String,
    val entityType: String,
    val entityId: Long,
    val summary: String
)
