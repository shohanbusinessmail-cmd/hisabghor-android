package com.shohankhan.hisabghor.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shohankhan.hisabghor.core.model.LedgerType
import com.shohankhan.hisabghor.core.model.TransactionKind

/**
 * One financial event. Money movement itself lives in [PostingEntity] rows;
 * this row carries the human metadata (title, links, timestamps).
 *
 * `amountPoisha` is the gross headline amount whose meaning depends on kind
 * (income amount, expense total, transferred amount, purchase total, total
 * paid in a repayment...). Editing never mutates history in place: the old
 * row is voided ([voidedAt]) and a corrected row points back via
 * [replacesTransactionId].
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(AccountEntity::class, ["id"], ["counterAccountId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(PartyEntity::class, ["id"], ["partyId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(ShopEntity::class, ["id"], ["shopId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(LoanEntity::class, ["id"], ["loanId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(SavingsGoalEntity::class, ["id"], ["savingsGoalId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [
        Index("occurredAt"), Index("kind"), Index("accountId"), Index("counterAccountId"),
        Index("categoryId"), Index("shopId"), Index("loanId"), Index("partyId"), Index("savingsGoalId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: TransactionKind,
    val occurredAt: Long,
    val title: String,
    val note: String? = null,
    val amountPoisha: Long,
    val feePoisha: Long = 0,
    val discountPoisha: Long = 0,
    val extraChargePoisha: Long = 0,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val counterAccountId: Long? = null,
    val partyId: Long? = null,
    val shopId: Long? = null,
    val loanId: Long? = null,
    val savingsGoalId: Long? = null,
    val receiptNo: String? = null,
    val voidedAt: Long? = null,
    val replacesTransactionId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * A journal line. All balances in the app are `SUM`s over this table
 * (joined to non-voided transactions). `ledgerId` is polymorphic
 * (account/category/shop/loan id, or 0 for EQUITY) so it intentionally has
 * no SQL foreign key; referential safety is guaranteed because ledgers are
 * archived rather than hard-deleted once used.
 */
@Entity(
    tableName = "postings",
    foreignKeys = [
        ForeignKey(TransactionEntity::class, ["id"], ["transactionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("transactionId"),
        Index(value = ["ledgerType", "ledgerId"])
    ]
)
data class PostingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val ledgerType: LedgerType,
    val ledgerId: Long,
    val signedAmountPoisha: Long
)

/**
 * Itemized bill line. Quantity is stored in thousandths (`quantityMilli`) so
 * weights like ১.৫ কেজি are exact; line total =
 * round(quantityMilli × unitPricePoisha ÷ 1000) − discountPoisha.
 */
@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(TransactionEntity::class, ["id"], ["transactionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("transactionId")]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val name: String,
    val quantityMilli: Long,
    val unit: String? = null,
    val unitPricePoisha: Long,
    val discountPoisha: Long = 0,
    val lineTotalPoisha: Long
)

/** Receipt file copied into app-private storage (`files/receipts/<fileName>`). */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(TransactionEntity::class, ["id"], ["transactionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("transactionId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val fileName: String,
    val mimeType: String? = null,
    val sizeBytes: Long = 0,
    val createdAt: Long
)
