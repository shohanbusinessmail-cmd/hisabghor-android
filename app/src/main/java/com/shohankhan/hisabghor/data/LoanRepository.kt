package com.shohankhan.hisabghor.data

import androidx.room.withTransaction
import com.shohankhan.hisabghor.core.accounting.InstallmentAllocator
import com.shohankhan.hisabghor.core.accounting.InstallmentDue
import com.shohankhan.hisabghor.core.accounting.PostingFactory
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.InstallmentAllocationEntity
import com.shohankhan.hisabghor.core.database.InstallmentEntity
import com.shohankhan.hisabghor.core.database.LoanEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.InstallmentFrequency
import com.shohankhan.hisabghor.core.model.InterestMethod
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.LoanStatus
import com.shohankhan.hisabghor.core.model.TransactionKind
import com.shohankhan.hisabghor.core.money.LoanMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * All debt agreements: personal borrow/lend, NGO loans, bank loans.
 *
 * The liability ledger always tracks principal only; interest/fees become
 * expenses when paid. Schedules are estimates the user can reconcile against
 * lender statements without rewriting recorded payments.
 */
class LoanRepository(
    private val db: HisabGhorDatabase,
    private val ledger: LedgerRepository,
    private val categories: CategoryRepository
) {

    private val loanDao get() = db.loanDao()
    private val installmentDao get() = db.installmentDao()

    // ---------------- reads ----------------

    fun observeLoansWithBalances(kindFilter: LoanKind? = null): Flow<List<LoanWithBalance>> {
        val loansFlow = if (kindFilter == null) loanDao.observeAll() else loanDao.observeByKind(kindFilter)
        return combine(
            loansFlow,
            ledger.balancesByLedger(LedgerType.LOAN_LIABILITY),
            ledger.balancesByLedger(LedgerType.RECEIVABLE)
        ) { loans, liabilities, receivables ->
            loans.map { loan ->
                val outstanding = if (loan.kind == LoanKind.PERSONAL_LEND) {
                    receivables[loan.id] ?: 0L
                } else {
                    -(liabilities[loan.id] ?: 0L)
                }
                LoanWithBalance(loan, outstanding)
            }
        }
    }

    fun observeTotalPayables(): Flow<Long> =
        ledger.balancesByLedger(LedgerType.LOAN_LIABILITY).map { -it.values.sum() }

    fun observeLoan(id: Long): Flow<LoanEntity?> = loanDao.observeById(id)

    fun observeOutstanding(loan: LoanEntity): Flow<Long> =
        if (loan.kind == LoanKind.PERSONAL_LEND) ledger.receivableOutstanding(loan.id)
        else ledger.loanOutstanding(loan.id)

    fun observeInstallments(loanId: Long): Flow<List<InstallmentEntity>> =
        installmentDao.observeForLoan(loanId)

    fun observeTotalCashRepaid(loanId: Long, kind: LoanKind): Flow<Long> =
        db.transactionDao().observeLoanCashTotal(
            loanId,
            if (kind == LoanKind.PERSONAL_LEND) TransactionKind.LEND_RETURN else TransactionKind.LOAN_REPAYMENT
        )

    suspend fun getLoan(id: Long): LoanEntity? = loanDao.getById(id)

    // ---------------- creation ----------------

    /** Personal borrow or lend agreement (no installment schedule). */
    suspend fun createPersonalAgreement(
        kind: LoanKind,
        partyId: Long?,
        displayName: String,
        accountId: Long?,
        principalPoisha: Long,
        startDateMs: Long,
        dueDateMs: Long?,
        interestRateBp: Int,
        purpose: String?,
        note: String?,
        moneyMovedNow: Boolean
    ): Long {
        require(kind == LoanKind.PERSONAL_BORROW || kind == LoanKind.PERSONAL_LEND)
        if (moneyMovedNow) require(accountId != null) { "account required when money moves now" }
        val now = Dates.nowMs()
        // One DB transaction for loan row + opening journal entry (Room
        // withTransaction is reentrant, so ledger.record joins it).
        return db.withTransaction {
            val loanId = loanDao.insert(
                LoanEntity(
                    kind = kind,
                    partyId = partyId,
                    organizationName = displayName.trim(),
                    accountId = accountId,
                    principalPoisha = principalPoisha,
                    disbursedPoisha = principalPoisha,
                    interestRateBp = interestRateBp,
                    interestMethod = if (interestRateBp > 0) InterestMethod.SIMPLE else InterestMethod.NONE,
                    startDate = startDateMs,
                    dueDate = dueDateMs,
                    purpose = purpose?.trim()?.ifBlank { null },
                    note = note?.trim()?.ifBlank { null },
                    createdAt = now, updatedAt = now
                )
            )
            val (txnKind, title, postings) = when {
                kind == LoanKind.PERSONAL_BORROW && moneyMovedNow -> Triple(
                    TransactionKind.LOAN_RECEIVE, "ধার নেওয়া — ${displayName.trim()}",
                    PostingFactory.loanReceive(loanId, accountId!!, principalPoisha)
                )
                kind == LoanKind.PERSONAL_BORROW -> Triple(
                    TransactionKind.OPENING_BALANCE, "আগের ধার — ${displayName.trim()}",
                    PostingFactory.openingLoanLiability(loanId, principalPoisha)
                )
                kind == LoanKind.PERSONAL_LEND && moneyMovedNow -> Triple(
                    TransactionKind.LEND_OUT, "ধার দেওয়া — ${displayName.trim()}",
                    PostingFactory.lendOut(loanId, accountId!!, principalPoisha)
                )
                else -> Triple(
                    TransactionKind.OPENING_BALANCE, "আগের পাওনা — ${displayName.trim()}",
                    PostingFactory.openingReceivable(loanId, principalPoisha)
                )
            }
            ledger.record(
                transaction = TransactionEntity(
                    kind = txnKind,
                    occurredAt = startDateMs,
                    title = title,
                    amountPoisha = principalPoisha,
                    accountId = if (moneyMovedNow) accountId else null,
                    partyId = partyId,
                    loanId = loanId,
                    createdAt = now, updatedAt = now
                ),
                postings = postings,
                auditSummary = title
            )
            loanId
        }
    }

    /** NGO or bank loan with a generated installment schedule (estimate). */
    suspend fun createInstallmentLoan(
        kind: LoanKind,
        organizationName: String,
        branch: String?,
        maskedRef: String?,
        accountId: Long?,
        principalPoisha: Long,
        disbursedPoisha: Long,
        interestMethod: InterestMethod,
        interestRateBp: Int,
        totalContractualPoisha: Long?,
        installmentAmountPoisha: Long?,
        installmentCount: Int,
        frequency: InstallmentFrequency,
        startDateMs: Long,
        firstDueDateMs: Long,
        note: String?,
        moneyMovedNow: Boolean
    ): Long {
        require(kind == LoanKind.NGO || kind == LoanKind.BANK)
        require(installmentCount > 0) { "installment count required" }
        if (moneyMovedNow) require(accountId != null) { "account required when money moves now" }
        val now = Dates.nowMs()
        return db.withTransaction {
            val loanId = loanDao.insert(
                LoanEntity(
                    kind = kind,
                    organizationName = organizationName.trim(),
                    branch = branch?.trim()?.ifBlank { null },
                    maskedRef = maskedRef?.trim()?.ifBlank { null },
                    accountId = accountId,
                    principalPoisha = principalPoisha,
                    disbursedPoisha = disbursedPoisha,
                    totalContractualPoisha = totalContractualPoisha,
                    interestRateBp = interestRateBp,
                    interestMethod = interestMethod,
                    frequency = frequency,
                    installmentCount = installmentCount,
                    installmentAmountPoisha = installmentAmountPoisha,
                    startDate = startDateMs,
                    firstDueDate = firstDueDateMs,
                    note = note?.trim()?.ifBlank { null },
                    createdAt = now, updatedAt = now
                )
            )
            val schedule = buildSchedule(
                principalPoisha, interestMethod, interestRateBp,
                totalContractualPoisha, installmentAmountPoisha,
                installmentCount, Dates.toLocalDate(firstDueDateMs), frequency
            )
            installmentDao.insertAll(schedule.map { s ->
                InstallmentEntity(
                    loanId = loanId,
                    seq = s.seq,
                    dueDate = Dates.startOfDayMs(s.dueDate),
                    amountPoisha = s.amountPoisha,
                    principalPartPoisha = s.principalPoisha,
                    interestPartPoisha = s.interestPoisha,
                    createdAt = now, updatedAt = now
                )
            })

            val title = "ঋণ গ্রহণ — ${organizationName.trim()}"
            val postings = if (moneyMovedNow) {
                val chargeCategoryId = if (disbursedPoisha < principalPoisha) {
                    categories.findOrCreate(
                        CategoryRepository.NAME_LOAN_INTEREST, CategoryKind.EXPENSE, "loan_interest"
                    )
                } else null
                PostingFactory.loanReceiveWithUpfrontCharge(
                    loanId, accountId!!, principalPoisha, disbursedPoisha, chargeCategoryId
                )
            } else {
                PostingFactory.openingLoanLiability(loanId, principalPoisha)
            }
            ledger.record(
                transaction = TransactionEntity(
                    kind = if (moneyMovedNow) TransactionKind.LOAN_RECEIVE else TransactionKind.OPENING_BALANCE,
                    occurredAt = startDateMs,
                    title = title,
                    amountPoisha = principalPoisha,
                    accountId = if (moneyMovedNow) accountId else null,
                    loanId = loanId,
                    createdAt = now, updatedAt = now
                ),
                postings = postings,
                auditSummary = title
            )
            loanId
        }
    }

    /** Schedule chosen by interest method; all variants keep exact poisha sums. */
    private fun buildSchedule(
        principalPoisha: Long,
        method: InterestMethod,
        interestRateBp: Int,
        totalContractualPoisha: Long?,
        installmentAmountPoisha: Long?,
        count: Int,
        firstDue: java.time.LocalDate,
        frequency: InstallmentFrequency
    ) = when (method) {
        InterestMethod.REDUCING_AMORTIZED ->
            LoanMath.amortizedSchedule(principalPoisha, interestRateBp, count, firstDue, frequency)
        InterestMethod.FLAT -> when {
            totalContractualPoisha != null ->
                LoanMath.flatSchedule(principalPoisha, totalContractualPoisha, count, firstDue, frequency)
            installmentAmountPoisha != null ->
                LoanMath.fixedInstallmentSchedule(principalPoisha, installmentAmountPoisha, count, firstDue, frequency)
            else -> {
                // Flat interest from the annual rate over the loan term.
                val ppy = LoanMath.periodsPerYear(frequency)
                val interest = BigDecimal(principalPoisha)
                    .multiply(BigDecimal(interestRateBp))
                    .multiply(BigDecimal(count))
                    .divide(BigDecimal(10_000L * ppy), 0, RoundingMode.HALF_UP)
                    .toLong()
                LoanMath.flatSchedule(principalPoisha, principalPoisha + interest, count, firstDue, frequency)
            }
        }
        InterestMethod.SIMPLE, InterestMethod.NONE ->
            LoanMath.flatSchedule(principalPoisha, maxOf(totalContractualPoisha ?: principalPoisha, principalPoisha), count, firstDue, frequency)
    }

    // ---------------- repayments ----------------

    /**
     * Records a repayment against a borrow/NGO/bank loan.
     *
     * With a schedule: [totalAmountPoisha] (minus the extra [feePoisha]) is
     * allocated oldest-first (interest → fee → principal per installment).
     * Any leftover beyond the whole schedule is extra principal, and the
     * schedule tail is shrunk accordingly (early settlement cancels the
     * remaining interest of fully-cleared tail installments).
     * Without a schedule the caller provides [manualPrincipalPoisha] and
     * [manualInterestPoisha]. Editing re-runs allocation against dues with the
     * replaced payment added back, so figures stay exact.
     */
    suspend fun recordRepayment(
        loanId: Long,
        accountId: Long,
        totalAmountPoisha: Long,
        occurredAt: Long,
        interestCategoryId: Long?,
        feePoisha: Long,
        feeCategoryId: Long?,
        receiptNo: String?,
        note: String?,
        manualPrincipalPoisha: Long? = null,
        manualInterestPoisha: Long? = null,
        replacesTransactionId: Long? = null
    ): Long {
        val loan = loanDao.getById(loanId) ?: throw IllegalArgumentException("loan not found")
        require(totalAmountPoisha > 0) { "amount must be positive" }
        val installments = installmentDao.getForLoan(loanId)

        val oldAllocations = if (replacesTransactionId != null) {
            db.transactionDao().getAllocations(replacesTransactionId).associateBy { it.installmentId }
        } else emptyMap()

        var principalPaid = 0L
        var interestPaid = 0L
        var scheduleFeePaid = 0L
        var leftover = 0L
        var allocationEntities: List<InstallmentAllocationEntity> = emptyList()

        if (installments.isNotEmpty() && manualPrincipalPoisha == null) {
            val dues = installments.map { inst ->
                val old = oldAllocations[inst.id]
                InstallmentDue(
                    installmentId = inst.id,
                    seq = inst.seq,
                    principalDuePoisha = inst.principalPartPoisha - inst.paidPrincipalPoisha + (old?.principalPaidPoisha ?: 0),
                    interestDuePoisha = inst.interestPartPoisha - inst.paidInterestPoisha + (old?.interestPaidPoisha ?: 0),
                    feeDuePoisha = inst.feePartPoisha - inst.paidFeePoisha + (old?.feePaidPoisha ?: 0)
                )
            }
            val forSchedule = totalAmountPoisha - feePoisha
            require(forSchedule > 0) { "repayment must exceed the extra fee" }
            val result = InstallmentAllocator.allocate(dues, forSchedule)
            principalPaid = result.totalPrincipalPoisha + result.leftoverPoisha
            interestPaid = result.totalInterestPoisha
            scheduleFeePaid = result.totalFeePoisha
            leftover = result.leftoverPoisha
            allocationEntities = result.allocations.map {
                InstallmentAllocationEntity(
                    transactionId = 0,
                    installmentId = it.installmentId,
                    principalPaidPoisha = it.principalPaidPoisha,
                    interestPaidPoisha = it.interestPaidPoisha,
                    feePaidPoisha = it.feePaidPoisha
                )
            }
        } else {
            principalPaid = manualPrincipalPoisha ?: (totalAmountPoisha - feePoisha)
            interestPaid = manualInterestPoisha ?: 0L
            require(principalPaid + interestPaid + feePoisha == totalAmountPoisha) {
                "principal + interest + fee must equal the paid total"
            }
        }

        val resolvedInterestCategory = if (interestPaid > 0) {
            interestCategoryId ?: categories.findOrCreate(
                CategoryRepository.NAME_LOAN_INTEREST, CategoryKind.EXPENSE, "loan_interest"
            )
        } else interestCategoryId
        val totalFee = feePoisha + scheduleFeePaid
        val resolvedFeeCategory = if (totalFee > 0) {
            feeCategoryId ?: resolvedInterestCategory ?: categories.findOrCreate(
                CategoryRepository.NAME_LOAN_INTEREST, CategoryKind.EXPENSE, "loan_interest"
            )
        } else feeCategoryId

        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.LOAN_REPAYMENT,
                occurredAt = occurredAt,
                title = "কিস্তি/ঋণ পরিশোধ — ${loan.organizationName}",
                note = note?.trim()?.ifBlank { null },
                amountPoisha = totalAmountPoisha,
                feePoisha = totalFee,
                accountId = accountId,
                partyId = loan.partyId,
                loanId = loanId,
                receiptNo = receiptNo?.trim()?.ifBlank { null },
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.loanRepayment(
                loanId, accountId, principalPaid, interestPaid,
                resolvedInterestCategory, totalFee, resolvedFeeCategory
            ),
            allocations = allocationEntities,
            replacesTransactionId = replacesTransactionId,
            auditSummary = "ঋণ পরিশোধ: ${loan.organizationName}"
        ) {
            if (leftover > 0) shrinkScheduleTail(loanId, leftover)
            closeIfSettled(loanId)
        }
    }

    /** Money returned by someone the user lent to. Principal is not income. */
    suspend fun recordLendReturn(
        loanId: Long,
        accountId: Long,
        principalPoisha: Long,
        interestPoisha: Long,
        interestCategoryId: Long?,
        occurredAt: Long,
        note: String?,
        replacesTransactionId: Long? = null
    ): Long {
        val loan = loanDao.getById(loanId) ?: throw IllegalArgumentException("loan not found")
        val resolvedInterest = if (interestPoisha > 0) {
            interestCategoryId ?: categories.findOrCreate(
                CategoryRepository.NAME_SAVINGS_PROFIT, CategoryKind.INCOME, "savings_profit"
            )
        } else interestCategoryId
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.LEND_RETURN,
                occurredAt = occurredAt,
                title = "ধারের টাকা ফেরত — ${loan.organizationName}",
                note = note?.trim()?.ifBlank { null },
                amountPoisha = principalPoisha + interestPoisha,
                accountId = accountId,
                partyId = loan.partyId,
                loanId = loanId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.lendReturn(loanId, accountId, principalPoisha, interestPoisha, resolvedInterest),
            replacesTransactionId = replacesTransactionId,
            auditSummary = "পাওনা ফেরত: ${loan.organizationName}"
        ) { closeIfSettled(loanId) }
    }

    /** Additional principal on an existing personal agreement (same agreement). */
    suspend fun recordAdditionalPrincipal(
        loanId: Long,
        accountId: Long,
        amountPoisha: Long,
        occurredAt: Long,
        note: String?
    ): Long {
        val loan = loanDao.getById(loanId) ?: throw IllegalArgumentException("loan not found")
        val now = Dates.nowMs()
        val isLend = loan.kind == LoanKind.PERSONAL_LEND
        val txnId = ledger.record(
            transaction = TransactionEntity(
                kind = if (isLend) TransactionKind.LEND_OUT else TransactionKind.LOAN_RECEIVE,
                occurredAt = occurredAt,
                title = if (isLend) "আবার ধার দেওয়া — ${loan.organizationName}" else "আবার ধার নেওয়া — ${loan.organizationName}",
                note = note?.trim()?.ifBlank { null },
                amountPoisha = amountPoisha,
                accountId = accountId,
                partyId = loan.partyId,
                loanId = loanId,
                createdAt = now, updatedAt = now
            ),
            postings = if (isLend) {
                PostingFactory.lendOut(loanId, accountId, amountPoisha)
            } else {
                PostingFactory.loanReceive(loanId, accountId, amountPoisha)
            },
            auditSummary = "অতিরিক্ত মূলধন: ${loan.organizationName}"
        )
        loanDao.update(
            loan.copy(
                principalPoisha = loan.principalPoisha + amountPoisha,
                disbursedPoisha = loan.disbursedPoisha + amountPoisha,
                status = LoanStatus.ACTIVE,
                updatedAt = Dates.nowMs()
            )
        )
        return txnId
    }

    // ---------------- maintenance ----------------

    suspend fun updateLoan(loan: LoanEntity) {
        loanDao.update(loan.copy(updatedAt = Dates.nowMs()))
    }

    suspend fun setLoanStatus(loanId: Long, status: LoanStatus) {
        val loan = loanDao.getById(loanId) ?: return
        loanDao.update(loan.copy(status = status, updatedAt = Dates.nowMs()))
    }

    /**
     * Regenerates a schedule from new terms. Refused once any payment exists —
     * in that case the user edits individual installments (reconciliation),
     * which never rewrites recorded payments.
     */
    suspend fun regenerateSchedule(loanId: Long): Boolean {
        val loan = loanDao.getById(loanId) ?: return false
        val installments = installmentDao.getForLoan(loanId)
        if (installments.any { it.paidTotalPoisha > 0 }) return false
        val firstDue = loan.firstDueDate ?: return false
        val now = Dates.nowMs()
        db.withTransaction {
            installmentDao.deleteUnpaidForLoan(loanId)
            val schedule = buildSchedule(
                loan.principalPoisha, loan.interestMethod, loan.interestRateBp,
                loan.totalContractualPoisha, loan.installmentAmountPoisha,
                loan.installmentCount, Dates.toLocalDate(firstDue), loan.frequency
            )
            installmentDao.insertAll(schedule.map { s ->
                InstallmentEntity(
                    loanId = loanId, seq = s.seq,
                    dueDate = Dates.startOfDayMs(s.dueDate),
                    amountPoisha = s.amountPoisha,
                    principalPartPoisha = s.principalPoisha,
                    interestPartPoisha = s.interestPoisha,
                    createdAt = now, updatedAt = now
                )
            })
        }
        return true
    }

    /** Statement reconciliation: user-corrects one installment's scheduled parts. */
    suspend fun updateInstallment(installment: InstallmentEntity) {
        installmentDao.update(installment.copy(updatedAt = Dates.nowMs()))
    }

    /** Deletes a loan only when it has no live transactions (history stays safe). */
    suspend fun deleteLoanIfUnused(loanId: Long): Boolean {
        val live = db.transactionDao().observeFiltered(
            kind = null, categoryId = null, accountId = null, shopId = null,
            loanId = loanId, partyId = null, fromMs = null, toMs = null,
            minAmount = null, maxAmount = null, q = ""
        ).first()
        if (live.isNotEmpty()) return false
        val loan = loanDao.getById(loanId) ?: return false
        loanDao.delete(loan) // installments cascade
        return true
    }

    // ---------------- internals (run inside ledger.record's DB transaction) ----------------

    private suspend fun shrinkScheduleTail(loanId: Long, extraPrincipalPoisha: Long) {
        var remaining = extraPrincipalPoisha
        val installments = installmentDao.getForLoan(loanId).sortedByDescending { it.seq }
        for (inst in installments) {
            if (remaining == 0L) break
            val principalDue = inst.principalPartPoisha - inst.paidPrincipalPoisha
            if (principalDue <= 0) continue
            val cut = minOf(remaining, principalDue)
            val newPrincipalPart = inst.principalPartPoisha - cut
            val fullyCleared = newPrincipalPart == inst.paidPrincipalPoisha &&
                inst.paidInterestPoisha == 0L && cut == principalDue
            val newInterestPart = if (fullyCleared) inst.paidInterestPoisha else inst.interestPartPoisha
            val interestCut = inst.interestPartPoisha - newInterestPart
            installmentDao.update(
                inst.copy(
                    principalPartPoisha = newPrincipalPart,
                    interestPartPoisha = newInterestPart,
                    amountPoisha = inst.amountPoisha - cut - interestCut,
                    updatedAt = Dates.nowMs()
                )
            )
            remaining -= cut
        }
    }

    private suspend fun closeIfSettled(loanId: Long) {
        val loan = loanDao.getById(loanId) ?: return
        val outstanding = if (loan.kind == LoanKind.PERSONAL_LEND) {
            ledger.ledgerBalanceOnce(LedgerType.RECEIVABLE, loanId)
        } else {
            -ledger.ledgerBalanceOnce(LedgerType.LOAN_LIABILITY, loanId)
        }
        if (outstanding == 0L && loan.status == LoanStatus.ACTIVE) {
            loanDao.update(loan.copy(status = LoanStatus.CLOSED, updatedAt = Dates.nowMs()))
        } else if (outstanding > 0 && loan.status == LoanStatus.CLOSED) {
            loanDao.update(loan.copy(status = LoanStatus.ACTIVE, updatedAt = Dates.nowMs()))
        }
    }
}
