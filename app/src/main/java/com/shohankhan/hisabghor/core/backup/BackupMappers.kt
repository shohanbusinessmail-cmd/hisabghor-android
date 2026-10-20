package com.shohankhan.hisabghor.core.backup

import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.AttachmentEntity
import com.shohankhan.hisabghor.core.database.BudgetEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.InstallmentAllocationEntity
import com.shohankhan.hisabghor.core.database.InstallmentEntity
import com.shohankhan.hisabghor.core.database.LoanEntity
import com.shohankhan.hisabghor.core.database.NetWorthItemEntity
import com.shohankhan.hisabghor.core.database.PartyEntity
import com.shohankhan.hisabghor.core.database.PostingEntity
import com.shohankhan.hisabghor.core.database.RecurringRuleEntity
import com.shohankhan.hisabghor.core.database.ReminderEntity
import com.shohankhan.hisabghor.core.database.SavingsGoalEntity
import com.shohankhan.hisabghor.core.database.ShopEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.database.TransactionItemEntity
import com.shohankhan.hisabghor.core.model.AccountScope
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.BudgetPeriod
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.InstallmentFrequency
import com.shohankhan.hisabghor.core.model.InterestMethod
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.LoanStatus
import com.shohankhan.hisabghor.core.model.NetWorthItemKind
import com.shohankhan.hisabghor.core.model.RepeatRule
import com.shohankhan.hisabghor.core.model.SavingsProductType
import com.shohankhan.hisabghor.core.model.TransactionKind

/** Entity ↔ backup DTO conversions. Ids are preserved exactly. */

fun AccountEntity.toDto() = AccountDto(
    id, name, type.name, scope.name, note, colorHex, iconKey, sortOrder, archivedAt, createdAt, updatedAt
)

fun AccountDto.toEntity() = AccountEntity(
    id, name, AccountType.valueOf(type), AccountScope.valueOf(scope), note,
    colorHex, iconKey, sortOrder, archivedAt, createdAt, updatedAt
)

fun CategoryEntity.toDto() = CategoryDto(
    id, name, kind.name, iconKey, colorHex, sortOrder, archivedAt, createdAt, updatedAt
)

fun CategoryDto.toEntity() = CategoryEntity(
    id, name, CategoryKind.valueOf(kind), iconKey, colorHex, sortOrder, archivedAt, createdAt, updatedAt
)

fun PartyEntity.toDto() = PartyDto(id, name, phone, address, note, archivedAt, createdAt, updatedAt)

fun PartyDto.toEntity() = PartyEntity(id, name, phone, address, note, archivedAt, createdAt, updatedAt)

fun ShopEntity.toDto() = ShopDto(
    id, name, merchantName, phone, address, shopType, note, archivedAt, createdAt, updatedAt
)

fun ShopDto.toEntity() = ShopEntity(
    id, name, merchantName, phone, address, shopType, note, archivedAt, createdAt, updatedAt
)

fun LoanEntity.toDto() = LoanDto(
    id, kind.name, partyId, organizationName, branch, maskedRef, accountId,
    principalPoisha, disbursedPoisha, totalContractualPoisha, interestRateBp,
    interestMethod.name, frequency.name, installmentCount, installmentAmountPoisha,
    startDate, firstDueDate, dueDate, purpose, note, status.name, createdAt, updatedAt
)

fun LoanDto.toEntity() = LoanEntity(
    id, LoanKind.valueOf(kind), partyId, organizationName, branch, maskedRef, accountId,
    principalPoisha, disbursedPoisha, totalContractualPoisha, interestRateBp,
    InterestMethod.valueOf(interestMethod), InstallmentFrequency.valueOf(frequency),
    installmentCount, installmentAmountPoisha, startDate, firstDueDate, dueDate,
    purpose, note, LoanStatus.valueOf(status), createdAt, updatedAt
)

fun InstallmentEntity.toDto() = InstallmentDto(
    id, loanId, seq, dueDate, amountPoisha, principalPartPoisha, interestPartPoisha,
    feePartPoisha, paidPrincipalPoisha, paidInterestPoisha, paidFeePoisha, createdAt, updatedAt
)

fun InstallmentDto.toEntity() = InstallmentEntity(
    id, loanId, seq, dueDate, amountPoisha, principalPartPoisha, interestPartPoisha,
    feePartPoisha, paidPrincipalPoisha, paidInterestPoisha, paidFeePoisha, createdAt, updatedAt
)

