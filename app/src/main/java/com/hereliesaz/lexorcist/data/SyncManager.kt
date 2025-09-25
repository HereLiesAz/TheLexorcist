package com.hereliesaz.lexorcist.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
            File(customLocation.toUri().path!!)
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

        if (existingCloudFile != null) {
            // Compare modification times
            if (spreadsheetFile.lastModified() > existingCloudFile.modifiedTime) {
                // Upload local file
                val spreadsheetBytes = spreadsheetFile.readBytes()
                cloudStorageProvider.updateFile(existingCloudFile.id, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", spreadsheetBytes)
            } else if (spreadsheetFile.lastModified() < existingCloudFile.modifiedTime) {
                // Download remote file
                val downloadResult = cloudStorageProvider.readFile(existingCloudFile.id)
                if (downloadResult is Result.Success) {
                    FileOutputStream(spreadsheetFile).use { it.write(downloadResult.data) }
                }
            }
        } else {
            // Upload local file if it doesn't exist in the cloud
            val spreadsheetBytes = spreadsheetFile.readBytes()
            cloudStorageProvider.writeFile(rootFolderId, "lexorcist_data.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", spreadsheetBytes)
        }

        val casesResult = localFileStorageService.getAllCases()
        if (casesResult is Result.Success) {
            val cases = casesResult.data
            val cloudFoldersResult = cloudStorageProvider.listFiles(rootFolderId)
            if (cloudFoldersResult is Result.Success) {
                val cloudFolders = cloudFoldersResult.data
                for (case in cases) {
                    val caseFolder = File(storageDir, case.spreadsheetId)
                    if (caseFolder.exists() && caseFolder.isDirectory) {
                        var cloudCaseFolder = cloudFolders.find { it.name == case.spreadsheetId }
                        val cloudCaseFolderId: String
                        if (cloudCaseFolder == null) {
                            val createFolderResult = cloudStorageProvider.createFolder(case.spreadsheetId, rootFolderId)
                            if (createFolderResult is Result.Success) {
                                cloudCaseFolderId = createFolderResult.data
                            } else {
                                continue // Skip to next case
                            }
                        } else {
                            cloudCaseFolderId = cloudCaseFolder.id
                        }

                        val localFiles = caseFolder.listFiles() ?: emptyArray()
                        val cloudFilesInFolderResult = cloudStorageProvider.listFiles(cloudCaseFolderId)
                        if (cloudFilesInFolderResult is Result.Success) {
                            val cloudFilesInFolder = cloudFilesInFolderResult.data
                            // Upload new or updated local files
                            for (localFile in localFiles) {
                                if (localFile.isFile) { // Added check here
                                    val cloudFile = cloudFilesInFolder.find { it.name == localFile.name }
                                    if (cloudFile == null) {
                                        // Upload new file
                                        val fileBytes = localFile.readBytes()
                                        val mimeType = getMimeType(localFile)
                                        cloudStorageProvider.writeFile(cloudCaseFolderId, localFile.name, mimeType, fileBytes)
                                    } else {
                                        // Update existing file if modified
                                        if (localFile.lastModified() > cloudFile.modifiedTime) {
                                            val fileBytes = localFile.readBytes()
                                            val mimeType = getMimeType(localFile)
                                            cloudStorageProvider.updateFile(cloudFile.id, mimeType, fileBytes)
                                        }
                                    }
                                }
                            }
                            // Download new or updated remote files
                            for (cloudFile in cloudFilesInFolder) {
                                val localFile = localFiles.find { it.name == cloudFile.name }
                                if (localFile == null) {
                                    // Download new file
                                    val downloadResult = cloudStorageProvider.readFile(cloudFile.id)
                                    if (downloadResult is Result.Success) {
                                        val newLocalFile = File(caseFolder, cloudFile.name)
                                        FileOutputStream(newLocalFile).use { it.write(downloadResult.data) }
                                    }
                                } else {
                                    // Update existing file if modified
                                    // Check if localFile is a file before comparing lastModified and writing
                                    if (localFile.isFile && cloudFile.modifiedTime > localFile.lastModified()) {
                                        val downloadResult = cloudStorageProvider.readFile(cloudFile.id)
                                        if (downloadResult is Result.Success) {
                                            FileOutputStream(localFile).use { it.write(downloadResult.data) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (casesResult is Result.Error) {
            return@withContext casesResult
        }

        Result.Success(Unit)
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "mp4" -> "video/mp4"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}
