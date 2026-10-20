package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.BudgetEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.NetWorthItemEntity
import com.shohankhan.hisabghor.core.database.RecurringRuleEntity
import com.shohankhan.hisabghor.core.database.ReminderEntity
import com.shohankhan.hisabghor.core.model.BudgetPeriod
import com.shohankhan.hisabghor.core.model.RepeatRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Budgets, reminders, recurring rules and manual net-worth items. */
class PlanningRepository(
    private val db: HisabGhorDatabase,
    private val transactions: TransactionRepository
) {

    // ---------------- budgets ----------------

    /** Current period [from, to) for a budget, in Asia/Dhaka. */
    fun currentBudgetRange(budget: BudgetEntity, today: LocalDate = Dates.today()): Pair<Long, Long> =
        when (budget.period) {
            BudgetPeriod.DAILY ->
                Dates.startOfDayMs(today) to Dates.startOfDayMs(today.plusDays(1))
            BudgetPeriod.WEEKLY -> {
                // Week starts on Saturday in Bangladesh (weekend Fri–Sat context:
                // most BD weeks are counted Saturday→Friday); documented choice.
                val dow = today.dayOfWeek.value // Mon=1..Sun=7
                val daysSinceSaturday = (dow + 1) % 7 // Sat=6 → 0
                val start = today.minusDays(daysSinceSaturday.toLong())
                Dates.startOfDayMs(start) to Dates.startOfDayMs(start.plusDays(7))
            }
            BudgetPeriod.MONTHLY -> {
                val anchor = budget.anchorDay.coerceIn(1, 28)
                var start = today.withDayOfMonth(anchor)
                if (today.dayOfMonth < anchor) start = start.minusMonths(1)
                Dates.startOfDayMs(start) to Dates.startOfDayMs(start.plusMonths(1))
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeBudgetsWithUsage(): Flow<List<BudgetWithUsage>> =
        combine(db.budgetDao().observeAll(), db.categoryDao().observeAll(true)) { budgets, categories ->
            budgets to categories.associateBy { it.id }
        }.flatMapLatest { (budgets, categoryById) ->
            if (budgets.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = budgets.map { budget ->
                    val (from, to) = currentBudgetRange(budget)
                    db.postingDao().observeExpenseForBudget(budget.categoryId, from, to)
                        .map { spent ->
                            BudgetWithUsage(budget, categoryById[budget.categoryId], spent, from, to)
                        }
                }
                combine(flows) { it.toList() }
            }
        }

    suspend fun addBudget(categoryId: Long?, period: BudgetPeriod, amountPoisha: Long, anchorDay: Int): Long {
        val now = Dates.nowMs()
        return db.budgetDao().insert(
            BudgetEntity(
                categoryId = categoryId, period = period, amountPoisha = amountPoisha,
                anchorDay = anchorDay.coerceIn(1, 28), createdAt = now, updatedAt = now
            )
        )
    }

    suspend fun updateBudget(budget: BudgetEntity) =
        db.budgetDao().update(budget.copy(updatedAt = Dates.nowMs()))

    suspend fun deleteBudget(budget: BudgetEntity) = db.budgetDao().delete(budget)

    fun observeBudgets(): Flow<List<BudgetEntity>> = db.budgetDao().observeAll()

    // ---------------- reminders ----------------

    fun observeReminders(): Flow<List<ReminderEntity>> = db.reminderDao().observeAll()

    suspend fun addReminder(reminder: ReminderEntity): Long = db.reminderDao().insert(reminder)

    suspend fun updateReminder(reminder: ReminderEntity) =
        db.reminderDao().update(reminder.copy(updatedAt = Dates.nowMs()))

    suspend fun deleteReminder(reminder: ReminderEntity) = db.reminderDao().delete(reminder)

    /** Marks a reminder done; repeating reminders roll to the next occurrence. */
    suspend fun markReminderDone(reminder: ReminderEntity) {
        val now = Dates.nowMs()
        if (reminder.repeat == RepeatRule.NONE) {
            db.reminderDao().update(reminder.copy(doneAt = now, updatedAt = now))
        } else {
            val next = advanceDate(reminder.dueDate, reminder.repeat)
            db.reminderDao().update(
                reminder.copy(dueDate = next, lastNotifiedAt = null, updatedAt = now)
            )
        }
    }

    // ---------------- recurring rules ----------------

    fun observeRecurringRules(): Flow<List<RecurringRuleEntity>> = db.recurringRuleDao().observeAll()

    suspend fun addRecurringRule(rule: RecurringRuleEntity): Long = db.recurringRuleDao().insert(rule)

    suspend fun updateRecurringRule(rule: RecurringRuleEntity) =
        db.recurringRuleDao().update(rule.copy(updatedAt = Dates.nowMs()))

    suspend fun deleteRecurringRule(rule: RecurringRuleEntity) = db.recurringRuleDao().delete(rule)

    /**
     * Creates transactions for all due auto-create rules (catching up missed
     * periods, capped to avoid runaway loops) and advances their next run.
     * Called by the daily worker and on app start. Returns how many entries
     * were created.
     */
    suspend fun materializeDueRecurringRules(nowMs: Long = Dates.nowMs()): Int {
        var created = 0
        val due = db.recurringRuleDao().getDueEnabled(nowMs)
        for (rule in due) {
            if (!rule.autoCreate) continue
            var nextRun = rule.nextRunDate
            var guard = 0
            var lastRule = rule
            while (nextRun <= nowMs && guard < 60) {
                if (rule.endDate != null && nextRun > rule.endDate) break
                if (rule.isIncome) {
                    transactions.addIncome(
                        accountId = rule.accountId, categoryId = rule.categoryId,
                        amountPoisha = rule.amountPoisha, occurredAt = nextRun,
                        title = rule.title, note = "স্বয়ংক্রিয় (পুনরাবৃত্ত)"
                    )
                } else {
                    transactions.addExpense(
                        accountId = rule.accountId, categoryId = rule.categoryId,
                        amountPoisha = rule.amountPoisha, items = emptyList(),
                        extraChargePoisha = 0, discountPoisha = 0,
                        occurredAt = nextRun, title = rule.title,
                        note = "স্বয়ংক্রিয় (পুনরাবৃত্ত)"
                    )
                }
                created++
                nextRun = advanceDate(nextRun, rule.frequency)
                guard++
            }
            val disabled = rule.endDate != null && nextRun > rule.endDate
            lastRule = rule.copy(
                nextRunDate = nextRun,
                lastRunAt = nowMs,
                enabled = rule.enabled && !disabled,
                updatedAt = nowMs
            )
            db.recurringRuleDao().update(lastRule)
        }
        return created
    }

    fun advanceDate(fromMs: Long, rule: RepeatRule): Long {
        val date = Dates.toLocalDate(fromMs)
        val next = when (rule) {
            RepeatRule.DAILY -> date.plusDays(1)
            RepeatRule.WEEKLY -> date.plusWeeks(1)
            RepeatRule.MONTHLY -> date.plusMonths(1)
            RepeatRule.YEARLY -> date.plusYears(1)
            RepeatRule.NONE -> date
        }
        return Dates.startOfDayMs(next)
    }

    // ---------------- manual net-worth items ----------------

    fun observeNetWorthItems(): Flow<List<NetWorthItemEntity>> = db.netWorthItemDao().observeAll()

    fun observeManualAssetTotal(): Flow<Long> = db.netWorthItemDao().observeAssetTotal()

    fun observeManualLiabilityTotal(): Flow<Long> = db.netWorthItemDao().observeLiabilityTotal()

    suspend fun addNetWorthItem(item: NetWorthItemEntity): Long = db.netWorthItemDao().insert(item)

    suspend fun updateNetWorthItem(item: NetWorthItemEntity) =
        db.netWorthItemDao().update(item.copy(updatedAt = Dates.nowMs()))

    suspend fun deleteNetWorthItem(item: NetWorthItemEntity) = db.netWorthItemDao().delete(item)
}
