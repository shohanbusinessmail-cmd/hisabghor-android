package com.shohankhan.hisabghor.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisabghor.core.model.LoanKind
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE (:includeArchived OR archivedAt IS NULL) ORDER BY name")
    fun observeAll(includeArchived: Boolean = false): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id")
    fun observeById(id: Long): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getById(id: Long): PartyEntity?

    @Query("SELECT * FROM parties WHERE name LIKE '%' || :q || '%' AND archivedAt IS NULL ORDER BY name LIMIT 20")
    suspend fun search(q: String): List<PartyEntity>

    @Query("SELECT * FROM parties ORDER BY id")
    suspend fun getAllForBackup(): List<PartyEntity>

    @Insert
    suspend fun insert(party: PartyEntity): Long

    @Update
    suspend fun update(party: PartyEntity)

    @Delete
    suspend fun delete(party: PartyEntity)
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops WHERE (:includeArchived OR archivedAt IS NULL) ORDER BY name")
    fun observeAll(includeArchived: Boolean = false): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE id = :id")
    fun observeById(id: Long): Flow<ShopEntity?>

    @Query("SELECT * FROM shops WHERE id = :id")
    suspend fun getById(id: Long): ShopEntity?

    @Query("SELECT * FROM shops WHERE (name LIKE '%' || :q || '%' OR merchantName LIKE '%' || :q || '%') AND archivedAt IS NULL ORDER BY name LIMIT 20")
    suspend fun search(q: String): List<ShopEntity>

    @Query("SELECT * FROM shops ORDER BY id")
    suspend fun getAllForBackup(): List<ShopEntity>

    @Insert
    suspend fun insert(shop: ShopEntity): Long

    @Update
    suspend fun update(shop: ShopEntity)

    @Delete
    suspend fun delete(shop: ShopEntity)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY status, createdAt DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE kind = :kind ORDER BY status, createdAt DESC")
    fun observeByKind(kind: LoanKind): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE kind IN ('PERSONAL_BORROW', 'PERSONAL_LEND') ORDER BY status, createdAt DESC")
    fun observePersonal(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE partyId = :partyId ORDER BY status, createdAt DESC")
    fun observeForParty(partyId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id")
    fun observeById(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE organizationName LIKE '%' || :q || '%' ORDER BY createdAt DESC LIMIT 20")
    suspend fun search(q: String): List<LoanEntity>

    @Query("SELECT * FROM loans ORDER BY id")
    suspend fun getAllForBackup(): List<LoanEntity>

    @Insert
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Delete
    suspend fun delete(loan: LoanEntity)
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY seq")
    fun observeForLoan(loanId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY seq")
    suspend fun getForLoan(loanId: Long): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getById(id: Long): InstallmentEntity?

    /** Unpaid installments due on/before [toMs] (includes overdue), soonest first. */
    @Query(
        """
        SELECT i.id AS installmentId, i.loanId AS loanId, l.organizationName AS organizationName,
               l.kind AS loanKind, i.seq AS seq, i.dueDate AS dueDate, i.amountPoisha AS amountPoisha,
               (i.paidPrincipalPoisha + i.paidInterestPoisha + i.paidFeePoisha) AS paidTotalPoisha
        FROM installments i
        INNER JOIN loans l ON l.id = i.loanId AND l.status = 'ACTIVE'
        WHERE i.dueDate <= :toMs
          AND (i.paidPrincipalPoisha + i.paidInterestPoisha + i.paidFeePoisha) < i.amountPoisha
        ORDER BY i.dueDate ASC
        LIMIT :limit
        """
    )
    fun observeUpcoming(toMs: Long, limit: Int = 20): Flow<List<UpcomingInstallmentRow>>

    @Query(
        """
        SELECT i.id AS installmentId, i.loanId AS loanId, l.organizationName AS organizationName,
               l.kind AS loanKind, i.seq AS seq, i.dueDate AS dueDate, i.amountPoisha AS amountPoisha,
               (i.paidPrincipalPoisha + i.paidInterestPoisha + i.paidFeePoisha) AS paidTotalPoisha
        FROM installments i
        INNER JOIN loans l ON l.id = i.loanId AND l.status = 'ACTIVE'
        WHERE i.dueDate <= :toMs
          AND (i.paidPrincipalPoisha + i.paidInterestPoisha + i.paidFeePoisha) < i.amountPoisha
        ORDER BY i.dueDate ASC
        """
    )
    suspend fun getDueUpTo(toMs: Long): List<UpcomingInstallmentRow>

    /** Personal agreements with a plain due date coming up (worker reminders). */
    @Query("SELECT * FROM loans WHERE status = 'ACTIVE' AND dueDate IS NOT NULL AND dueDate <= :toMs ORDER BY dueDate")
    suspend fun getLoansWithDueDateUpTo(toMs: Long): List<LoanEntity>

    @Query("SELECT * FROM installments ORDER BY id")
    suspend fun getAllForBackup(): List<InstallmentEntity>

    @Insert
    suspend fun insertAll(installments: List<InstallmentEntity>): List<Long>

    @Insert
    suspend fun insert(installment: InstallmentEntity): Long

    @Update
    suspend fun update(installment: InstallmentEntity)

    @Query("DELETE FROM installments WHERE loanId = :loanId AND paidPrincipalPoisha = 0 AND paidInterestPoisha = 0 AND paidFeePoisha = 0")
    suspend fun deleteUnpaidForLoan(loanId: Long)
}
