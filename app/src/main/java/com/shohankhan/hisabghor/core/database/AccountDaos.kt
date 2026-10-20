package com.shohankhan.hisabghor.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisabghor.core.model.CategoryKind
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE (:includeArchived OR archivedAt IS NULL) ORDER BY sortOrder, createdAt")
    fun observeAll(includeArchived: Boolean = false): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun countAll(): Int

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)
}

@Dao
interface CategoryDao {

    @Query(
        "SELECT * FROM categories WHERE kind = :kind AND (:includeArchived OR archivedAt IS NULL) " +
            "ORDER BY sortOrder, createdAt"
    )
    fun observeByKind(kind: CategoryKind, includeArchived: Boolean = false): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE (:includeArchived OR archivedAt IS NULL) ORDER BY kind, sortOrder, createdAt")
    fun observeAll(includeArchived: Boolean = false): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countAll(): Int

    @Query(
        "SELECT * FROM categories WHERE name LIKE '%' || :q || '%' AND archivedAt IS NULL ORDER BY name LIMIT 20"
    )
    suspend fun search(q: String): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    /** Hard delete — repository enforces "no postings reference this category" first. */
    @Delete
    suspend fun delete(category: CategoryEntity)
}
