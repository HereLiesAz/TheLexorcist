package com.hereliesaz.lexorcist.data.crypto

import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import kotlin.random.Random

/**
 * Evidence files at rest.
 *
 * The important properties are that the plaintext is not recoverable from the
 * file, that reads can still seek -- the content provider serves video and EXIF
 * through a random-access descriptor -- and that a file written before
 * encryption existed is still readable rather than lost.
 */
class EvidenceEncryptionTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun cipher(aad: ByteArray = FileCipher.EVIDENCE_ASSOCIATED_DATA): StreamingAeadFileCipher {
        StreamingAeadConfig.register()
        return StreamingAeadFileCipher(
            KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
                .getPrimitive(StreamingAead::class.java),
            aad,
        )
    }

    private fun fileOf(bytes: ByteArray, name: String = "evidence.jpg"): File =
        temp.newFile(name).apply { writeBytes(bytes) }

    @Test
    fun `an encrypted evidence file round-trips`() {
        val c = cipher()
        val original = Random(7).nextBytes(200_000)
        val file = fileOf(original)

        assertTrue(c.encryptFile(file, file))
        assertTrue(c.canDecrypt(file))

        val decrypted = c.seekableDecryptingChannel(file).use { channel ->
            val buffer = ByteBuffer.allocate(channel.size().toInt())
            while (buffer.hasRemaining() && channel.read(buffer) > 0) Unit
            buffer.array()
        }
        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `the plaintext is not present in the file`() {
        val c = cipher()
        val secret = "PRIVILEGED: Smith v Jones settlement figure".toByteArray()
        val file = fileOf(secret, "note.txt")
        c.encryptFile(file, file)

        val onDisk = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(onDisk.contains("PRIVILEGED"))
        assertFalse(onDisk.contains("Smith v Jones"))
    }

    @Test
    fun `reads can seek`() {
        // The content provider serves reads through openProxyFileDescriptor,
        // which calls onRead with arbitrary offsets. A pipe could not do this,
        // and video scrubbing and ExifInterface both depend on it.
        val c = cipher()
        val original = Random(11).nextBytes(100_000)
        val file = fileOf(original)
        c.encryptFile(file, file)

        c.seekableDecryptingChannel(file).use { channel ->
            assertEquals(original.size.toLong(), channel.size())
            listOf(0L, 4_095L, 4_096L, 50_000L, 99_000L).forEach { offset ->
                channel.position(offset)
                val buffer = ByteBuffer.allocate(64)
                while (buffer.hasRemaining() && channel.read(buffer) > 0) Unit
                assertArrayEquals(
                    "mismatch at offset $offset",
                    original.copyOfRange(offset.toInt(), offset.toInt() + 64),
                    buffer.array(),
                )
            }
        }
    }

    @Test
    fun `a file written before encryption is recognised as plaintext`() {
        val c = cipher()
        val file = fileOf("an old unencrypted screenshot".toByteArray())
        // The provider uses this to decide whether to decrypt or serve directly.
        assertFalse(c.canDecrypt(file))
    }

    @Test
    fun `evidence cannot be decrypted with the database key`() {
        // Distinct associated data, so a file moved between the two stores
        // fails authentication rather than being served to the wrong reader.
        StreamingAeadConfig.register()
        val key = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
            .getPrimitive(StreamingAead::class.java)
        val asEvidence = StreamingAeadFileCipher(key, FileCipher.EVIDENCE_ASSOCIATED_DATA)
        val asDatabase = StreamingAeadFileCipher(key, FileCipher.ASSOCIATED_DATA)

        val file = fileOf("evidence".toByteArray())
        asEvidence.encryptFile(file, file)

        assertTrue(asEvidence.canDecrypt(file))
        assertFalse("same key, wrong associated data must fail", asDatabase.canDecrypt(file))
    }

    @Test
    fun `an interrupted encryption leaves the original readable`() {
        val c = cipher()
        val original = "still readable".toByteArray()
        val file = fileOf(original)
        // A crash between writing the temporary file and renaming it.
        File(file.parentFile, "${file.name}.encrypting").writeBytes(byteArrayOf(1, 2, 3))

        assertArrayEquals(original, file.readBytes())
        assertFalse(c.canDecrypt(file))
    }

    @Test
    fun `a zero-length evidence file reports a zero size`() {
        // Tink's decrypting channel cannot report a size for an empty payload:
        // no read against it ever succeeds, so size() keeps throwing "cannot
        // determine size before first read()-call". plaintextSize covers it.
        val c = cipher()
        val file = temp.newFile("empty.bin")
        assertTrue(c.encryptFile(file, file))
        assertEquals(0L, c.plaintextSize(file))
    }

    @Test
    fun `size is available before anything is read`() {
        // The content provider serves reads through openProxyFileDescriptor,
        // whose onGetSize the system calls before any onRead. An unprimed Tink
        // channel throws there, which would have failed every evidence read.
        val c = cipher()
        val file = fileOf(Random(3).nextBytes(9_000))
        c.encryptFile(file, file)

        c.seekableDecryptingChannel(file).use { channel ->
            assertEquals(9_000L, channel.size())
        }
        assertEquals(9_000L, c.plaintextSize(file))
    }
}
