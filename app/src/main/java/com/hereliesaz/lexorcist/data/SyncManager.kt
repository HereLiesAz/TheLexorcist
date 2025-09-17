package com.hereliesaz.lexorcist.data

import android.content.Context
import android.net.Uri
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val storageDir: File by lazy {
        val customLocation = settingsManager.getStorageLocation()
        val dir = if (customLocation != null) {
            File(Uri.parse(customLocation).path!!)
        } else {
            context.getExternalFilesDir(null) ?: context.filesDir
        }
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val spreadsheetFile: File by lazy { File(storageDir, "lexorcist_data.xlsx") }

    suspend fun synchronize(cloudStorageProvider: CloudStorageProvider, localFileStorageService: LocalFileStorageService): Result<Unit> = withContext(Dispatchers.IO) {
        if (!spreadsheetFile.exists()) {
            return@withContext Result.Success(Unit) // Nothing to sync
        }

        val rootFolderIdResult = cloudStorageProvider.getRootFolderId()
        if (rootFolderIdResult is Result.Error) {
            return@withContext rootFolderIdResult
        }
        if (rootFolderIdResult is Result.UserRecoverableError) {
            return@withContext rootFolderIdResult
        }

        val rootFolderId = (rootFolderIdResult as Result.Success).data

        val filesResult = cloudStorageProvider.listFiles(rootFolderId)
        if (filesResult is Result.Error) {
            return@withContext filesResult
        }
        if (filesResult is Result.UserRecoverableError) {
            return@withContext filesResult
        }

        val cloudFiles = (filesResult as Result.Success).data
        val existingCloudFile = cloudFiles.find { it.name == "lexorcist_data.xlsx" }

        val spreadsheetBytes = spreadsheetFile.readBytes()

        val uploadSpreadsheetResult = if (existingCloudFile != null) {
            // Check for modification time before uploading
            if (spreadsheetFile.lastModified() > existingCloudFile.modifiedTime) {
                cloudStorageProvider.updateFile(existingCloudFile.id, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", spreadsheetBytes)
            } else {
                Result.Success(existingCloudFile)
            }
        } else {
            cloudStorageProvider.writeFile(rootFolderId, "lexorcist_data.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", spreadsheetBytes)
        }

        if (uploadSpreadsheetResult is Result.Error) {
            return@withContext uploadSpreadsheetResult
        }
        if (uploadSpreadsheetResult is Result.UserRecoverableError) {
            return@withContext uploadSpreadsheetResult
        }

        // TODO: Implement folder synchronization for cases
        val casesResult = localFileStorageService.getAllCases()
        if (casesResult is Result.Success) {
            val cases = casesResult.data
            for (case in cases) {
                val caseFolder = File(storageDir, case.spreadsheetId)
                if (caseFolder.exists() && caseFolder.isDirectory) {
                    // TODO: Implement folder upload logic.
                    // This will involve creating a folder in the cloud and then uploading each file in the local folder.
                }
            }
        } else if (casesResult is Result.Error) {
            return@withContext casesResult
        }

        Result.Success(Unit)
    }
}
