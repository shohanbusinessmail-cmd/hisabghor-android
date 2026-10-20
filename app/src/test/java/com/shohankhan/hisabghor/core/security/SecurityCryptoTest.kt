package com.shohankhan.hisabghor.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class SecurityCryptoTest {

    // ---------- BackupCrypto ----------

    @Test
    fun backup_encryptDecryptRoundtrip_bengaliPayload() {
        val plain = "{\"দোকান\":\"রহিম স্টোর\",\"পরিমাণ\":১০০০}".toByteArray(Charsets.UTF_8)
        val blob = BackupCrypto.encrypt(plain, "সঠিক-পাসওয়ার্ড-১২৩".toCharArray())
        val decrypted = BackupCrypto.decrypt(blob, "সঠিক-পাসওয়ার্ড-১২৩".toCharArray())
        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun backup_wrongPasswordFailsAuthentication() {
        val blob = BackupCrypto.encrypt("secret".toByteArray(), "password-1".toCharArray())
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decrypt(blob, "password-2".toCharArray())
        }
    }

    @Test
    fun backup_tamperedCiphertextFailsAuthentication() {
        val blob = BackupCrypto.encrypt("secret data".toByteArray(), "pw123456".toCharArray())
        val tampered = blob.copy(cipherText = blob.cipherText.clone().apply { this[0] = (this[0] + 1).toByte() })
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decrypt(tampered, "pw123456".toCharArray())
        }
    }

    @Test
    fun backup_eachEncryptionUsesFreshSaltAndIv() {
        val a = BackupCrypto.encrypt("same".toByteArray(), "pw123456".toCharArray())
        val b = BackupCrypto.encrypt("same".toByteArray(), "pw123456".toCharArray())
        assertFalse(a.salt.contentEquals(b.salt))
        assertFalse(a.iv.contentEquals(b.iv))
        assertFalse(a.cipherText.contentEquals(b.cipherText))
    }

    // ---------- PinHasher ----------

    @Test
    fun pin_verifyAcceptsCorrectAndRejectsWrong() {
        val record = PinHasher.newRecord("135790")
        assertTrue(PinHasher.verify("135790", record))
        assertFalse(PinHasher.verify("135791", record))
        assertFalse(PinHasher.verify("", record))
    }

    @Test
    fun pin_samePinDifferentRecordsUseDifferentSalts() {
        val a = PinHasher.newRecord("2580")
        val b = PinHasher.newRecord("2580")
        assertNotEquals(a.saltB64, b.saltB64)
        assertNotEquals(a.hashB64, b.hashB64)
        assertTrue(PinHasher.verify("2580", a))
        assertTrue(PinHasher.verify("2580", b))
    }

    // ---------- Lockout policy ----------

    @Test
    fun lockout_escalatesAndCapsAtThirtyMinutes() {
        assertEquals(0L, SecurityRepository.lockoutDelayMs(1))
        assertEquals(0L, SecurityRepository.lockoutDelayMs(5))
        assertEquals(30_000L, SecurityRepository.lockoutDelayMs(6))
        assertEquals(60_000L, SecurityRepository.lockoutDelayMs(7))
        assertEquals(1_800_000L, SecurityRepository.lockoutDelayMs(50)) // never permanent
    }
}
