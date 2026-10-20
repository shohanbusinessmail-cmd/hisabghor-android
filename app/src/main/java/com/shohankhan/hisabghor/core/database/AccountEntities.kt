package com.shohankhan.hisabghor.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shohankhan.hisabghor.core.model.AccountScope
import com.shohankhan.hisabghor.core.model.AccountType
import com.shohankhan.hisabghor.core.model.CategoryKind

/**
 * An owned money account (cash, bank, bKash...). Its balance is never stored
 * here — it is always derived from journal postings (see PostingDao).
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val scope: AccountScope = AccountScope.PERSONAL,
    val note: String? = null,
    val colorHex: Long = 0xFF0B7A4B,
    val iconKey: String = "wallet",
    val sortOrder: Int = 0,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** User-managed income/expense category. Historical rows are archived, not deleted. */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["kind", "name"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val iconKey: String = "label",
    val colorHex: Long = 0xFF0B7A4B,
    val sortOrder: Int = 0,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
