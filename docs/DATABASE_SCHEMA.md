# HisabGhor — Database Schema (Room / SQLite, version 1)

Conventions: all money columns are `INTEGER` poisha (`Long`). All timestamps
are epoch milliseconds (`Long`) in UTC, displayed in Asia/Dhaka. Dates for
due-days are stored as epoch milliseconds at local midnight. Every table has
`createdAt`/`updatedAt` unless noted. Foreign keys are enforced
(`PRAGMA foreign_keys=ON` via Room). No destructive migrations — future schema
changes ship explicit `Migration`s.

## Tables

### accounts — owned money accounts
`id` PK · `name` · `type` (CASH/BANK/BKASH/NAGAD/ROCKET/MFS_OTHER/SAVINGS/WALLET_OTHER)
· `scope` (PERSONAL/BUSINESS) · `note` · `colorHex` · `iconKey` · `sortOrder`
· `archivedAt?`
Balance is derived from `postings`, never stored.

### categories
`id` PK · `name` · `kind` (INCOME/EXPENSE) · `iconKey` · `colorHex` ·
`sortOrder` · `archivedAt?`
Index: `(kind, name)`. Seeded suggestions are ordinary rows — fully editable.

### parties — people (for lending/borrowing)
`id` PK · `name` · `phone?` · `address?` · `note?`

### shops
`id` PK · `name` · `merchantName?` · `phone?` · `address?` · `shopType?` ·
`note?` · `archivedAt?`

### loans — all debt agreements (personal, NGO, bank, lending)
`id` PK · `kind` (PERSONAL_BORROW/PERSONAL_LEND/NGO/BANK) · `partyId?` FK→parties
· `organizationName` · `branch?` · `maskedRef?` · `accountId?` FK→accounts
· `principalPoisha` · `disbursedPoisha` · `totalContractualPoisha?`
· `interestRateBp` · `interestMethod` (NONE/FLAT/REDUCING_AMORTIZED/SIMPLE)
· `frequency` (NONE/WEEKLY/MONTHLY) · `installmentCount` · `startDate`
· `firstDueDate?` · `dueDate?` (single-agreement due date) · `status` (ACTIVE/CLOSED)
· `purpose?` · `note?`

### installments
`id` PK · `loanId` FK→loans (CASCADE) · `seq` · `dueDate` · `amountPoisha` ·
`principalPartPoisha` · `interestPartPoisha` · `feePartPoisha` ·
`paidPrincipalPoisha` · `paidInterestPoisha` · `paidFeePoisha`
Unique `(loanId, seq)`. Status (upcoming/partial/paid/overdue) is derived.

### transactions — one row per financial event
`id` PK · `kind` (OPENING_BALANCE/INCOME/EXPENSE/EXPENSE_REFUND/TRANSFER/
SHOP_PURCHASE_CREDIT/SHOP_PAYMENT/LOAN_RECEIVE/LOAN_REPAYMENT/LEND_OUT/
LEND_RETURN/SAVINGS_DEPOSIT/SAVINGS_WITHDRAWAL/ADJUSTMENT)
· `occurredAt` · `title` · `note?` · `amountPoisha` (gross headline amount) ·
`feePoisha` · `discountPoisha` · `extraChargePoisha` ·
`categoryId?` FK · `accountId?` FK · `counterAccountId?` FK · `partyId?` FK ·
`shopId?` FK · `loanId?` FK · `savingsGoalId?` FK · `receiptNo?` ·
`voidedAt?` · `replacesTransactionId?`
Indexes: `occurredAt`, `kind`, `accountId`, `shopId`, `loanId`, `categoryId`.

### postings — journal lines (the source of truth for all balances)
`id` PK · `transactionId` FK→transactions (CASCADE) · `ledgerType`
(ACCOUNT/INCOME_CATEGORY/EXPENSE_CATEGORY/SHOP_LIABILITY/LOAN_LIABILITY/
RECEIVABLE/EQUITY) · `ledgerId` (referenced row id; 0 for EQUITY) ·
`signedAmountPoisha`
Invariant: per transaction `SUM(signedAmountPoisha) = 0`. Indexes:
`(ledgerType, ledgerId)`, `transactionId`.

### transaction_items — itemized bill lines
`id` PK · `transactionId` FK (CASCADE) · `name` · `quantityMilli` (qty×1000) ·
`unit?` · `unitPricePoisha` · `discountPoisha` · `lineTotalPoisha`

### installment_allocations — which repayment paid what
`id` PK · `transactionId` FK (CASCADE) · `installmentId` FK (CASCADE) ·
`principalPaidPoisha` · `interestPaidPoisha` · `feePaidPoisha`

### savings_goals
`id` PK · `name` · `targetPoisha` · `targetDate?` · `accountId` FK→accounts ·
`productType` (HOME_CASH/BANK/NGO/DPS/FDR/OTHER) · `note?` · `archivedAt?`
Progress = Σ deposits − Σ withdrawals of transactions tagged with this goal
(a memo allocation inside the linked account's real balance — never double
counted in net worth).

### budgets
`id` PK · `categoryId?` FK (null = overall) · `period` (DAILY/WEEKLY/MONTHLY) ·
`amountPoisha` · `anchorDay` (day-of-month for monthly) · `active`

### reminders — user bills/custom reminders
`id` PK · `title` · `amountPoisha?` · `dueDate` · `repeat`
(NONE/WEEKLY/MONTHLY/YEARLY) · `categoryId?` · `note?` · `enabled` ·
`lastNotifiedAt?` · `doneAt?`

### recurring_rules — auto income/expense
`id` PK · `kind` (INCOME/EXPENSE) · `title` · `amountPoisha` · `categoryId` FK ·
`accountId` FK · `frequency` (DAILY/WEEKLY/MONTHLY/YEARLY) · `nextRunDate` ·
`endDate?` · `enabled` · `autoCreate`

### net_worth_items — manual assets/liabilities (land, gold, deposits…)
`id` PK · `name` · `kind` (ASSET/LIABILITY) · `valuePoisha` · `note?`

### attachments — receipt files (copied into app-private storage)
`id` PK · `transactionId` FK (CASCADE) · `fileName` · `mimeType?` · `sizeBytes`

### audit_events
`id` PK · `atEpochMs` · `eventType` · `entityType` · `entityId` · `summary`

## Preferences (DataStore, not Room)
PIN verifier blob (Keystore-encrypted), biometric toggle, auto-lock minutes,
failed-attempt counter & lockout deadline, first-run flag, last backup time,
reminder lead days. See docs/SECURITY.md.
