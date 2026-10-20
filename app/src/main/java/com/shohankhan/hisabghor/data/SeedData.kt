package com.shohankhan.hisabghor.data

import androidx.room.withTransaction
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AccountEntity
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.CategoryKind

/**
 * First-run seed data: suggestion categories and one cash account.
 * Everything is ordinary user data afterwards — fully editable/archivable.
 * Seed data is clearly structure only; it never pretends to be user finances
 * (no fake transactions or balances are created).
 */
object SeedData {

    private val incomeDefaults = listOf(
        Triple("বেতন", "salary", 0xFF0B7A4B),
        Triple("ব্যবসা আয়", "business", 0xFF1565C0),
        Triple("কৃষি আয়", "agriculture", 0xFF6A9A2A),
        Triple("পণ্য বিক্রয়", "sales", 0xFF00838F),
        Triple("বাড়ি ভাড়া পাওয়া", "rent", 0xFF6A1B9A),
        Triple("কমিশন", "commission", 0xFFAD1457),
        Triple("রেমিট্যান্স", "remittance", 0xFF283593),
        Triple("উপহার", "gift", 0xFFEF6C00),
        Triple(CategoryRepository.NAME_SAVINGS_PROFIT, "savings_profit", 0xFF2E7D32),
        Triple("অন্যান্য আয়", "other_income", 0xFF546E7A)
    )

    private val expenseDefaults = listOf(
        Triple("খাবার ও বাজার", "food", 0xFFC62828),
        Triple("বাড়ি ভাড়া", "home_rent", 0xFF6A1B9A),
        Triple("যাতায়াত", "transport", 0xFF1565C0),
        Triple("বিদ্যুৎ ও বিল", "bills", 0xFFEF6C00),
        Triple("মোবাইল ও ইন্টারনেট", "mobile", 0xFF00838F),
        Triple("স্বাস্থ্য ও ওষুধ", "health", 0xFFAD1457),
        Triple("শিক্ষা", "education", 0xFF283593),
        Triple("পোশাক", "clothing", 0xFF5D4037),
        Triple("কৃষি খরচ", "agriculture", 0xFF6A9A2A),
        Triple("পরিবার ও সামাজিক", "family", 0xFF8E24AA),
        Triple(CategoryRepository.NAME_LOAN_INTEREST, "loan_interest", 0xFFB26A00),
        Triple(CategoryRepository.NAME_TRANSFER_FEE, "bank_charge", 0xFF455A64),
        Triple("অন্যান্য খরচ", "other_expense", 0xFF546E7A)
    )

    suspend fun seedIfEmpty(db: HisabGhorDatabase) {
        db.withTransaction {
            val now = Dates.nowMs()
            if (db.categoryDao().countAll() == 0) {
                incomeDefaults.forEachIndexed { i, (name, icon, color) ->
                    db.categoryDao().insert(
                        CategoryEntity(
                            name = name, kind = CategoryKind.INCOME, iconKey = icon,
                            colorHex = color, sortOrder = i, createdAt = now, updatedAt = now
                        )
                    )
                }
                expenseDefaults.forEachIndexed { i, (name, icon, color) ->
                    db.categoryDao().insert(
                        CategoryEntity(
                            name = name, kind = CategoryKind.EXPENSE, iconKey = icon,
                            colorHex = color, sortOrder = i, createdAt = now, updatedAt = now
                        )
                    )
                }
            }
            if (db.accountDao().countAll() == 0) {
                db.accountDao().insert(
                    AccountEntity(
                        name = "নগদ টাকা (ক্যাশ)",
                        type = AccountType.CASH,
                        iconKey = "cash",
                        createdAt = now, updatedAt = now
                    )
                )
            }
        }
    }
}
