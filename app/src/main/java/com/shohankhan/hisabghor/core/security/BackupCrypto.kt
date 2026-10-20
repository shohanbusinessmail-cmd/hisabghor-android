package com.shohankhan.hisabghor.core.security

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Result of encrypting a backup payload (all values ready for the envelope JSON). */
data class EncryptedBlob(
    val salt: ByteArray,
    val iv: ByteArray,
    val cipherText: ByteArray,
    val iterations: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBlob) return false
        return iterations == other.iterations &&
            salt.contentEquals(other.salt) &&
            iv.contentEquals(other.iv) &&
            cipherText.contentEquals(other.cipherText)
    }

    override fun hashCode(): Int {
        var result = salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + cipherText.contentHashCode()
        result = 31 * result + iterations
        return result
    }
}

/**
 * Password-based encryption for backup files, independent of the app PIN:
 * PBKDF2-HMAC-SHA256 (310,000 iterations, 16-byte salt) → AES-256-GCM
 * (12-byte random IV, 128-bit tag). Wrong passwords and any tampering fail
 * authentication ([AEADBadTagException]) instead of producing bad data.
 * See docs/BACKUP_FORMAT.md.
 */
object BackupCrypto {

    const val ITERATIONS = 310_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256

    private val random = SecureRandom()

    fun encrypt(plaintext: ByteArray, password: CharArray): EncryptedBlob {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val key = deriveKey(password, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return EncryptedBlob(salt, iv, cipher.doFinal(plaintext), ITERATIONS)
    }

    /** @throws AEADBadTagException when the password is wrong or data was modified. */
    fun decrypt(blob: EncryptedBlob, password: CharArray): ByteArray {
        val key = deriveKey(password, blob.salt, blob.iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob.iv))
        return cipher.doFinal(blob.cipherText)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
