package com.shohankhan.hisabghor.data

import com.shohankhan.hisabghor.core.common.Dates
import com.shohankhan.hisabghor.core.database.AuditDao
import com.shohankhan.hisabghor.core.database.AuditEventEntity

/** Append-only audit trail for edits, voids, restores and security events. */
class AuditLogger(private val auditDao: AuditDao) {

    suspend fun log(eventType: String, entityType: String, entityId: Long, summary: String) {
        auditDao.insert(
            AuditEventEntity(
                atEpochMs = Dates.nowMs(),
                eventType = eventType,
                entityType = entityType,
                entityId = entityId,
                summary = summary
            )
        )
    }

    companion object {
        const val CREATE = "CREATE"
        const val EDIT = "EDIT"
        const val VOID = "VOID"
        const val DELETE = "DELETE"
        const val RESTORE = "RESTORE"
        const val BACKUP = "BACKUP"
        const val PIN_CHANGE = "PIN_CHANGE"
    }
}
