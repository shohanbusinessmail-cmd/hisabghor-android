package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.shohankhan.hisabghor.core.model.AccountScope
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.BudgetPeriod
import com.shohankhan.hisabghor.core.model.CategoryKind
import com.shohankhan.hisabghor.core.model.InstallmentFrequency
import com.shohankhan.hisabghor.core.model.InterestMethod
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.NetWorthItemKind
import com.shohankhan.hisabghor.core.model.RepeatRule
import com.shohankhan.hisabghor.core.model.SavingsProductType
import com.shohankhan.hisabghor.core.model.TransactionKind

/** Bengali display labels for every domain enum (UI language is Bengali-only). */

fun AccountType.bnLabel(): String = when (this) {
    AccountType.CASH -> "ক্যাশ (হাতে নগদ)"
    AccountType.BANK -> "ব্যাংক অ্যাকাউন্ট"
    AccountType.BKASH -> "বিকাশ"
    AccountType.NAGAD -> "নগদ (Nagad)"
    AccountType.ROCKET -> "রকেট"
    AccountType.MFS_OTHER -> "অন্যান্য মোবাইল ব্যাংকিং"
    AccountType.SAVINGS -> "সঞ্চয় অ্যাকাউন্ট"
    AccountType.WALLET_OTHER -> "অন্যান্য ওয়ালেট"
}

fun AccountScope.bnLabel(): String = when (this) {
    AccountScope.PERSONAL -> "ব্যক্তিগত"
    AccountScope.BUSINESS -> "ব্যবসায়িক"
}

fun CategoryKind.bnLabel(): String = when (this) {
    CategoryKind.INCOME -> "আয়"
    CategoryKind.EXPENSE -> "খরচ"
}

fun TransactionKind.bnLabel(): String = when (this) {
    TransactionKind.OPENING_BALANCE -> "শুরুর ব্যালান্স"
    TransactionKind.INCOME -> "আয়"
    TransactionKind.EXPENSE -> "খরচ"
    TransactionKind.EXPENSE_REFUND -> "রিফান্ড (খরচ ফেরত)"
    TransactionKind.TRANSFER -> "ট্রান্সফার"
    TransactionKind.SHOP_PURCHASE_CREDIT -> "বাকি ক্রয়"
    TransactionKind.SHOP_PAYMENT -> "দোকান পরিশোধ"
    TransactionKind.LOAN_RECEIVE -> "ঋণ/ধার গ্রহণ"
    TransactionKind.LOAN_REPAYMENT -> "ঋণ পরিশোধ"
    TransactionKind.LEND_OUT -> "ধার দেওয়া"
    TransactionKind.LEND_RETURN -> "পাওনা ফেরত"
    TransactionKind.SAVINGS_DEPOSIT -> "সঞ্চয়ে জমা"
    TransactionKind.SAVINGS_WITHDRAWAL -> "সঞ্চয় উত্তোলন"
    TransactionKind.ADJUSTMENT -> "ব্যালান্স সমন্বয়"
}

/** +1 money-in feel, -1 money-out feel, 0 neutral — for list amount coloring only. */
fun TransactionKind.displaySign(): Int = when (this) {
    TransactionKind.INCOME, TransactionKind.EXPENSE_REFUND, TransactionKind.LEND_RETURN -> 1
    TransactionKind.EXPENSE, TransactionKind.SHOP_PURCHASE_CREDIT, TransactionKind.SHOP_PAYMENT,
    TransactionKind.LOAN_REPAYMENT, TransactionKind.LEND_OUT -> -1
    else -> 0
}

fun LoanKind.bnLabel(): String = when (this) {
    LoanKind.PERSONAL_BORROW -> "ধার নেওয়া"
    LoanKind.PERSONAL_LEND -> "ধার দেওয়া"
    LoanKind.NGO -> "এনজিও ঋণ"
    LoanKind.BANK -> "ব্যাংক ঋণ"
}

fun InterestMethod.bnLabel(): String = when (this) {
    InterestMethod.NONE -> "সুদ নেই"
    InterestMethod.FLAT -> "ফ্ল্যাট (সরল হারে মোট নির্ধারিত)"
    InterestMethod.REDUCING_AMORTIZED -> "হ্রাসমান ব্যালান্স (EMI)"
    InterestMethod.SIMPLE -> "সরল সুদ"
}

