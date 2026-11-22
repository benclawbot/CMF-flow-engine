package com.benclawbot.cmfflow.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Versioned, authenticated offline archive. All bytes before the ciphertext
 * are AES-GCM authenticated additional data; modifying KDF metadata invalidates
 * the archive. The plaintext is intentionally bounded before allocation.
 *
 * Android API 28's platform JCE supports PBKDF2-HMAC-SHA256. We use 600,000
 * iterations as the documented platform-compatible alternative to Argon2id.
 */
object EncryptedBackupCodec {
    const val MAX_PLAINTEXT_BYTES = 8 * 1024 * 1024
    const val MAX_ARCHIVE_BYTES = MAX_PLAINTEXT_BYTES + 128
    private val magic = "CMFBK001".toByteArray(Charsets.US_ASCII)
    private const val version: Byte = 1
    private const val iterations = 600_000
    private const val saltBytes = 16
    private const val nonceBytes = 12
    private const val headerBytes = 8 + 1 + 4 + saltBytes + nonceBytes
    private const val tagBits = 128

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        require(password.size >= 12) { "Use a passphrase of at least 12 characters" }
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) { "Backup data exceeds the 8 MiB limit" }
        val salt = ByteArray(saltBytes).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(nonceBytes).also(SecureRandom()::nextBytes)
        val header = ByteBuffer.allocate(headerBytes)
            .put(magic).put(version).putInt(iterations).put(salt).put(nonce).array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt, iterations), GCMParameterSpec(tagBits, nonce))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plaintext)
    }

    fun decrypt(archive: ByteArray, password: CharArray): ByteArray {
        require(archive.size in (headerBytes + 16)..MAX_ARCHIVE_BYTES) {
            "Invalid or oversized CMF Flow encrypted backup"
        }
        val bytes = ByteBuffer.wrap(archive)
        val observedMagic = ByteArray(magic.size).also(bytes::get)
        require(observedMagic.contentEquals(magic)) { "Not a CMF Flow encrypted backup" }
        require(bytes.get() == version) { "Unsupported encrypted backup version" }
        val rounds = bytes.int
        require(rounds in 500_000..1_000_000) { "Unsupported backup key-derivation settings" }
        val salt = ByteArray(saltBytes).also(bytes::get)
        val nonce = ByteArray(nonceBytes).also(bytes::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt, rounds), GCMParameterSpec(tagBits, nonce))
        cipher.updateAAD(archive, 0, headerBytes)
        return try {
            val plaintext = cipher.doFinal(archive, headerBytes, archive.size - headerBytes)
            require(plaintext.size <= MAX_PLAINTEXT_BYTES) { "Backup data exceeds the 8 MiB limit" }
            plaintext
        } catch (error: AEADBadTagException) {
            throw IllegalArgumentException("Incorrect passphrase or damaged backup", error)
        }
    }

    private fun derive(password: CharArray, salt: ByteArray, rounds: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, rounds, 256)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES",
            )
        } finally {
            spec.clearPassword()
        }
    }
}
