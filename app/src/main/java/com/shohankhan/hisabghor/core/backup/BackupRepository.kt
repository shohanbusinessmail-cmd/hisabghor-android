package com.shohankhan.hisabghor.core.backup

import androidx.room.withTransaction
import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AttachmentEntity
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.security.BackupCrypto
import com.shohankhan.hisabghor.core.security.EncryptedBlob
import com.shohankhan.hisabghor.data.AuditLogger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Base64
import javax.crypto.AEADBadTagException

/** Backup problems shown to the user in Bengali. */
class BackupException(message: String) : Exception(message)

data class BackupPreview(
    val createdAtEpochMs: Long,
    val appVersion: String,
    val schemaVersion: Int,
    val accountCount: Int,
    val transactionCount: Int,
    val shopCount: Int,
    val loanCount: Int,
    val attachmentFilesIncluded: Int,
    val attachmentFilesMissing: Int
)

/**
 * Encrypted full backup & restore, plain JSON export, CSV export.
 *
 * Restore safety model (docs/BACKUP_FORMAT.md):
 *  1. Decrypt (GCM authentication catches wrong password / tampering).
 *  2. Parse + validate (balanced postings, referential integrity).
 *  3. Replace ALL data inside ONE database transaction — any failure rolls
 *     back and the previous database stays byte-for-byte intact.
 *  4. Receipt files are written only after the database commit succeeds.
 * Restore is always a full replace (never a silent merge); the UI states this
 * before the user confirms.
 */