fun InstallmentFrequency.bnLabel(): String = when (this) {
    InstallmentFrequency.NONE -> "কিস্তি নেই"
    InstallmentFrequency.WEEKLY -> "সাপ্তাহিক"
    InstallmentFrequency.MONTHLY -> "মাসিক"
}

fun BudgetPeriod.bnLabel(): String = when (this) {
    BudgetPeriod.DAILY -> "দৈনিক"
    BudgetPeriod.WEEKLY -> "সাপ্তাহিক"
    BudgetPeriod.MONTHLY -> "মাসিক"
}

fun RepeatRule.bnLabel(): String = when (this) {
    RepeatRule.NONE -> "একবারই"
    RepeatRule.DAILY -> "প্রতিদিন"
    RepeatRule.WEEKLY -> "সাপ্তাহিক"
    RepeatRule.MONTHLY -> "মাসিক"
    RepeatRule.YEARLY -> "বাৎসরিক"
}

fun SavingsProductType.bnLabel(): String = when (this) {
    SavingsProductType.HOME_CASH -> "বাড়িতে নগদ সঞ্চয়"
    SavingsProductType.BANK -> "ব্যাংক সঞ্চয়"
    SavingsProductType.NGO -> "এনজিও সঞ্চয়"
    SavingsProductType.DPS -> "ডিপিএস"
    SavingsProductType.FDR -> "এফডিআর"
    SavingsProductType.OTHER -> "অন্যান্য"
}

fun NetWorthItemKind.bnLabel(): String = when (this) {
    NetWorthItemKind.ASSET -> "সম্পদ"
    NetWorthItemKind.LIABILITY -> "দায়"
}

/** Icon choices offered in category/account editors (key → icon). */
val iconOptions: List<Pair<String, ImageVector>> = listOf(
    "salary" to Icons.Filled.Payments,
    "business" to Icons.Filled.BusinessCenter,
    "agriculture" to Icons.Filled.Agriculture,
    "sales" to Icons.Filled.PriceCheck,
    "rent" to Icons.Filled.Home,
    "commission" to Icons.Filled.Percent,
    "remittance" to Icons.Filled.FlightTakeoff,
    "gift" to Icons.Filled.CardGiftcard,
    "savings_profit" to Icons.Filled.TrendingUp,
    "other_income" to Icons.Filled.AttachMoney,
    "food" to Icons.Filled.Fastfood,
    "home_rent" to Icons.Filled.Home,
    "transport" to Icons.Filled.DirectionsBus,
    "bills" to Icons.Filled.ElectricBolt,
    "mobile" to Icons.Filled.PhoneAndroid,
    "health" to Icons.Filled.LocalHospital,
    "education" to Icons.Filled.School,
    "clothing" to Icons.Filled.Checkroom,
    "family" to Icons.Filled.People,
    "loan_interest" to Icons.Filled.Percent,
    "bank_charge" to Icons.Filled.AccountBalance,
    "other_expense" to Icons.Filled.ReceiptLong,
    "shopping" to Icons.Filled.ShoppingCart,
    "store" to Icons.Filled.Storefront,
    "work" to Icons.Filled.Work,
    "handshake" to Icons.Filled.Handshake,
    "wallet" to Icons.Filled.Wallet,
    "cash" to Icons.Filled.Money,
    "bank" to Icons.Filled.AccountBalance,
    "savings" to Icons.Filled.Savings,
    "exchange" to Icons.Filled.CurrencyExchange,
    "label" to Icons.Filled.Label,
    "category" to Icons.Filled.Category
)

fun iconForKey(iconKey: String): ImageVector =
    iconOptions.firstOrNull { it.first == iconKey }?.second ?: Icons.Filled.Label

fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH -> Icons.Filled.Money
    AccountType.BANK -> Icons.Filled.AccountBalance
    AccountType.BKASH, AccountType.NAGAD, AccountType.ROCKET, AccountType.MFS_OTHER ->
        Icons.Filled.PhoneAndroid
    AccountType.SAVINGS -> Icons.Filled.Savings
    AccountType.WALLET_OTHER -> Icons.Filled.AccountBalanceWallet
}

/** Color choices for categories (stored as ARGB Long). */
val categoryColorOptions: List<Color> = listOf(
    Color(0xFF0B7A4B), Color(0xFF1565C0), Color(0xFFC62828), Color(0xFFEF6C00),
    Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFAD1457), Color(0xFF5D4037),
    Color(0xFF283593), Color(0xFF2E7D32), Color(0xFFB26A00), Color(0xFF455A64)
)

fun colorFromHex(hex: Long): Color = Color(hex)
