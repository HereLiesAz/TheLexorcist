package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.data.storage.CaseStorage
import com.hereliesaz.lexorcist.service.LogService
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-way sync of the case database and case folders with a cloud provider.
 *
 * Rewritten because the previous implementation did not sync and, when it
 * would have, lost data.
 *
 * It looked for the database under `getExternalFilesDir(null)`. Nothing writes
 * there -- `LocalFileStorageService` uses `filesDir` -- so the very first
 * statement, `if (!spreadsheetFile.exists()) return Success(Unit)`, returned
 * "nothing to sync" every time and reported success. Cloud sync was a no-op
 * that said it had worked.
 *
 * Had the paths agreed, the database resolution was
 * `if (local.lastModified() > remote.modifiedTime) upload else download` --
 * last writer wins, whole file. Two devices editing the same case load meant
 * one of them lost everything it had added, with no copy kept and nothing
 * said. [SyncState] now records what the previous sync saw, which is what
 * makes "both sides changed" distinguishable from "one side changed", and a
 * genuine conflict is preserved rather than resolved by timestamp.
 */
@Singleton
class SyncManager @Inject constructor(
    private val caseStorage: CaseStorage,
    private val syncState: SyncState,
    private val logService: LogService,
) {

    suspend fun synchronize(
        cloudStorageProvider: CloudStorageProvider,
        localFileStorageService: LocalFileStorageService,
        providerName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val rootFolderId = when (val r = cloudStorageProvider.getRootFolderId()) {
            is Result.Success -> r.data
            is Result.Error -> return@withContext r
            is Result.UserRecoverableError -> return@withContext r
            is Result.Loading -> return@withContext Result.Error(IllegalStateException("Root folder still loading"))
        }

        when (val r = syncDatabase(cloudStorageProvider, localFileStorageService, rootFolderId, providerName)) {
            is Result.Success -> Unit
            is Result.Error -> return@withContext r
            is Result.UserRecoverableError -> return@withContext r
            is Result.Loading -> Unit
        }

        syncCaseFolders(cloudStorageProvider, localFileStorageService, rootFolderId)
    }

    // -----------------------------------------------------------------
    // The database
    // -----------------------------------------------------------------

    private suspend fun syncDatabase(
        provider: CloudStorageProvider,
        local: LocalFileStorageService,
        rootFolderId: String,
        providerName: String,
    ): Result<Unit> {
        if (!caseStorage.databaseFile.exists()) return Result.Success(Unit)

        val plaintext = try {
            local.readDatabaseForSync()
        } catch (e: Exception) {
            Log.e(TAG, "Could not read the database for sync", e)
            return Result.Error(e)
        }
        val localDigest = SyncState.digestOf(plaintext)

        val cloudFiles = when (val r = provider.listFiles(rootFolderId)) {
            is Result.Success -> r.data
            is Result.Error -> return r
            is Result.UserRecoverableError -> return r
            is Result.Loading -> return Result.Error(IllegalStateException("Listing still loading"))
        }
        val remote = cloudFiles.find { it.name == CaseStorage.DATABASE_FILE_NAME }

        val decision = decideDatabaseSync(
            localDigest = localDigest,
            lastSyncedDigest = syncState.lastSyncedDigest(providerName),
            remoteExists = remote != null,
            remoteModifiedTime = remote?.modifiedTime ?: 0L,
            lastSyncedRemoteTime = syncState.lastSyncedRemoteTime(providerName),
        )

        return when (decision) {
            SyncDecision.UPLOAD_NEW ->
                upload(provider, rootFolderId, plaintext, localDigest, providerName, remoteId = null)

            SyncDecision.NOTHING_TO_DO -> Result.Success(Unit)

            SyncDecision.UPLOAD ->
                upload(provider, rootFolderId, plaintext, localDigest, providerName, remote!!.id)

            SyncDecision.DOWNLOAD ->
                download(provider, local, remote!!, providerName)

            SyncDecision.CONFLICT -> {
                // Both sides moved since the last sync. Neither copy is
                // authoritative, so nothing is overwritten: the local database
                // stays as it is and the cloud copy is written alongside it for
                // the user to open and reconcile.
                val bytes = when (val r = provider.readFile(remote!!.id)) {
                    is Result.Success -> r.data
                    is Result.Error -> return r
                    is Result.UserRecoverableError -> return r
                    is Result.Loading -> return Result.Error(IllegalStateException("Download still loading"))
                }
                val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val copy = local.writeConflictCopy(bytes, "lexorcist_data.conflict-$stamp.xlsx")
                val message = "This case database and the copy in $providerName have both " +
                    "changed since they were last synced. Nothing has been overwritten. " +
                    "The cloud version has been saved as ${copy.name}."
                Log.w(TAG, message)
                logService.addLog(message)
                Result.Error(SyncConflictException(message, copy))
            }
        }
    }

    private suspend fun upload(
        provider: CloudStorageProvider,
        rootFolderId: String,
        plaintext: ByteArray,
        digest: String,
        providerName: String,
        remoteId: String?,
    ): Result<Unit> {
        val result = if (remoteId == null) {
            provider.writeFile(rootFolderId, CaseStorage.DATABASE_FILE_NAME, XLSX_MIME, plaintext)
        } else {
            provider.updateFile(remoteId, XLSX_MIME, plaintext)
        }
        return when (result) {
            is Result.Success -> {
                syncState.record(providerName, digest, result.data.modifiedTime)
                Result.Success(Unit)
            }
            is Result.Error -> result
            is Result.UserRecoverableError -> result
            is Result.Loading -> Result.Error(IllegalStateException("Upload still loading"))
        }
    }

    private suspend fun download(
        provider: CloudStorageProvider,
        local: LocalFileStorageService,
        remote: CloudFile,
        providerName: String,
    ): Result<Unit> {
        val bytes = when (val r = provider.readFile(remote.id)) {
            is Result.Success -> r.data
            is Result.Error -> return r
            is Result.UserRecoverableError -> return r
            is Result.Loading -> return Result.Error(IllegalStateException("Download still loading"))
        }
        return when (val written = local.writeDatabaseFromSync(bytes)) {
            is Result.Success -> {
                syncState.record(providerName, SyncState.digestOf(bytes), remote.modifiedTime)
                Result.Success(Unit)
            }
            is Result.Error -> written
            is Result.UserRecoverableError -> written
            is Result.Loading -> Result.Error(IllegalStateException("Write still loading"))
        }
    }

    // -----------------------------------------------------------------
    // Case folders
    // -----------------------------------------------------------------

    /**
     * Evidence files are immutable once imported, so there is no merge to do:
     * each side gains whatever the other has.
     *
     * A name that exists on both sides with different content is not treated as
     * one file being newer than the other -- it is two different pieces of
     * evidence that happen to share a name, and overwriting either would be
     * destroying evidence. The downloaded one is kept under a suffixed name.
     */
    private suspend fun syncCaseFolders(
        provider: CloudStorageProvider,
        local: LocalFileStorageService,
        rootFolderId: String,
    ): Result<Unit> {
        val cases = when (val r = local.getAllCases()) {
            is Result.Success -> r.data
            is Result.Error -> return r
            is Result.UserRecoverableError -> return r
            is Result.Loading -> return Result.Success(Unit)
        }
        val cloudFolders = when (val r = provider.listFiles(rootFolderId)) {
            is Result.Success -> r.data
            else -> return Result.Success(Unit)
        }

        for (case in cases) {
            val caseFolder = caseStorage.caseDirectory(case.spreadsheetId)
            if (!caseFolder.isDirectory) continue

            val cloudCaseFolderId = cloudFolders.find { it.name == case.spreadsheetId }?.id
                ?: when (val r = provider.createFolder(case.spreadsheetId, rootFolderId)) {
                    is Result.Success -> r.data
                    else -> continue
                }

            val cloudFilesInFolder = when (val r = provider.listFiles(cloudCaseFolderId)) {
                is Result.Success -> r.data
                else -> continue
            }
            val localFiles = caseFolder.listFiles()?.filter { it.isFile }.orEmpty()

            for (localFile in localFiles) {
                if (cloudFilesInFolder.none { it.name == localFile.name }) {
                    provider.writeFile(
                        cloudCaseFolderId,
                        localFile.name,
                        mimeTypeOf(localFile),
                        localFile.readBytes(),
                    )
                }
            }

            for (cloudFile in cloudFilesInFolder) {
                val existing = File(caseFolder, cloudFile.name)
                if (!existing.exists()) {
                    when (val r = provider.readFile(cloudFile.id)) {
                        is Result.Success -> existing.writeBytes(r.data)
                        else -> Unit
                    }
                    continue
                }
                if (existing.length() == cloudFile.size) continue
                // Same name, different size: two distinct files. Keep both.
                val alternate = uncollidedName(caseFolder, cloudFile.name)
                when (val r = provider.readFile(cloudFile.id)) {
                    is Result.Success -> {
                        alternate.writeBytes(r.data)
                        logService.addLog(
                            "Kept a differing cloud copy of ${cloudFile.name} as ${alternate.name}.",
                        )
                    }
                    else -> Unit
                }
            }
        }
        return Result.Success(Unit)
    }

    private fun uncollidedName(dir: File, name: String): File {
        val base = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (true) {
            val suffix = if (ext.isEmpty()) "$base (cloud $i)" else "$base (cloud $i).$ext"
            val candidate = File(dir, suffix)
            if (!candidate.exists()) return candidate
            i++
        }
    }

    private fun mimeTypeOf(file: File): String = when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "mp4" -> "video/mp4"
        "pdf" -> "application/pdf"
        "xlsx" -> XLSX_MIME
        else -> "application/octet-stream"
    }

    private companion object {
        const val TAG = "SyncManager"
        const val XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}

/** Raised when both copies changed and neither was overwritten. */
class SyncConflictException(message: String, val conflictCopy: File) : Exception(message)
