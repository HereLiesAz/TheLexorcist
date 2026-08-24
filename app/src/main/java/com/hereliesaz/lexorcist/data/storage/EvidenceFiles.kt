package com.hereliesaz.lexorcist.data.storage

import android.net.Uri
import android.util.Log
import com.hereliesaz.lexorcist.data.crypto.EvidenceCipherProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes evidence into a case folder as ciphertext, and hands back the URI
 * that reads it.
 *
 * Kept apart from [EvidenceFileProvider] so the write path can be exercised
 * without a running content provider, and apart from `LocalFileStorageService`
 * because that class is already the largest in the project.
 */
@Singleton
class EvidenceFiles @Inject constructor(
    private val storage: CaseStorage,
    private val cipherProvider: EvidenceCipherProvider,
) {

    /**
     * Encrypts [plaintext] in place and returns the URI that serves it.
     *
     * Takes a file that has already been written rather than a stream, because
     * the copy is done by `LocalFileStorageService` from a `Uri` the user
     * picked and that logic -- name sanitising, extension inference, path
     * traversal defence -- should stay in one place.
     *
     * When no key is available the file is left as it is and the URI still
     * works; the provider serves whatever the file actually contains.
     */
    fun protect(plaintext: File): Uri? {
        val cipher = cipherProvider.streamingCipher
        if (cipher != null) {
            try {
                cipher.encryptFile(plaintext, plaintext)
            } catch (e: Exception) {
                // The plaintext is still on disk and readable, so the evidence
                // is not lost; it is simply not protected. Report rather than
                // fail the import.
                Log.e(TAG, "Could not encrypt ${plaintext.name}; storing it unencrypted.", e)
            }
        }
        return EvidenceFileProvider.uriFor(storage, plaintext)
    }

    /**
     * Encrypts any still-plaintext files in a case folder.
     *
     * Files imported before evidence encryption existed stay readable either
     * way -- the provider serves them as they are -- but they are not protected
     * until this runs. Called when a case is opened rather than on a global
     * startup sweep, so a user with many cases does not pay for all of them at
     * once, and so a failure is scoped to the case being opened.
     *
     * @return the number of files newly encrypted.
     */
    fun migrateCase(caseSpreadsheetId: String): Int {
        val cipher = cipherProvider.streamingCipher ?: return 0
        val raw = storage.rawDirectory(caseSpreadsheetId)
        if (!raw.isDirectory) return 0
        var migrated = 0
        raw.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            if (file.name.endsWith(".encrypting")) {
                // An interrupted encryption. The original is still in place.
                file.delete()
                return@forEach
            }
            if (cipher.canDecrypt(file)) return@forEach
            try {
                if (cipher.encryptFile(file, file)) migrated++
            } catch (e: Exception) {
                Log.e(TAG, "Could not encrypt ${file.name}", e)
            }
        }
        return migrated
    }

    private companion object {
        const val TAG = "EvidenceFiles"
    }
}
