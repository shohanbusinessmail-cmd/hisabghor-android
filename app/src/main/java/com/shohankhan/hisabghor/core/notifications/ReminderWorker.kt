package com.shohankhan.hisabghor.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shohankhan.hisabghor.HisabGhorApp
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.money.BengaliNumber
import com.shohankhan.hisabghor.core.model.LoanKind
import kotlinx.coroutines.flow.first
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Daily local checks (WorkManager persists across reboots; timing is
 * approximate by design — Android may defer background work):
 *  - materializes due recurring income/expense entries,
 *  - notifies about installments due within the lead window or overdue,
 *  - notifies about personal debt due dates,
 *  - notifies about user bills/reminders (at most once per day each).
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? HisabGhorApp ?: return Result.failure()
        val container = app.container
        val db = container.database
        val now = Dates.nowMs()
        val today = Dates.today()

        container.planningRepository.materializeDueRecurringRules(now)

        val leadDays = container.securityRepository.settings.first().reminderLeadDays
        val horizon = Dates.startOfDayMs(today.plusDays(leadDays.toLong() + 1)) - 1

        // Installments (NGO/bank)
        val dueInstallments = db.installmentDao().getDueUpTo(horizon)
        for (row in dueInstallments) {
            val dueDate = Dates.toLocalDate(row.dueDate)
            val days = ChronoUnit.DAYS.between(today, dueDate)
            // Notify on due-day/lead boundary and when overdue (not every single day in window).
            if (days == leadDays.toLong() || days == 0L || days < 0) {
                val whenText = when {
                    days < 0 -> "${BengaliNumber.formatCount(-days)} দিন আগে বাকি পড়েছে"
                    days == 0L -> "আজই দিতে হবে"
                    else -> "${BengaliNumber.formatCount(days)} দিন পর দিতে হবে"
                }
                NotificationHelper.notifyReminder(
                    applicationContext,
                    notificationId = ("inst" + row.installmentId).hashCode(),
                    title = if (row.loanKind == LoanKind.NGO) "এনজিও কিস্তির অনুস্মারক" else "ব্যাংক কিস্তির অনুস্মারক",
                    body = "${row.organizationName}: ${BengaliNumber.formatPoisha(row.remainingPoisha)} — $whenText (${Dates.formatShortDate(row.dueDate)})"
                )
            }
        }

        // Personal agreements with a single due date
        val personalDue = db.installmentDao().getLoansWithDueDateUpTo(horizon)
            .filter { it.kind == LoanKind.PERSONAL_BORROW || it.kind == LoanKind.PERSONAL_LEND }
        for (loan in personalDue) {
            val outstanding = container.loanRepository.observeOutstanding(loan).first()
            if (outstanding <= 0) continue
            val dueMs = loan.dueDate ?: continue
            val days = ChronoUnit.DAYS.between(today, Dates.toLocalDate(dueMs))
            if (days == leadDays.toLong() || days == 0L || days < 0) {
                val isLend = loan.kind == LoanKind.PERSONAL_LEND
                NotificationHelper.notifyReminder(
                    applicationContext,
                    notificationId = ("loan" + loan.id).hashCode(),
                    title = if (isLend) "পাওনার অনুস্মারক" else "ধার পরিশোধের অনুস্মারক",
                    body = "${loan.organizationName}: ${BengaliNumber.formatPoisha(outstanding)} — তারিখ: ${Dates.formatShortDate(dueMs)}"
                )
            }
        }

        // User bills/reminders: once per day per reminder
        val reminders = db.reminderDao().getEnabledDueUpTo(horizon)
        for (reminder in reminders) {
            val last = reminder.lastNotifiedAt
            if (last != null && Dates.toLocalDate(last) == today) continue
            val days = ChronoUnit.DAYS.between(today, Dates.toLocalDate(reminder.dueDate))
            if (days > 0 && days != leadDays.toLong()) continue
            val amountText = reminder.amountPoisha?.let { " (${BengaliNumber.formatPoisha(it)})" } ?: ""
            NotificationHelper.notifyReminder(
                applicationContext,
                notificationId = ("rem" + reminder.id).hashCode(),
                title = "অনুস্মারক",
                body = "${reminder.title}$amountText — ${Dates.formatShortDate(reminder.dueDate)}"
            )
            db.reminderDao().update(reminder.copy(lastNotifiedAt = now, updatedAt = now))
        }

        return Result.success()
    }
}

object ReminderScheduler {

    private const val WORK_NAME = "hisabghor_daily_reminders"

    fun scheduleDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}
