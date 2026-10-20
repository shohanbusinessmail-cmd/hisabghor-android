package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.PartyEntity
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.LoanKind
import com.shohankhan.hisabghor.core.model.LoanStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PartyRepository(
    private val db: HisabGhorDatabase,
    private val ledger: LedgerRepository
) {

    private val partyDao get() = db.partyDao()

    fun observeAll(includeArchived: Boolean = false): Flow<List<PartyEntity>> =
        partyDao.observeAll(includeArchived)

    /** People with their net receivable/payable positions across all agreements. */
    fun observePartySummaries(): Flow<List<PartySummary>> = combine(
        partyDao.observeAll(false),
        db.loanDao().observePersonal(),
        ledger.balancesByLedger(LedgerType.LOAN_LIABILITY),
        ledger.balancesByLedger(LedgerType.RECEIVABLE)
    ) { parties, loans, liabilities, receivables ->
        parties.map { party ->
            val partyLoans = loans.filter { it.partyId == party.id }
            val receivable = partyLoans
                .filter { it.kind == LoanKind.PERSONAL_LEND }
                .sumOf { receivables[it.id] ?: 0L }
            val payable = partyLoans
                .filter { it.kind == LoanKind.PERSONAL_BORROW }
                .sumOf { -(liabilities[it.id] ?: 0L) }
            PartySummary(
                party = party,
                receivablePoisha = receivable,
                payablePoisha = payable,
                activeAgreements = partyLoans.count { it.status == LoanStatus.ACTIVE }
            )
        }
    }

    fun observeTotalReceivables(): Flow<Long> =
        ledger.balancesByLedger(LedgerType.RECEIVABLE).map { it.values.sum() }

    fun observeById(id: Long): Flow<PartyEntity?> = partyDao.observeById(id)

    suspend fun getById(id: Long): PartyEntity? = partyDao.getById(id)

    suspend fun create(name: String, phone: String?, address: String?, note: String?): Long {
        val now = Dates.nowMs()
        return partyDao.insert(
            PartyEntity(
                name = name.trim(),
                phone = phone?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                createdAt = now, updatedAt = now
            )
        )
    }

    suspend fun update(party: PartyEntity) {
        partyDao.update(party.copy(updatedAt = Dates.nowMs()))
    }

    suspend fun setArchived(party: PartyEntity, archived: Boolean) {
        partyDao.update(
            party.copy(
                archivedAt = if (archived) Dates.nowMs() else null,
                updatedAt = Dates.nowMs()
            )
        )
    }
}
