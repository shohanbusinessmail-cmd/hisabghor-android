package com.shohankhan.hisabghor.core.security

import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Stored PIN verifier (never the PIN itself). Serialized, then Keystore-encrypted. */
@Serializable
data class PinRecord(
    val saltB64: String,
    val hashB64: String,
    val iterations: Int
)

/**
 * PIN hashing with PBKDF2-HMAC-SHA256.
 *
 * Parameters (documented in docs/SECURITY.md): 210,000 iterations, 16-byte
 * random salt, 256-bit output. Verification is constant-time. A short PIN is
 * inherently low-entropy, so this verifier is additionally encrypted with an
 * Android Keystore key at rest, and the app enforces escalating retry delays.
 */
object PinHasher {

    const val ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    private val random = SecureRandom()

    fun newRecord(pin: String): PinRecord {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = derive(pin, salt, ITERATIONS)
        return PinRecord(
            saltB64 = Base64.getEncoder().encodeToString(salt),
            hashB64 = Base64.getEncoder().encodeToString(hash),
            iterations = ITERATIONS
        )
    }

    fun verify(pin: String, record: PinRecord): Boolean {
        val salt = Base64.getDecoder().decode(record.saltB64)
        val expected = Base64.getDecoder().decode(record.hashB64)
        val actual = derive(pin, salt, record.iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
