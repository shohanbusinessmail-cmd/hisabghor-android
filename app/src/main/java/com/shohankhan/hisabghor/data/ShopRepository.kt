package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.accounting.PostingFactory
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.database.ShopEntity
import com.shohankhan.hisabghor.core.database.TransactionEntity
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.TransactionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Shop credit ledgers. Expense is recognised once at purchase time; paying the
 * shop later reduces cash and the liability together (never a second expense).
 */
class ShopRepository(
    private val db: HisabGhorDatabase,
    private val ledger: LedgerRepository
) {

    private val shopDao get() = db.shopDao()

    fun observeShopsWithDue(includeArchived: Boolean = false): Flow<List<ShopWithDue>> =
        combine(
            shopDao.observeAll(includeArchived),
            ledger.balancesByLedger(LedgerType.SHOP_LIABILITY)
        ) { shops, balances ->
            shops.map { ShopWithDue(it, -(balances[it.id] ?: 0L)) }
        }

    fun observeTotalShopDue(): Flow<Long> =
        ledger.balancesByLedger(LedgerType.SHOP_LIABILITY).map { balances ->
            -balances.values.sum()
        }

    fun observeShop(id: Long): Flow<ShopEntity?> = shopDao.observeById(id)

    suspend fun getShop(id: Long): ShopEntity? = shopDao.getById(id)

    suspend fun createShop(
        name: String,
        merchantName: String?,
        phone: String?,
        address: String?,
        shopType: String?,
        note: String?,
        openingDuePoisha: Long
    ): Long {
        val now = Dates.nowMs()
        val id = shopDao.insert(
            ShopEntity(
                name = name.trim(),
                merchantName = merchantName?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null },
                shopType = shopType?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                createdAt = now, updatedAt = now
            )
        )
        if (openingDuePoisha > 0) {
            ledger.record(
                transaction = TransactionEntity(
                    kind = TransactionKind.OPENING_BALANCE,
                    occurredAt = now,
                    title = "আগের বকেয়া — ${name.trim()}",
                    amountPoisha = openingDuePoisha,
                    shopId = id,
                    createdAt = now, updatedAt = now
                ),
                postings = PostingFactory.openingShopDue(id, openingDuePoisha),
                auditSummary = "দোকান তৈরি (আগের বকেয়াসহ): ${name.trim()}"
            )
        }
        return id
    }

    suspend fun updateShop(shop: ShopEntity) {
        shopDao.update(shop.copy(updatedAt = Dates.nowMs()))
    }

    suspend fun setArchived(shop: ShopEntity, archived: Boolean) {
        shopDao.update(
            shop.copy(
                archivedAt = if (archived) Dates.nowMs() else null,
                updatedAt = Dates.nowMs()
            )
        )
    }

    /**
     * Credit (বাকি) purchase, optionally itemized. [paidNowPoisha] may be 0
     * (fully on credit) or up to the total. Expense counted once, now.
     */
    suspend fun recordCreditPurchase(
        shopId: Long,
        categoryId: Long,
        amountPoisha: Long,
        items: List<ItemDraft>,
        extraChargePoisha: Long,
        discountPoisha: Long,
        paidNowPoisha: Long,
        accountId: Long?,
        occurredAt: Long,
        title: String,
        note: String?,
        replacesTransactionId: Long? = null
    ): Long {
        val total = if (items.isNotEmpty()) {
            (items.sumOf { it.lineTotalPoisha } + extraChargePoisha - discountPoisha).coerceAtLeast(0)
        } else amountPoisha
        require(total > 0) { "purchase total must be positive" }
        val shop = shopDao.getById(shopId)
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.SHOP_PURCHASE_CREDIT,
                occurredAt = occurredAt,
                title = title.ifBlank { "বাকি ক্রয় — ${shop?.name ?: ""}" },
                note = note?.trim()?.ifBlank { null },
                amountPoisha = total,
                discountPoisha = discountPoisha,
                extraChargePoisha = extraChargePoisha,
                categoryId = categoryId,
                accountId = accountId,
                shopId = shopId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.shopCreditPurchase(
                shopId, categoryId, total, paidNowPoisha, accountId
            ),
            items = items.map { it.toEntity() },
            replacesTransactionId = replacesTransactionId,
            auditSummary = "বাকি ক্রয়: ${shop?.name}"
        )
    }

    /** Partial or full payment of a shop due. Does not create a new expense. */
    suspend fun recordPayment(
        shopId: Long,
        accountId: Long,
        amountPoisha: Long,
        occurredAt: Long,
        note: String?,
        replacesTransactionId: Long? = null
    ): Long {
        val shop = shopDao.getById(shopId)
        val now = Dates.nowMs()
        return ledger.record(
            transaction = TransactionEntity(
                kind = TransactionKind.SHOP_PAYMENT,
                occurredAt = occurredAt,
                title = "দোকানের বকেয়া পরিশোধ — ${shop?.name ?: ""}",
                note = note?.trim()?.ifBlank { null },
                amountPoisha = amountPoisha,
                accountId = accountId,
                shopId = shopId,
                createdAt = now, updatedAt = now
            ),
            postings = PostingFactory.shopPayment(shopId, accountId, amountPoisha),
            replacesTransactionId = replacesTransactionId,
            auditSummary = "দোকান পরিশোধ: ${shop?.name}"
        )
    }
}
