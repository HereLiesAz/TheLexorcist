package com.hereliesaz.lexorcist.data.crypto

import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.GeneralSecurityException

/**
 * Round-trip, tamper and migration tests for database encryption.
 *
 * Uses a real Tink `StreamingAead` from a locally generated keyset rather than
 * a mock, so the actual ciphertext format is exercised. The only thing the
 * Android build adds is where the key comes from — the Keystore instead of
 * memory — which is exactly why the cipher was written to take a
 * `StreamingAead` rather than reach for `AndroidKeysetManager` itself.
 */
class DatabaseCipherTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var cipher: StreamingAeadDatabaseCipher

    @Before
    fun setUp() {
        StreamingAeadConfig.register()
        cipher = StreamingAeadDatabaseCipher(newAead())
    }

    private fun newAead(): StreamingAead =
        KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
            .getPrimitive(StreamingAead::class.java)

    private fun workbook(marker: String): XSSFWorkbook = XSSFWorkbook().apply {
        createSheet("Cases").createRow(0).createCell(0).setCellValue(marker)
    }

    private fun writeEncrypted(file: File, wb: XSSFWorkbook) {
        FileOutputStream(file).use { raw ->
            cipher.encryptingStream(raw).use { out -> wb.write(out) }
        }
    }

    private fun readEncryptedMarker(file: File): String =
        FileInputStream(file).use { raw ->
            cipher.decryptingStream(raw).use { plain ->
                XSSFWorkbook(plain).use { it.getSheet("Cases").getRow(0).getCell(0).stringCellValue }
            }
        }

    @Test
    fun `a workbook survives an encrypt-decrypt round trip`() {
        val f = temp.newFile("db.xlsx")
        workbook("Smith v Jones").use { writeEncrypted(f, it) }
        assertEquals("Smith v Jones", readEncryptedMarker(f))
    }

    @Test
    fun `the file on disk is not a readable workbook`() {
        val f = temp.newFile("db.xlsx")
        workbook("privileged").use { writeEncrypted(f, it) }

        // The whole point: anything that can read the directory must not be
        // able to open the case database.
        val opened = runCatching { WorkbookFactory.create(f) }
        assertTrue("the database must not open as a plain workbook", opened.isFailure)

        val bytes = f.readBytes()
        assertNotEquals("PK", String(bytes, 0, 2))
        assertFalse(
            "case text must not appear in the ciphertext",
            String(bytes, Charsets.ISO_8859_1).contains("privileged"),
        )
    }

    @Test
    fun `a different key cannot read the database`() {
        val f = temp.newFile("db.xlsx")
        workbook("confidential").use { writeEncrypted(f, it) }

        val other = StreamingAeadDatabaseCipher(newAead())
        val read = runCatching {
            FileInputStream(f).use { raw -> other.decryptingStream(raw).use { it.readBytes() } }
        }
        assertTrue("another key must not decrypt this database", read.isFailure)
    }

    @Test
    fun `associated data is bound into the ciphertext`() {
        val out = ByteArrayOutputStream()
        cipher.encryptingStream(out).use { it.write("hello".toByteArray()) }

        val wrongContext = StreamingAeadDatabaseCipher(newAead(), "somewhere-else".toByteArray())
        val read = runCatching {
            wrongContext.decryptingStream(ByteArrayInputStream(out.toByteArray())).readBytes()
        }
        assertTrue("ciphertext must not decrypt under different associated data", read.isFailure)
    }

    @Test
    fun `tampered ciphertext is rejected rather than returned`() {
        val f = temp.newFile("db.xlsx")
        workbook("original").use { writeEncrypted(f, it) }

        val bytes = f.readBytes()
        bytes[bytes.size / 2] = (bytes[bytes.size / 2] + 1).toByte()
        f.writeBytes(bytes)

        val read = runCatching { readEncryptedMarker(f) }
        assertTrue("a modified database must fail to decrypt", read.isFailure)
        assertTrue(
            "expected a crypto failure, got ${read.exceptionOrNull()}",
            generateSequence(read.exceptionOrNull()) { it.cause }
                .any { it is GeneralSecurityException || it is java.io.IOException },
        )
    }

    @Test
    fun `a plaintext workbook is recognised`() {
        val f = temp.newFile("plain.xlsx")
        workbook("legacy").use { wb -> FileOutputStream(f).use { wb.write(it) } }
        assertTrue(DatabaseCipher.looksLikePlaintextWorkbook(f))
    }

    @Test
    fun `an encrypted workbook is not mistaken for plaintext`() {
        val f = temp.newFile("db.xlsx")
        workbook("current").use { writeEncrypted(f, it) }
        assertFalse(DatabaseCipher.looksLikePlaintextWorkbook(f))
    }

    @Test
    fun `an empty or missing file is not mistaken for plaintext`() {
        assertFalse(DatabaseCipher.looksLikePlaintextWorkbook(File(temp.root, "nope.xlsx")))
        assertFalse(DatabaseCipher.looksLikePlaintextWorkbook(temp.newFile("empty.xlsx")))
    }

    @Test
    fun `migration encrypts an existing plaintext database without losing it`() {
        val f = temp.newFile("db.xlsx")
        workbook("cases from before the upgrade").use { wb -> FileOutputStream(f).use { wb.write(it) } }

        assertTrue(cipher.encryptInPlace(f))

        assertFalse(DatabaseCipher.looksLikePlaintextWorkbook(f))
        assertEquals("cases from before the upgrade", readEncryptedMarker(f))
        assertFalse("no migration temp file should remain", File(temp.root, "db.xlsx.migrating").exists())
    }

    @Test
    fun `migration is a no-op on an already encrypted database`() {
        val f = temp.newFile("db.xlsx")
        workbook("already done").use { writeEncrypted(f, it) }
        val before = f.readBytes()

        assertFalse(cipher.encryptInPlace(f))
        assertTrue("the file must not be touched", before.contentEquals(f.readBytes()))
        assertEquals("already done", readEncryptedMarker(f))
    }

    @Test
    fun `the no-op cipher passes bytes through unchanged`() {
        val out = ByteArrayOutputStream()
        NoOpDatabaseCipher.encryptingStream(out).write("plain".toByteArray())
        assertEquals("plain", out.toString(Charsets.UTF_8.name()))
        assertFalse(NoOpDatabaseCipher.isEnabled)
        assertEquals(
            "plain",
            NoOpDatabaseCipher.decryptingStream(ByteArrayInputStream("plain".toByteArray()))
                .readBytes().toString(Charsets.UTF_8),
        )
    }
}
