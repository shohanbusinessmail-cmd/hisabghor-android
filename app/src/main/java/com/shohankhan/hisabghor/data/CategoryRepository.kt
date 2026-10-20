package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.CategoryEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.model.CategoryKind
import kotlinx.coroutines.flow.Flow

/**
 * Categories are fully user-managed. Safety rule: a category referenced by any
 * live posting can only be archived — hard delete is refused so historical
 * reports can never be corrupted.
 */
class CategoryRepository(private val db: HisabGhorDatabase) {

    private val categoryDao get() = db.categoryDao()

    fun observeByKind(kind: CategoryKind, includeArchived: Boolean = false): Flow<List<CategoryEntity>> =
        categoryDao.observeByKind(kind, includeArchived)

    fun observeAll(includeArchived: Boolean = false): Flow<List<CategoryEntity>> =
        categoryDao.observeAll(includeArchived)

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun create(name: String, kind: CategoryKind, iconKey: String, colorHex: Long): Long {
        val now = Dates.nowMs()
        return categoryDao.insert(
            CategoryEntity(
                name = name.trim(), kind = kind, iconKey = iconKey, colorHex = colorHex,
                createdAt = now, updatedAt = now
            )
        )
    }

    suspend fun update(category: CategoryEntity) {
        categoryDao.update(category.copy(name = category.name.trim(), updatedAt = Dates.nowMs()))
    }

    suspend fun setArchived(category: CategoryEntity, archived: Boolean) {
        categoryDao.update(
            category.copy(
                archivedAt = if (archived) Dates.nowMs() else null,
                updatedAt = Dates.nowMs()
            )
        )
    }

    /** Returns true if deleted; false when live postings still reference it. */
    suspend fun deleteIfUnused(category: CategoryEntity): Boolean {
        val usage = db.postingDao().countLiveCategoryPostings(category.id)
        if (usage > 0) return false
        categoryDao.delete(category)
        return true
    }

    /** Live posting count — shown to the user before archive/delete decisions. */
    suspend fun usageCount(categoryId: Long): Int =
        db.postingDao().countLiveCategoryPostings(categoryId)

    /**
     * Finds an active category by exact name or creates it. Used for system
     * buckets like loan interest / transfer fees when the user hasn't picked one.
     */
    suspend fun findOrCreate(name: String, kind: CategoryKind, iconKey: String): Long {
        val existing = categoryDao.search(name).firstOrNull { it.name == name && it.kind == kind }
        if (existing != null) return existing.id
        return create(name, kind, iconKey, 0xFF0B7A4B)
    }

    companion object {
        const val NAME_LOAN_INTEREST = "ঋণের সুদ ও চার্জ"
        const val NAME_TRANSFER_FEE = "ব্যাংক/ওয়ালেট চার্জ"
        const val NAME_SAVINGS_PROFIT = "সঞ্চয়ের লাভ/সুদ"
    }
}
