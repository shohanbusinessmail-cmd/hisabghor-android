package com.shohankhan.hisabghor.core.money

import com.shohankhan.hisabghor.core.model.InstallmentFrequency
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

/** One row of a generated repayment schedule (an estimate, user-editable). */
data class ScheduledInstallment(
    val seq: Int,
    val dueDate: LocalDate,
    val amountPoisha: Long,
    val principalPoisha: Long,
    val interestPoisha: Long
)

/**
 * Loan arithmetic. All inputs/outputs are integer poisha; intermediate rate
 * math uses [BigDecimal] with explicit HALF_UP rounding to the nearest poisha.
 *
 * Rules (see docs/ACCOUNTING_RULES.md):
 *  - Periodic rate r = annual rate ÷ periods per year (12 monthly, 52 weekly).
 *  - Amortizing EMI = P·r·(1+r)ⁿ / ((1+r)ⁿ − 1); zero-rate → P ÷ n.
 *  - Per-period interest = round(outstanding principal × r).
 *  - The final installment always absorbs rounding remainders so schedule
 *    sums equal the contractual totals exactly.
 *  - Flat (NGO-style): total interest = contractual total − principal, spread
 *    evenly; final installment absorbs remainders.
 *
 * Generated schedules are estimates only — never a lender's official figures.
 */
object LoanMath {

    private val MC = MathContext(20, RoundingMode.HALF_UP)

    fun periodsPerYear(frequency: InstallmentFrequency): Int = when (frequency) {
        InstallmentFrequency.WEEKLY -> 52
        InstallmentFrequency.MONTHLY -> 12
        InstallmentFrequency.NONE -> 12
    }

    /** Annual rate in basis points (1250 = 12.50%) → periodic rate. */
    fun periodicRate(annualRateBp: Int, frequency: InstallmentFrequency): BigDecimal {
        require(annualRateBp >= 0) { "rate must be >= 0" }
        val denom = BigDecimal(10_000L * periodsPerYear(frequency))
        return BigDecimal(annualRateBp).divide(denom, 14, RoundingMode.HALF_UP)
    }

    /** Equal installment (EMI) in poisha for an amortizing loan. */
    fun emiPoisha(
        principalPoisha: Long,
        annualRateBp: Int,
        installments: Int,
        frequency: InstallmentFrequency
    ): Long {
        require(principalPoisha > 0) { "principal must be positive" }
        require(installments > 0) { "installments must be positive" }
        val r = periodicRate(annualRateBp, frequency)
        if (r.signum() == 0) {
            return BigDecimal(principalPoisha)
                .divide(BigDecimal(installments), 0, RoundingMode.HALF_UP)
                .toLong()
        }
        val p = BigDecimal(principalPoisha)
        val onePlusRpowN = (BigDecimal.ONE + r).pow(installments, MC)
        val emi = p.multiply(r, MC).multiply(onePlusRpowN, MC)
            .divide(onePlusRpowN - BigDecimal.ONE, MC)
        return emi.setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /** Splits [total] into [n] near-equal parts; the last part takes the remainder. */
    fun splitEvenly(total: Long, n: Int): List<Long> {
        require(n > 0) { "n must be positive" }
        require(total >= 0) { "total must be >= 0" }
        val base = total / n
        val remainder = total - base * n
        return List(n) { i -> if (i == n - 1) base + remainder else base }
    }

    fun nextDueDate(from: LocalDate, frequency: InstallmentFrequency, steps: Int): LocalDate =
        when (frequency) {
            InstallmentFrequency.WEEKLY -> from.plusWeeks(steps.toLong())
            InstallmentFrequency.MONTHLY, InstallmentFrequency.NONE -> from.plusMonths(steps.toLong())
        }

    /**
     * Reducing-balance amortization schedule (bank loans).
     * Final installment is adjusted so principal sums exactly to [principalPoisha].
     */
    fun amortizedSchedule(
        principalPoisha: Long,
        annualRateBp: Int,
        installments: Int,
        firstDueDate: LocalDate,
        frequency: InstallmentFrequency
    ): List<ScheduledInstallment> {
        val r = periodicRate(annualRateBp, frequency)
        val emi = emiPoisha(principalPoisha, annualRateBp, installments, frequency)
        var balance = principalPoisha
        val result = ArrayList<ScheduledInstallment>(installments)
        for (i in 1..installments) {
            val interest = BigDecimal(balance).multiply(r, MC)
                .setScale(0, RoundingMode.HALF_UP).toLong()
            val principalPart: Long
            val amount: Long
            if (i == installments || emi - interest >= balance) {
                principalPart = balance
                amount = balance + interest
            } else {
                principalPart = emi - interest
                require(principalPart > 0) { "EMI does not cover interest; increase installment or check rate" }
                amount = emi
            }
            balance -= principalPart
            result.add(
                ScheduledInstallment(
                    seq = i,
                    dueDate = nextDueDate(firstDueDate, frequency, i - 1),
                    amountPoisha = amount,
                    principalPoisha = principalPart,
                    interestPoisha = interest
                )
            )
            if (balance == 0L) break
        }
        return result
    }

    /**
     * Flat schedule (typical NGO/microfinance): principal and total interest
     * are each spread evenly across installments.
     */
    fun flatSchedule(
        principalPoisha: Long,
        totalContractualPoisha: Long,
        installments: Int,
        firstDueDate: LocalDate,
        frequency: InstallmentFrequency
    ): List<ScheduledInstallment> {
        require(totalContractualPoisha >= principalPoisha) {
            "total contractual must be >= principal"
        }
        val interestTotal = totalContractualPoisha - principalPoisha
        val principalParts = splitEvenly(principalPoisha, installments)
        val interestParts = splitEvenly(interestTotal, installments)
        return (1..installments).map { i ->
            val p = principalParts[i - 1]
            val interest = interestParts[i - 1]
            ScheduledInstallment(
                seq = i,
                dueDate = nextDueDate(firstDueDate, frequency, i - 1),
                amountPoisha = p + interest,
                principalPoisha = p,
                interestPoisha = interest
            )
        }
    }

    /**
     * Schedule when only the equal installment amount is known (e.g. user
     * knows "৳১,০৫০ করে ৪৬ কিস্তি" but not the exact principal/interest split).
     * Principal is spread evenly; interest is derived from the installment
     * amount. The last installment absorbs remainders.
     */
    fun fixedInstallmentSchedule(
        principalPoisha: Long,
        installmentAmountPoisha: Long,
        installments: Int,
        firstDueDate: LocalDate,
        frequency: InstallmentFrequency
    ): List<ScheduledInstallment> {
        val total = Math.multiplyExact(installmentAmountPoisha, installments.toLong())
        return flatSchedule(principalPoisha, maxOf(total, principalPoisha), installments, firstDueDate, frequency)
    }

    /** Simple interest estimate for personal agreements: P × rate × days/365. */
    fun simpleInterestPoisha(principalPoisha: Long, annualRateBp: Int, days: Long): Long {
        require(days >= 0) { "days must be >= 0" }
        if (annualRateBp == 0 || days == 0L || principalPoisha == 0L) return 0L
        return BigDecimal(principalPoisha)
            .multiply(BigDecimal(annualRateBp), MC)
            .multiply(BigDecimal(days), MC)
            .divide(BigDecimal(10_000L * 365L), MC)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}
