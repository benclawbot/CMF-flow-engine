package com.benclawbot.cmfflow.backup

import org.junit.Assert.*
import org.junit.Test

class EncryptedBackupCodecTest {
    private val password = "correct horse battery staple".toCharArray()

    @Test fun roundTripIsAuthenticatedAndNonDeterministic() {
        val original = "fictional check-in and experiment data".toByteArray()
        val first = EncryptedBackupCodec.encrypt(original, password)
        val second = EncryptedBackupCodec.encrypt(original, password)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(original, EncryptedBackupCodec.decrypt(first, password))
        assertArrayEquals(original, EncryptedBackupCodec.decrypt(second, password))
    }

    @Test fun wrongPassphraseAndTamperedCiphertextFailClosed() {
        val encrypted = EncryptedBackupCodec.encrypt("private fixture".toByteArray(), password)
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(encrypted, "incorrect test phrase".toCharArray())
        }
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 0x01).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(encrypted, password)
        }
    }

    @Test fun tamperedHeaderAndTruncatedArchivesFailClosed() {
        val encrypted = EncryptedBackupCodec.encrypt("fixture".toByteArray(), password)
        encrypted[10] = (encrypted[10].toInt() xor 0x01).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(encrypted, password)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(byteArrayOf(1, 2, 3), password)
        }
    }

    @Test fun weakPassphrasesAndOversizedArchivesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.encrypt("sample".toByteArray(), "short".toCharArray())
        }
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackupCodec.decrypt(
                ByteArray(EncryptedBackupCodec.MAX_ARCHIVE_BYTES + 1),
                password,
            )
        }
    }
}