fun TransactionEntity.toDto() = TransactionDto(
    id, kind.name, occurredAt, title, note, amountPoisha, feePoisha, discountPoisha,
    extraChargePoisha, categoryId, accountId, counterAccountId, partyId, shopId,
    loanId, savingsGoalId, receiptNo, voidedAt, replacesTransactionId, createdAt, updatedAt
)

fun TransactionDto.toEntity() = TransactionEntity(
    id, TransactionKind.valueOf(kind), occurredAt, title, note, amountPoisha, feePoisha,
    discountPoisha, extraChargePoisha, categoryId, accountId, counterAccountId, partyId,
    shopId, loanId, savingsGoalId, receiptNo, voidedAt, replacesTransactionId, createdAt, updatedAt
)

fun PostingEntity.toDto() = PostingDto(id, transactionId, ledgerType.name, ledgerId, signedAmountPoisha)

fun PostingDto.toEntity() = PostingEntity(
    id, transactionId, LedgerType.valueOf(ledgerType), ledgerId, signedAmountPoisha
)

fun TransactionItemEntity.toDto() = TransactionItemDto(
    id, transactionId, name, quantityMilli, unit, unitPricePoisha, discountPoisha, lineTotalPoisha
)

fun TransactionItemDto.toEntity() = TransactionItemEntity(
    id, transactionId, name, quantityMilli, unit, unitPricePoisha, discountPoisha, lineTotalPoisha
)

fun InstallmentAllocationEntity.toDto() = InstallmentAllocationDto(
    id, transactionId, installmentId, principalPaidPoisha, interestPaidPoisha, feePaidPoisha
)

fun InstallmentAllocationDto.toEntity() = InstallmentAllocationEntity(
    id, transactionId, installmentId, principalPaidPoisha, interestPaidPoisha, feePaidPoisha
)

fun SavingsGoalEntity.toDto() = SavingsGoalDto(
    id, name, targetPoisha, targetDate, accountId, productType.name, note, archivedAt, createdAt, updatedAt
)

fun SavingsGoalDto.toEntity() = SavingsGoalEntity(
    id, name, targetPoisha, targetDate, accountId, SavingsProductType.valueOf(productType),
    note, archivedAt, createdAt, updatedAt
)

fun BudgetEntity.toDto() = BudgetDto(
    id, categoryId, period.name, amountPoisha, anchorDay, active, createdAt, updatedAt
)

fun BudgetDto.toEntity() = BudgetEntity(
    id, categoryId, BudgetPeriod.valueOf(period), amountPoisha, anchorDay, active, createdAt, updatedAt
)

fun ReminderEntity.toDto() = ReminderDto(
    id, title, amountPoisha, dueDate, repeat.name, categoryId, note, enabled,
    lastNotifiedAt, doneAt, createdAt, updatedAt
)

fun ReminderDto.toEntity() = ReminderEntity(
    id, title, amountPoisha, dueDate, RepeatRule.valueOf(repeat), categoryId, note,
    enabled, lastNotifiedAt, doneAt, createdAt, updatedAt
)

fun RecurringRuleEntity.toDto() = RecurringRuleDto(
    id, isIncome, title, amountPoisha, categoryId, accountId, frequency.name,
    nextRunDate, endDate, enabled, autoCreate, lastRunAt, createdAt, updatedAt
)

fun RecurringRuleDto.toEntity() = RecurringRuleEntity(
    id, isIncome, title, amountPoisha, categoryId, accountId, RepeatRule.valueOf(frequency),
    nextRunDate, endDate, enabled, autoCreate, lastRunAt, createdAt, updatedAt
)

fun NetWorthItemEntity.toDto() = NetWorthItemDto(id, name, kind.name, valuePoisha, note, createdAt, updatedAt)

fun NetWorthItemDto.toEntity() = NetWorthItemEntity(
    id, name, NetWorthItemKind.valueOf(kind), valuePoisha, note, createdAt, updatedAt
)

fun AttachmentEntity.toDto(contentB64: String?) = AttachmentDto(
    id, transactionId, fileName, mimeType, sizeBytes, createdAt, contentB64
)

fun AttachmentDto.toEntity() = AttachmentEntity(
    id, transactionId, fileName, mimeType, sizeBytes, createdAt
)

fun com.shohankhan.hisabghor.core.database.AuditEventEntity.toDto() = AuditEventDto(
    id, atEpochMs, eventType, entityType, entityId, summary
)

fun AuditEventDto.toEntity() = com.shohankhan.hisabghor.core.database.AuditEventEntity(
    id, atEpochMs, eventType, entityType, entityId, summary
)
