package com.shohankhan.hisabghor.core.backup

import kotlinx.serialization.Serializable

/**
 * Versioned backup format (see docs/BACKUP_FORMAT.md).
 *
 * A backup file is a JSON [BackupEnvelope]. The payload is either embedded
 * encrypted (normal backups) or exported as plain JSON (explicit export).
 * Enum values are stored as names so newer app versions can migrate older
 * files deliberately instead of failing unpredictably.
 */
@Serializable
data class BackupEnvelope(
    val format: String = FORMAT,
    val version: Int = 1,
    val kdf: String = "PBKDF2-HMAC-SHA256",
    val kdfIterations: Int,
    val saltB64: String,
    val cipher: String = "AES-256-GCM",
    val ivB64: String,
    val dataB64: String
) {
    companion object {
        const val FORMAT = "hisabghor-backup"
    }
}

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val appVersion: String,
    val createdAtEpochMs: Long,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val parties: List<PartyDto> = emptyList(),
    val shops: List<ShopDto> = emptyList(),
    val loans: List<LoanDto> = emptyList(),
    val installments: List<InstallmentDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val postings: List<PostingDto> = emptyList(),
    val transactionItems: List<TransactionItemDto> = emptyList(),
    val installmentAllocations: List<InstallmentAllocationDto> = emptyList(),
    val savingsGoals: List<SavingsGoalDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    val recurringRules: List<RecurringRuleDto> = emptyList(),
    val netWorthItems: List<NetWorthItemDto> = emptyList(),
    val attachments: List<AttachmentDto> = emptyList(),
    val auditEvents: List<AuditEventDto> = emptyList()
)

@Serializable
data class AccountDto(
    val id: Long, val name: String, val type: String, val scope: String,
    val note: String? = null, val colorHex: Long, val iconKey: String,
    val sortOrder: Int, val archivedAt: Long? = null,
    val createdAt: Long, val updatedAt: Long
)

@Serializable
data class CategoryDto(
    val id: Long, val name: String, val kind: String, val iconKey: String,
    val colorHex: Long, val sortOrder: Int, val archivedAt: Long? = null,
    val createdAt: Long, val updatedAt: Long
)

@Serializable
data class PartyDto(
    val id: Long, val name: String, val phone: String? = null,
    val address: String? = null, val note: String? = null,
    val archivedAt: Long? = null, val createdAt: Long, val updatedAt: Long
)

@Serializable
data class ShopDto(
    val id: Long, val name: String, val merchantName: String? = null,
    val phone: String? = null, val address: String? = null,
    val shopType: String? = null, val note: String? = null,
    val archivedAt: Long? = null, val createdAt: Long, val updatedAt: Long
)

@Serializable
data class LoanDto(
    val id: Long, val kind: String, val partyId: Long? = null,
    val organizationName: String, val branch: String? = null,
    val maskedRef: String? = null, val accountId: Long? = null,
    val principalPoisha: Long, val disbursedPoisha: Long,
    val totalContractualPoisha: Long? = null, val interestRateBp: Int,
    val interestMethod: String, val frequency: String,
    val installmentCount: Int, val installmentAmountPoisha: Long? = null,
    val startDate: Long, val firstDueDate: Long? = null, val dueDate: Long? = null,
    val purpose: String? = null, val note: String? = null, val status: String,
    val createdAt: Long, val updatedAt: Long
)

@Serializable
data class InstallmentDto(
    val id: Long, val loanId: Long, val seq: Int, val dueDate: Long,
    val amountPoisha: Long, val principalPartPoisha: Long,
    val interestPartPoisha: Long, val feePartPoisha: Long,
    val paidPrincipalPoisha: Long, val paidInterestPoisha: Long,
    val paidFeePoisha: Long, val createdAt: Long, val updatedAt: Long
)

@Serializable
data class TransactionDto(
    val id: Long, val kind: String, val occurredAt: Long, val title: String,
    val note: String? = null, val amountPoisha: Long, val feePoisha: Long,
    val discountPoisha: Long, val extraChargePoisha: Long,
    val categoryId: Long? = null, val accountId: Long? = null,
    val counterAccountId: Long? = null, val partyId: Long? = null,
    val shopId: Long? = null, val loanId: Long? = null,
    val savingsGoalId: Long? = null, val receiptNo: String? = null,
    val voidedAt: Long? = null, val replacesTransactionId: Long? = null,
    val createdAt: Long, val updatedAt: Long
)

@Serializable
data class PostingDto(
    val id: Long, val transactionId: Long, val ledgerType: String,
    val ledgerId: Long, val signedAmountPoisha: Long
)

@Serializable
data class TransactionItemDto(
    val id: Long, val transactionId: Long, val name: String,
    val quantityMilli: Long, val unit: String? = null,
    val unitPricePoisha: Long, val discountPoisha: Long, val lineTotalPoisha: Long
)

@Serializable
data class InstallmentAllocationDto(
    val id: Long, val transactionId: Long, val installmentId: Long,
    val principalPaidPoisha: Long, val interestPaidPoisha: Long, val feePaidPoisha: Long
)

@Serializable
data class SavingsGoalDto(
    val id: Long, val name: String, val targetPoisha: Long,
    val targetDate: Long? = null, val accountId: Long? = null,
    val productType: String, val note: String? = null,
    val archivedAt: Long? = null, val createdAt: Long, val updatedAt: Long
)

@Serializable
data class BudgetDto(
    val id: Long, val categoryId: Long? = null, val period: String,
    val amountPoisha: Long, val anchorDay: Int, val active: Boolean,
    val createdAt: Long, val updatedAt: Long
)

@Serializable
data class ReminderDto(
    val id: Long, val title: String, val amountPoisha: Long? = null,
    val dueDate: Long, val repeat: String, val categoryId: Long? = null,
    val note: String? = null, val enabled: Boolean,
    val lastNotifiedAt: Long? = null, val doneAt: Long? = null,
    val createdAt: Long, val updatedAt: Long
)

@Serializable
data class RecurringRuleDto(
    val id: Long, val isIncome: Boolean, val title: String,
    val amountPoisha: Long, val categoryId: Long, val accountId: Long,
    val frequency: String, val nextRunDate: Long, val endDate: Long? = null,
    val enabled: Boolean, val autoCreate: Boolean, val lastRunAt: Long? = null,
    val createdAt: Long, val updatedAt: Long
)

@Serializable
data class NetWorthItemDto(
    val id: Long, val name: String, val kind: String, val valuePoisha: Long,
    val note: String? = null, val createdAt: Long, val updatedAt: Long
)

@Serializable
data class AuditEventDto(
    val id: Long, val atEpochMs: Long, val eventType: String,
    val entityType: String, val entityId: Long, val summary: String
)

/** Receipt metadata + optional file bytes (size-capped at export time). */
@Serializable
data class AttachmentDto(
    val id: Long, val transactionId: Long, val fileName: String,
    val mimeType: String? = null, val sizeBytes: Long, val createdAt: Long,
    val contentB64: String? = null
)
