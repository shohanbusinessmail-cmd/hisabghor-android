package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.BudgetEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.LoanEntity
import com.shohankhan.hisabghor.core.database.PartyEntity
import com.shohankhan.hisabghor.core.database.SavingsGoalEntity
import com.shohankhan.hisabghor.core.database.ShopEntity

/** Combined read models exposed to ViewModels (entities + derived balances). */

data class AccountWithBalance(
    val account: AccountEntity,
    val balancePoisha: Long
)

data class ShopWithDue(
    val shop: ShopEntity,
    val duePoisha: Long
)

data class LoanWithBalance(
    val loan: LoanEntity,
    /** Principal outstanding (borrow/NGO/bank) or receivable outstanding (lend). */
    val outstandingPoisha: Long
)

data class PartySummary(
    val party: PartyEntity,
    /** Total they owe the user (receivables). */
    val receivablePoisha: Long,
    /** Total the user owes them (payables). */
    val payablePoisha: Long,
    val activeAgreements: Int
) {
    /** Positive → they owe the user overall; negative → user owes them. */
    val netPoisha: Long get() = receivablePoisha - payablePoisha
}

data class GoalWithProgress(
    val goal: SavingsGoalEntity,
    val savedPoisha: Long
) {
    val remainingPoisha: Long get() = (goal.targetPoisha - savedPoisha).coerceAtLeast(0)
    val progressFraction: Float
        get() = if (goal.targetPoisha <= 0) 0f
        else (savedPoisha.toDouble() / goal.targetPoisha.toDouble()).toFloat().coerceIn(0f, 1f)
}

data class BudgetWithUsage(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val spentPoisha: Long,
    val periodFromMs: Long,
    val periodToMs: Long
) {
    val remainingPoisha: Long get() = budget.amountPoisha - spentPoisha
    val usageFraction: Float
        get() = if (budget.amountPoisha <= 0) 0f
        else (spentPoisha.toDouble() / budget.amountPoisha.toDouble()).toFloat().coerceAtLeast(0f)
    val isOverBudget: Boolean get() = spentPoisha > budget.amountPoisha
}
