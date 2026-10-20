package com.shohankhan.hisabghor.core.model

/** Type of an owned money account. */
enum class AccountType { CASH, BANK, BKASH, NAGAD, ROCKET, MFS_OTHER, SAVINGS, WALLET_OTHER }

/** Personal vs business separation. */
enum class AccountScope { PERSONAL, BUSINESS }

enum class CategoryKind { INCOME, EXPENSE }

/** Ledger (journal) bucket a posting belongs to. See docs/ACCOUNTING_RULES.md. */
enum class LedgerType {
    ACCOUNT, INCOME_CATEGORY, EXPENSE_CATEGORY,
    SHOP_LIABILITY, LOAN_LIABILITY, RECEIVABLE, EQUITY
}

/** Financial event kinds. Every kind has a fixed posting shape (PostingFactory). */
enum class TransactionKind {
    OPENING_BALANCE,
    INCOME,
    EXPENSE,
    EXPENSE_REFUND,
    TRANSFER,
    SHOP_PURCHASE_CREDIT,
    SHOP_PAYMENT,
    LOAN_RECEIVE,
    LOAN_REPAYMENT,
    LEND_OUT,
    LEND_RETURN,
    SAVINGS_DEPOSIT,
    SAVINGS_WITHDRAWAL,
    ADJUSTMENT
}

enum class LoanKind { PERSONAL_BORROW, PERSONAL_LEND, NGO, BANK }

enum class InterestMethod { NONE, FLAT, REDUCING_AMORTIZED, SIMPLE }

enum class InstallmentFrequency { NONE, WEEKLY, MONTHLY }

enum class LoanStatus { ACTIVE, CLOSED }

enum class SavingsProductType { HOME_CASH, BANK, NGO, DPS, FDR, OTHER }

enum class BudgetPeriod { DAILY, WEEKLY, MONTHLY }

enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

enum class NetWorthItemKind { ASSET, LIABILITY }

/** Derived installment status (never stored). */
enum class InstallmentStatus { UPCOMING, PARTIAL, PAID, OVERDUE }
