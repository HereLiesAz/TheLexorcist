package com.hereliesaz.lexorcist.data.storage

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.hereliesaz.lexorcist.data.crypto.EvidenceCipherProvider
import com.hereliesaz.lexorcist.data.crypto.FileCipher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

/**
 * Serves original evidence files, decrypting them as they are read.
 *
 * Evidence is stored encrypted, but every consumer in the app reads it through
 * a `Uri`: Coil renders thumbnails, ExoPlayer plays audio and video, ML Kit
 * OCRs images, `ExifInterface` reads capture metadata, `HashingUtils` hashes
 * for integrity, and share intents hand files to other apps. Rewriting all of
 * them to take a decrypting stream would have been a large change to every
 * feature, and would still leave share intents broken.
 *
 * Putting the decryption behind a `content://` URI means none of them change.
 * The only edit at each call site is that `mediaUri` now names this provider
 * rather than the file directly.
 *
 * Reads are served through [StorageManager.openProxyFileDescriptor] rather than
 * a pipe, because a pipe cannot seek and video scrubbing and `ExifInterface`
 * both need to. Tink's seekable decrypting channel decrypts only the segments
 * actually read, so a plaintext copy is never written anywhere.
 *
 * Files written before evidence encryption existed are still plaintext. Rather
 * than migrate them all at once on some unlucky startup, [openFile] serves
 * whatever the file actually is; the write path encrypts from now on and
 * [EvidenceFiles] migrates a case's files when it is next opened.
 */
class EvidenceFileProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun caseStorage(): CaseStorage
        fun evidenceCipherProvider(): EvidenceCipherProvider
    }

    private lateinit var storage: CaseStorage
    private lateinit var cipher: FileCipher
    private var callbackThread: HandlerThread? = null

    override fun onCreate(): Boolean = true

    private fun ensureDeps(context: Context) {
        if (::storage.isInitialized) return
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        storage = deps.caseStorage()
        cipher = deps.evidenceCipherProvider().cipher
    }

    /**
     * Resolves a request URI to a file inside the storage root.
     *
     * Any app holding a grant can call in here, so the path is canonicalised
     * and checked against the root: a `../` sequence must not be able to serve
     * the case database, the Tink keysets or anything else outside the
     * evidence tree.
     */
    private fun resolve(uri: Uri): File {
        val context = context ?: throw FileNotFoundException("No context")
        ensureDeps(context)
        val relative = uri.pathSegments.joinToString("/").ifEmpty {
            throw FileNotFoundException("No path in $uri")
        }
        val file = storage.resolveWithinRoot(relative)
            ?: throw FileNotFoundException("Path escapes the storage root")
        if (!file.isFile) throw FileNotFoundException("No such evidence file")
        return file
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Evidence is read-only through this provider")
        val file = resolve(uri)

        if (!cipher.isEnabled || !cipher.canDecrypt(file)) {
            // Either encryption is unavailable on this device, or this file
            // predates it. Serve it directly rather than failing the read.
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        val storageManager = context!!.getSystemService(StorageManager::class.java)
            ?: throw FileNotFoundException("StorageManager unavailable")
        val thread = callbackThread ?: HandlerThread("evidence-fd").also {
            it.start()
            callbackThread = it
        }
        return storageManager.openProxyFileDescriptor(
            ParcelFileDescriptor.MODE_READ_ONLY,
            DecryptingCallback(cipher.seekableDecryptingChannel(file)),
            Handler(thread.looper),
        )
    }

    private class DecryptingCallback(
        private val channel: SeekableByteChannel,
    ) : ProxyFileDescriptorCallback() {

        override fun onGetSize(): Long = channel.size()

        override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
            channel.position(offset)
            val buffer = ByteBuffer.wrap(data, 0, size)
            var total = 0
            while (buffer.hasRemaining()) {
                val read = channel.read(buffer)
                if (read <= 0) break
                total += read
            }
            return total
        }

        override fun onRelease() {
            runCatching { channel.close() }
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val file = resolve(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val size = if (cipher.isEnabled && cipher.canDecrypt(file)) {
            // The plaintext length, not the file length: a consumer that
            // allocates a buffer from SIZE and then reads must not come up
            // short, and Tink's ciphertext is longer than its plaintext.
            runCatching { cipher.seekableDecryptingChannel(file).use { it.size() } }
                .getOrDefault(file.length())
        } else {
            file.length()
        }
        val row = columns.map { column ->
            when (column) {
                OpenableColumns.DISPLAY_NAME -> file.name
                OpenableColumns.SIZE -> size
                else -> null
            }
        }
        return MatrixCursor(columns, 1).apply { addRow(row) }
    }

    override fun getType(uri: Uri): String {
        val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Evidence is written through LocalFileStorageService")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Evidence is written through LocalFileStorageService")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Evidence is deleted through LocalFileStorageService")

    override fun shutdown() {
        callbackThread?.quitSafely()
        callbackThread = null
        super.shutdown()
    }

    companion object {
        const val AUTHORITY = "com.hereliesaz.lexorcist.evidence"

        /** The URI that serves [file], or null when it is outside the storage root. */
        fun uriFor(storage: CaseStorage, file: File): Uri? {
            val relative = storage.relativize(file) ?: return null
            return Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .apply { relative.split(File.separator).forEach { appendPath(it) } }
                .build()
        }

        /** True when [uri] is one of ours. */
        fun isEvidenceUri(uri: Uri): Boolean =
            uri.scheme == "content" && uri.authority == AUTHORITY
    }
}
