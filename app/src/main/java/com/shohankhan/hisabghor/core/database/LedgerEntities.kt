package com.shohankhan.hisabghor.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shohankhan.hisabghor.core.model.InstallmentFrequency
import com.shohankhan.hisabghor.core.model.InterestMethod
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.LoanStatus

/** A person involved in lending/borrowing (distinct agreements stay separate). */
@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val note: String? = null,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** A shop/merchant with its own credit ledger (SHOP_LIABILITY postings). */
@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val merchantName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val shopType: String? = null,
    val note: String? = null,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * One debt agreement: personal borrow, personal lending, NGO loan or bank
 * loan. Principal outstanding is derived from LOAN_LIABILITY / RECEIVABLE
 * postings; scheduled amounts live in [InstallmentEntity] rows.
 */
@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(PartyEntity::class, ["id"], ["partyId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("partyId"), Index("accountId"), Index("kind"), Index("status")]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: LoanKind,
    val partyId: Long? = null,
    val organizationName: String,
    val branch: String? = null,
    val maskedRef: String? = null,
    val accountId: Long? = null,
    val principalPoisha: Long,
    val disbursedPoisha: Long,
    val totalContractualPoisha: Long? = null,
    val interestRateBp: Int = 0,
    val interestMethod: InterestMethod = InterestMethod.NONE,
    val frequency: InstallmentFrequency = InstallmentFrequency.NONE,
    val installmentCount: Int = 0,
    val installmentAmountPoisha: Long? = null,
    val startDate: Long,
    val firstDueDate: Long? = null,
    val dueDate: Long? = null,
    val purpose: String? = null,
    val note: String? = null,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * One scheduled installment. `paid*` columns track allocations from actual
 * repayment transactions; status (upcoming/partial/paid/overdue) is derived.
 * Users may edit amounts to reconcile with a lender statement — recorded
 * payments (postings) are never rewritten by such edits.
 */
@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(LoanEntity::class, ["id"], ["loanId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("loanId"), Index(value = ["loanId", "seq"], unique = true), Index("dueDate")]
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val seq: Int,
    val dueDate: Long,
    val amountPoisha: Long,
    val principalPartPoisha: Long,
    val interestPartPoisha: Long,
    val feePartPoisha: Long = 0,
    val paidPrincipalPoisha: Long = 0,
    val paidInterestPoisha: Long = 0,
    val paidFeePoisha: Long = 0,
    val createdAt: Long,
    val updatedAt: Long
) {
    val paidTotalPoisha: Long get() = paidPrincipalPoisha + paidInterestPoisha + paidFeePoisha
    val remainingPoisha: Long get() = amountPoisha - paidTotalPoisha
}

/** Which repayment transaction paid which installment (audit + safe edits). */
@Entity(
    tableName = "installment_allocations",
    foreignKeys = [
        ForeignKey(TransactionEntity::class, ["id"], ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(InstallmentEntity::class, ["id"], ["installmentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("transactionId"), Index("installmentId")]
)
data class InstallmentAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val installmentId: Long,
    val principalPaidPoisha: Long,
    val interestPaidPoisha: Long,
    val feePaidPoisha: Long
)