class BackupRepository(
    private val db: HisabGhorDatabase,
    private val receiptsDir: File,
    private val audit: AuditLogger,
    private val appVersion: String
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---------------- export ----------------

    suspend fun buildPayload(): BackupPayload {
        val attachments = db.transactionDao().getAllAttachmentsForBackup().map { entity ->
            entity.toDto(readReceiptBase64(entity))
        }
        return BackupPayload(
            schemaVersion = 1,
            appVersion = appVersion,
            createdAtEpochMs = Dates.nowMs(),
            accounts = db.accountDao().observeAll(true).first().map { it.toDto() },
            categories = db.categoryDao().observeAll(true).first().map { it.toDto() },
            parties = db.partyDao().getAllForBackup().map { it.toDto() },
            shops = db.shopDao().getAllForBackup().map { it.toDto() },
            loans = db.loanDao().getAllForBackup().map { it.toDto() },
            installments = db.installmentDao().getAllForBackup().map { it.toDto() },
            transactions = db.transactionDao().getAllForBackup().map { it.toDto() },
            postings = db.transactionDao().getAllPostingsForBackup().map { it.toDto() },
            transactionItems = db.transactionDao().getAllItemsForBackup().map { it.toDto() },
            installmentAllocations = db.transactionDao().getAllAllocationsForBackup().map { it.toDto() },
            savingsGoals = db.savingsGoalDao().getAllForBackup().map { it.toDto() },
            budgets = db.budgetDao().getAllForBackup().map { it.toDto() },
            reminders = db.reminderDao().getAllForBackup().map { it.toDto() },
            recurringRules = db.recurringRuleDao().getAllForBackup().map { it.toDto() },
            netWorthItems = db.netWorthItemDao().getAllForBackup().map { it.toDto() },
            attachments = attachments,
            auditEvents = db.auditDao().getAllForBackup().map { it.toDto() }
        )
    }

    /** Encrypted backup file bytes (JSON envelope, UTF-8). */
    suspend fun createEncryptedBackup(password: CharArray): ByteArray {
        require(password.size >= 6) { "password too short" }
        val payloadJson = json.encodeToString(BackupPayload.serializer(), buildPayload())
        val blob = BackupCrypto.encrypt(payloadJson.toByteArray(Charsets.UTF_8), password)
        val envelope = BackupEnvelope(
            kdfIterations = blob.iterations,
            saltB64 = Base64.getEncoder().encodeToString(blob.salt),
            ivB64 = Base64.getEncoder().encodeToString(blob.iv),
            dataB64 = Base64.getEncoder().encodeToString(blob.cipherText)
        )
        audit.log(AuditLogger.BACKUP, "backup", 0, "এনক্রিপ্ট করা ব্যাকআপ তৈরি")
        return json.encodeToString(BackupEnvelope.serializer(), envelope).toByteArray(Charsets.UTF_8)
    }

    /** Plain (unencrypted) JSON export of the same documented schema. */
    suspend fun createPlainJsonExport(): ByteArray {
        val pretty = Json { prettyPrint = true; encodeDefaults = true }
        return pretty.encodeToString(BackupPayload.serializer(), buildPayload())
            .toByteArray(Charsets.UTF_8)
    }

    /** Human-readable transactions CSV for a period (live transactions only). */
    suspend fun createTransactionsCsv(fromMs: Long, toMs: Long): ByteArray {
        val transactions = db.transactionDao().observeFiltered(
            kind = null, categoryId = null, accountId = null, shopId = null,
            loanId = null, partyId = null, fromMs = fromMs, toMs = toMs,
            minAmount = null, maxAmount = null, q = "", limit = 100_000
        ).first().sortedBy { it.occurredAt }
        val categories = db.categoryDao().observeAll(true).first().associateBy { it.id }
        val accounts = db.accountDao().observeAll(true).first().associateBy { it.id }
        val rows = transactions.map { t ->
            listOf(
                Dates.formatDateTime(t.occurredAt),
                t.title,
                t.kind.name,
                categories[t.categoryId]?.name ?: "",
                accounts[t.accountId]?.name ?: "",
                accounts[t.counterAccountId]?.name ?: "",
                CsvExporter.takaPlain(t.amountPoisha),
                t.note ?: ""
            )
        }
        val csv = CsvExporter.build(
            headers = listOf("তারিখ", "শিরোনাম", "ধরন", "ক্যাটাগরি", "অ্যাকাউন্ট", "অপর অ্যাকাউন্ট", "পরিমাণ (টাকা)", "মন্তব্য"),
            rows = rows
        )
        return csv.toByteArray(Charsets.UTF_8)
    }

    // ---------------- inspect & restore ----------------

    fun inspectEncrypted(bytes: ByteArray, password: CharArray): BackupPreview {
        val payload = decryptPayload(bytes, password)
        validate(payload)
        return payload.toPreview()
    }

    /**
     * Full-replace restore. Throws [BackupException] with a Bengali message on
     * any problem; the existing database is untouched unless everything checks
     * out and the single transaction commits.
     */
    suspend fun restoreEncrypted(bytes: ByteArray, password: CharArray): BackupPreview {
        val payload = decryptPayload(bytes, password)
        validate(payload)
        val preview = payload.toPreview()

        db.withTransaction {
            db.clearAllTables()
            // Insert in FK-safe order, preserving original ids.
            payload.accounts.forEach { db.accountDao().insert(it.toEntity()) }
            payload.categories.forEach { db.categoryDao().insert(it.toEntity()) }
            payload.parties.forEach { db.partyDao().insert(it.toEntity()) }
            payload.shops.forEach { db.shopDao().insert(it.toEntity()) }
            payload.loans.forEach { db.loanDao().insert(it.toEntity()) }
            payload.savingsGoals.forEach { db.savingsGoalDao().insert(it.toEntity()) }
            payload.transactions.forEach { db.transactionDao().insertTransaction(it.toEntity()) }
            if (payload.postings.isNotEmpty()) {
                db.transactionDao().insertPostings(payload.postings.map { it.toEntity() })
            }
            if (payload.transactionItems.isNotEmpty()) {
                db.transactionDao().insertItems(payload.transactionItems.map { it.toEntity() })
            }
            if (payload.installments.isNotEmpty()) {
                db.installmentDao().insertAll(payload.installments.map { it.toEntity() })
            }
            if (payload.installmentAllocations.isNotEmpty()) {
                db.transactionDao().insertAllocations(payload.installmentAllocations.map { it.toEntity() })
            }
            payload.budgets.forEach { db.budgetDao().insert(it.toEntity()) }
            payload.reminders.forEach { db.reminderDao().insert(it.toEntity()) }
            payload.recurringRules.forEach { db.recurringRuleDao().insert(it.toEntity()) }
            payload.netWorthItems.forEach { db.netWorthItemDao().insert(it.toEntity()) }
            payload.attachments.forEach { db.transactionDao().insertAttachment(it.toEntity()) }
            payload.auditEvents.forEach { db.auditDao().insert(it.toEntity()) }

            // Final integrity gate inside the transaction: rollback on any problem.
            val unbalanced = db.postingDao().countUnbalancedTransactions()
            if (unbalanced != 0) {
                throw BackupException("ব্যাকআপের হিসাব মিলছে না — পুনরুদ্ধার বাতিল করা হয়েছে। আপনার বর্তমান তথ্য অপরিবর্তিত আছে।")
            }
        }

        writeReceiptFiles(payload.attachments)
        audit.log(AuditLogger.RESTORE, "backup", 0, "ব্যাকআপ থেকে পুনরুদ্ধার সম্পন্ন")
        return preview
    }

    // ---------------- internals ----------------

    private fun decryptPayload(bytes: ByteArray, password: CharArray): BackupPayload {
        val envelope = try {
            json.decodeFromString(BackupEnvelope.serializer(), bytes.decodeToString())
        } catch (e: Exception) {
            throw BackupException("এটি সঠিক হিসাবঘর ব্যাকআপ ফাইল নয়।")
        }
        if (envelope.format != BackupEnvelope.FORMAT) {
            throw BackupException("এটি সঠিক হিসাবঘর ব্যাকআপ ফাইল নয়।")
        }
        if (envelope.version > 1) {
            throw BackupException("এই ব্যাকআপটি নতুন সংস্করণে তৈরি — আগে অ্যাপটি আপডেট করুন।")
        }
        val blob = try {
            EncryptedBlob(
                salt = Base64.getDecoder().decode(envelope.saltB64),
                iv = Base64.getDecoder().decode(envelope.ivB64),
                cipherText = Base64.getDecoder().decode(envelope.dataB64),
                iterations = envelope.kdfIterations
            )
        } catch (e: Exception) {
            throw BackupException("ব্যাকআপ ফাইলটি নষ্ট হয়েছে।")
        }
        val plain = try {
            BackupCrypto.decrypt(blob, password)
        } catch (e: AEADBadTagException) {
            throw BackupException("পাসওয়ার্ড সঠিক নয়, অথবা ফাইলটি পরিবর্তিত/নষ্ট হয়েছে।")
        } catch (e: Exception) {
            throw BackupException("ব্যাকআপ খুলতে সমস্যা হয়েছে।")
        }
        return try {
            json.decodeFromString(BackupPayload.serializer(), plain.decodeToString())
        } catch (e: Exception) {
            throw BackupException("ব্যাকআপের ভেতরের তথ্য পড়া যাচ্ছে না।")
        }
    }

    /**
     * Structural validation before touching the database: enum names parse,
     * postings balance per transaction, and foreign references exist.
     */
    internal fun validate(payload: BackupPayload) {
        if (payload.schemaVersion > 1) {
            throw BackupException("এই ব্যাকআপের স্কিমা এই অ্যাপ সংস্করণের চেয়ে নতুন।")
        }
        // Enum parsing happens during toEntity(); force it now for key tables.
        try {
            payload.accounts.forEach { it.toEntity() }
            payload.categories.forEach { it.toEntity() }
            payload.loans.forEach { it.toEntity() }
            payload.transactions.forEach { it.toEntity() }
            payload.postings.forEach { it.toEntity() }
        } catch (e: IllegalArgumentException) {
            throw BackupException("ব্যাকআপে অজানা ধরনের তথ্য আছে — অ্যাপ আপডেট করে আবার চেষ্টা করুন।")
        }
        val txnIds = payload.transactions.map { it.id }.toHashSet()
        if (txnIds.size != payload.transactions.size) {
            throw BackupException("ব্যাকআপে ডুপ্লিকেট লেনদেন পাওয়া গেছে।")
        }
        val sums = HashMap<Long, Long>()
        for (p in payload.postings) {
            if (p.transactionId !in txnIds) {
                throw BackupException("ব্যাকআপে ভাঙা হিসাব-এন্ট্রি পাওয়া গেছে।")
            }
            sums[p.transactionId] = (sums[p.transactionId] ?: 0L) + p.signedAmountPoisha
        }
        if (sums.values.any { it != 0L }) {
            throw BackupException("ব্যাকআপের কিছু লেনদেনের হিসাব মিলছে না।")
        }
        val loanIds = payload.loans.map { it.id }.toHashSet()
        if (payload.installments.any { it.loanId !in loanIds }) {
            throw BackupException("ব্যাকআপে ভাঙা কিস্তি তথ্য পাওয়া গেছে।")
        }
    }

    private fun BackupPayload.toPreview() = BackupPreview(
        createdAtEpochMs = createdAtEpochMs,
        appVersion = appVersion,
        schemaVersion = schemaVersion,
        accountCount = accounts.size,
        transactionCount = transactions.count { it.voidedAt == null },
        shopCount = shops.size,
        loanCount = loans.size,
        attachmentFilesIncluded = attachments.count { it.contentB64 != null },
        attachmentFilesMissing = attachments.count { it.contentB64 == null }
    )

    private fun readReceiptBase64(entity: AttachmentEntity): String? {
        val file = File(receiptsDir, entity.fileName)
        if (!file.exists() || file.length() > MAX_RECEIPT_BYTES) return null
        return try {
            Base64.getEncoder().encodeToString(file.readBytes())
        } catch (e: Exception) {
            null
        }
    }

    private fun writeReceiptFiles(attachments: List<AttachmentDto>) {
        try {
            if (!receiptsDir.exists()) receiptsDir.mkdirs()
            // Remove files of the replaced dataset, then write restored ones.
            receiptsDir.listFiles()?.forEach { it.delete() }
            for (a in attachments) {
                val content = a.contentB64 ?: continue
                try {
                    File(receiptsDir, a.fileName).writeBytes(Base64.getDecoder().decode(content))
                } catch (e: Exception) {
                    // Best effort: metadata exists; missing file is shown as unavailable.
                }
            }
        } catch (e: Exception) {
            // Receipt files are secondary; the financial restore already succeeded.
        }
    }

    companion object {
        private const val MAX_RECEIPT_BYTES = 5L * 1024 * 1024
    }
}
