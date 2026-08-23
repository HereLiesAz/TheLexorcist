package com.hereliesaz.lexorcist.data.crypto

import com.google.crypto.tink.StreamingAead
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.SeekableByteChannel

/**
 * Encrypts the case database at rest.
 *
 * `lexorcist_data.xlsx` holds every case in the app: parties, court, case
 * numbers, allegations, all OCR'd text and every audio transcript. It was
 * written as a plain OOXML file, so anything that could read the app's storage
 * directory could read the whole practice. `TinkSecureStorage` already
 * protected OAuth tokens this way; the evidence itself was not protected at
 * all.
 *
 * Streaming rather than block AEAD, because the workbook is written by
 * `XSSFWorkbook.write(OutputStream)` and read by `XSSFWorkbook(InputStream)` —
 * a streaming primitive slots into both without buffering the whole database
 * into memory, which on a large case would be an out-of-memory risk on top of
 * the one POI already carries.
 *
 * The implementation is separated from the Android Keystore so the format,
 * round-tripping and migration logic can be tested on the JVM with an
 * in-memory keyset. [AndroidDatabaseCipherProvider] supplies the real key.
 */
interface FileCipher {

    /** False when no key is available; callers must then read and write plaintext. */
    val isEnabled: Boolean

    /** Wraps [sink] so bytes written to the result are encrypted. */
    fun encryptingStream(sink: OutputStream): OutputStream

    /** Wraps [source] so bytes read from the result are decrypted. */
    fun decryptingStream(source: InputStream): InputStream

    /**
     * A random-access view of [file]'s plaintext.
     *
     * Needed because evidence is served to ExoPlayer, Coil and `ExifInterface`
     * through a content provider, and those seek. A pipe-backed descriptor
     * would force either a plaintext copy in the cache -- undoing the point --
     * or the loss of seeking, which breaks video scrubbing. Tink's streaming
     * AEAD is designed for exactly this and decrypts individual segments on
     * demand, so nothing is ever fully materialised.
     */
    fun seekableDecryptingChannel(file: File): SeekableByteChannel

    /**
     * True when [file] can be decrypted with this cipher.
     *
     * Used to tell an already-encrypted file from one written before evidence
     * encryption existed. There is no magic number to test for -- a Tink
     * stream begins with a length byte and a random salt -- so this reads the
     * first plaintext byte and reports whether that succeeded.
     */
    fun canDecrypt(file: File): Boolean

    companion object {
        /**
         * Associated data bound into the ciphertext.
         *
         * Ties a database to this application, so a file cannot be swapped in
         * from elsewhere and silently decrypted.
         */
        val ASSOCIATED_DATA: ByteArray = "com.hereliesaz.lexorcist/database/v1".toByteArray()

        /**
         * Associated data for original evidence files.
         *
         * Distinct from the database's, and paired with a distinct keyset, so
         * that a ciphertext from one store cannot be presented as the other.
         */
        val EVIDENCE_ASSOCIATED_DATA: ByteArray =
            "com.hereliesaz.lexorcist/evidence/v1".toByteArray()

        /** First bytes of a ZIP local file header — an unencrypted `.xlsx`. */
        private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

        /**
         * True when [file] is a plaintext OOXML workbook rather than ciphertext.
         *
         * Used to migrate databases written before encryption existed. A
         * Tink-encrypted stream begins with its own header, never `PK`.
         */
        fun looksLikePlaintextWorkbook(file: File): Boolean {
            if (!file.isFile || file.length() < ZIP_MAGIC.size) return false
            val head = ByteArray(ZIP_MAGIC.size)
            FileInputStream(file).use { input ->
                if (input.read(head) != head.size) return false
            }
            return head.contentEquals(ZIP_MAGIC)
        }
    }
}

/** Reads and writes plaintext. Used when no key could be obtained. */
object NoOpFileCipher : FileCipher {
    override val isEnabled: Boolean = false
    override fun encryptingStream(sink: OutputStream): OutputStream = sink
    override fun decryptingStream(source: InputStream): InputStream = source
    override fun seekableDecryptingChannel(file: File): SeekableByteChannel =
        FileInputStream(file).channel
    override fun canDecrypt(file: File): Boolean = false
}

/**
 * A [FileCipher] backed by any Tink [StreamingAead].
 *
 * Kept free of Android types so tests can drive it with a locally generated
 * keyset; production wires it to an Android Keystore-wrapped one.
 */
class StreamingAeadFileCipher(
    private val streamingAead: StreamingAead,
    private val associatedData: ByteArray = FileCipher.ASSOCIATED_DATA,
) : FileCipher {

    override val isEnabled: Boolean = true

    override fun encryptingStream(sink: OutputStream): OutputStream =
        streamingAead.newEncryptingStream(sink, associatedData)

    override fun decryptingStream(source: InputStream): InputStream =
        streamingAead.newDecryptingStream(source, associatedData)

    override fun seekableDecryptingChannel(file: File): SeekableByteChannel =
        streamingAead.newSeekableDecryptingChannel(
            FileInputStream(file).channel,
            associatedData,
        )

    override fun canDecrypt(file: File): Boolean = try {
        FileInputStream(file).use { raw ->
            decryptingStream(raw).use { it.read() }
        }
        true
    } catch (_: Exception) {
        false
    }

    /**
     * Encrypts [source] into [destination], then deletes [source].
     *
     * Used both when a newly captured file is taken into the case folder and
     * when migrating files written before evidence encryption existed. Writes
     * to a temporary file and renames, so an interruption cannot leave a
     * half-encrypted file that neither path can read.
     */
    fun encryptFile(source: File, destination: File): Boolean {
        val temp = File(destination.parentFile, "${destination.name}.encrypting")
        if (temp.exists()) temp.delete()
        try {
            FileInputStream(source).use { plain ->
                FileOutputStream(temp).use { raw ->
                    // The encrypting stream closes `raw`; the descriptor is
                    // fsynced below on a freshly opened one.
                    encryptingStream(raw).use { cipher -> plain.copyTo(cipher) }
                }
            }
            runCatching { FileOutputStream(temp, true).use { it.fd.sync() } }
            if (temp.length() == 0L) {
                temp.delete()
                return false
            }
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
            if (source.path != destination.path) source.delete()
            return true
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
    }

    /**
     * Rewrites a plaintext database in place as ciphertext.
     *
     * Goes via a temporary file and a rename for the same reason ordinary saves
     * do: a migration interrupted halfway must leave the original readable
     * rather than a half-encrypted file that neither path can open.
     *
     * @return true when a migration happened.
     */
    fun encryptInPlace(file: File): Boolean {
        if (!FileCipher.looksLikePlaintextWorkbook(file)) return false

        val temp = File(file.parentFile, "${file.name}.migrating")
        if (temp.exists()) temp.delete()

        try {
            FileInputStream(file).use { plain ->
                FileOutputStream(temp).use { raw ->
                    // The encrypting stream closes `raw`, so the descriptor
                    // cannot be fsynced afterwards; that is done below on a
                    // freshly opened one.
                    encryptingStream(raw).use { cipher -> plain.copyTo(cipher) }
                }
            }
            runCatching { FileOutputStream(temp, true).use { it.fd.sync() } }
            if (temp.length() == 0L) {
                temp.delete()
                return false
            }
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
            return true
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
    }
}
