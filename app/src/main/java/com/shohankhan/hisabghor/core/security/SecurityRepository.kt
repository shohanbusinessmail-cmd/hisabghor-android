package com.shohankhan.hisabghor.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shohankhan.hisabghor.core.common.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.hisabGhorDataStore: DataStore<Preferences> by preferencesDataStore(name = "hisabghor_settings")

/** General (non-security) app preferences. */
data class AppSettings(
    val hideAmountsOnDashboard: Boolean = false,
    val reminderLeadDays: Int = 2,
    val lastBackupAtMs: Long? = null
)

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data class Wrong(val attemptsBeforeDelay: Int) : PinVerifyResult
    data class LockedOut(val untilMs: Long) : PinVerifyResult
    data object NotConfigured : PinVerifyResult
}

/**
 * PIN lifecycle and app settings, persisted in DataStore.
 *
 * Retry policy: 5 free attempts, then escalating delays
 * (30s → 1m → 2m → 5m → 15m → 30m cap). There is deliberately **no** permanent
 * lockout and no master PIN — see docs/SECURITY.md for the honest recovery
 * story (forgotten PIN = reinstall + restore from encrypted backup).
 */
class SecurityRepository(
    private val dataStore: DataStore<Preferences>,
    private val keystoreCrypto: KeystoreCrypto
) {

    private val json = Json { ignoreUnknownKeys = true }

    val isPinSet: Flow<Boolean> = dataStore.data.map { it[KEY_PIN_VERIFIER] != null }

    val biometricEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_BIOMETRIC] ?: false }

    /** Minutes of background time before the app locks again. 0 = immediately. */
    val autoLockMinutes: Flow<Int> = dataStore.data.map { it[KEY_AUTOLOCK_MIN] ?: 5 }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            hideAmountsOnDashboard = prefs[KEY_HIDE_AMOUNTS] ?: false,
            reminderLeadDays = prefs[KEY_REMINDER_LEAD_DAYS] ?: 2,
            lastBackupAtMs = prefs[KEY_LAST_BACKUP]
        )
    }

    suspend fun setPin(pin: String) {
        val record = PinHasher.newRecord(pin)
        val encrypted = keystoreCrypto.encrypt(json.encodeToString(PinRecord.serializer(), record).toByteArray())
        dataStore.edit { prefs ->
            prefs[KEY_PIN_VERIFIER] = encrypted
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun verifyPin(pin: String): PinVerifyResult {
        val prefs = dataStore.data.first()
        val now = Dates.nowMs()
        val lockoutUntil = prefs[KEY_LOCKOUT_UNTIL] ?: 0L
        if (now < lockoutUntil) return PinVerifyResult.LockedOut(lockoutUntil)

        val record = readRecord() ?: return PinVerifyResult.NotConfigured
        return if (PinHasher.verify(pin, record)) {
            dataStore.edit {
                it[KEY_FAILED_ATTEMPTS] = 0
                it[KEY_LOCKOUT_UNTIL] = 0L
            }
            PinVerifyResult.Success
        } else {
            val fails = (prefs[KEY_FAILED_ATTEMPTS] ?: 0) + 1
            val delayMs = lockoutDelayMs(fails)
            dataStore.edit {
                it[KEY_FAILED_ATTEMPTS] = fails
                it[KEY_LOCKOUT_UNTIL] = if (delayMs > 0) now + delayMs else 0L
            }
            if (delayMs > 0) PinVerifyResult.LockedOut(now + delayMs)
            else PinVerifyResult.Wrong(FREE_ATTEMPTS - fails)
        }
    }

    suspend fun lockoutRemainingMs(): Long {
        val until = dataStore.data.first()[KEY_LOCKOUT_UNTIL] ?: 0L
        return (until - Dates.nowMs()).coerceAtLeast(0)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC] = enabled }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        dataStore.edit { it[KEY_AUTOLOCK_MIN] = minutes }
    }

    suspend fun setHideAmounts(hide: Boolean) {
        dataStore.edit { it[KEY_HIDE_AMOUNTS] = hide }
    }

    suspend fun setReminderLeadDays(days: Int) {
        dataStore.edit { it[KEY_REMINDER_LEAD_DAYS] = days.coerceIn(0, 14) }
    }

    suspend fun markBackupDone(atMs: Long) {
        dataStore.edit { it[KEY_LAST_BACKUP] = atMs }
    }

    private suspend fun readRecord(): PinRecord? {
        val encrypted = dataStore.data.first()[KEY_PIN_VERIFIER] ?: return null
        return try {
            val plain = keystoreCrypto.decrypt(encrypted).decodeToString()
            json.decodeFromString(PinRecord.serializer(), plain)
        } catch (e: Exception) {
            // Keystore key lost (e.g. device reset) — verifier unusable.
            // Treated as "not configured" so the user can set a new PIN;
            // their financial data itself is unaffected (see SECURITY.md).
            null
        }
    }

    companion object {
        private const val FREE_ATTEMPTS = 5

        internal fun lockoutDelayMs(failedAttempts: Int): Long = when {
            failedAttempts <= FREE_ATTEMPTS -> 0L
            failedAttempts == 6 -> 30_000L
            failedAttempts == 7 -> 60_000L
            failedAttempts == 8 -> 120_000L
            failedAttempts == 9 -> 300_000L
            failedAttempts == 10 -> 900_000L
            else -> 1_800_000L // 30-minute cap, never permanent
        }

        private val KEY_PIN_VERIFIER = stringPreferencesKey("pin_verifier_enc")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_AUTOLOCK_MIN = intPreferencesKey("autolock_minutes")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        private val KEY_LOCKOUT_UNTIL = longPreferencesKey("lockout_until_ms")
        private val KEY_HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        private val KEY_REMINDER_LEAD_DAYS = intPreferencesKey("reminder_lead_days")
        private val KEY_LAST_BACKUP = longPreferencesKey("last_backup_at_ms")
    }
}
