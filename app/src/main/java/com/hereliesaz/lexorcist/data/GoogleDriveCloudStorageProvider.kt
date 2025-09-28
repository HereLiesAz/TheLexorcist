package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.model.CloudUser
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class GoogleDriveCloudStorageProvider @Inject constructor(
    private val googleApiService: GoogleApiService,
    private val credentialHolder: CredentialHolder,
    @ApplicationContext private val context: Context,
) : CloudStorageProvider {
    override suspend fun getCurrentUser(): Result<CloudUser> {
        val userInfo = credentialHolder.userInfo
        return if (userInfo?.email != null && userInfo.id != null) {
            Result.Success(
                CloudUser(
                    id = userInfo.id,
                    email = userInfo.email,
                    displayName = userInfo.displayName ?: "Unknown User",
                    photoUrl = userInfo.photoUrl,
                ),
            )
        } else {
            Result.Error(Exception("No signed-in user found, or email or id is missing."))
        }
    }

    override suspend fun getRootFolderId(): Result<String> {
        return googleApiService.getOrCreateAppRootFolder()
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> {
        return when (val result = googleApiService.listFiles(folderId)) {
            is Result.Success -> {
                val cloudFiles =
                    result.data.map { driveFile ->
                        CloudFile(
                            id = driveFile.id,
                            name = driveFile.name,
                            modifiedTime = driveFile.modifiedTime.value,
                        )
                    }
                Result.Success(cloudFiles)
            }
            is Result.Error -> result
            is Result.UserRecoverableError -> result
            is Result.Loading -> result
        }
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> {
        return googleApiService.downloadFile(fileId)
    }

    private suspend fun <T> withTempFile(
        fileName: String,
        content: ByteArray,
        block: suspend (File) -> Result<T>,
    ): Result<T> =
        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, fileName)
            try {
                FileOutputStream(tempFile).use { it.write(content) }
                block(tempFile)
            } catch (e: Exception) {
                Result.Error(e)
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }

    override suspend fun writeFile(
        folderId: String,
        fileName: String,
        mimeType: String,
        content: ByteArray,
    ): Result<CloudFile> {
        return withTempFile(fileName, content) { tempFile ->
            when (val result = googleApiService.uploadFile(tempFile, folderId, mimeType)) {
                is Result.Success -> {
                    val driveFile = result.data
                    if (driveFile != null) {
                        // After a successful upload, get the full metadata to get the modifiedTime
                        when (val metadataResult = getFileMetadata(driveFile.id)) {
                            is Result.Success -> Result.Success(metadataResult.data)
                            is Result.Error -> Result.Error(metadataResult.exception) // Propagate error
                            is Result.UserRecoverableError -> metadataResult
                            is Result.Loading -> metadataResult
                        }
                    } else {
                        Result.Error(Exception("Upload returned a null file."))
                    }
                }
                is Result.Error -> result
                is Result.UserRecoverableError -> result
                is Result.Loading -> result
            }
        }
    }

    override suspend fun updateFile(
        fileId: String,
        mimeType: String,
        content: ByteArray,
    ): Result<CloudFile> {
        // The file name for the temp file doesn't matter for the update.
        return withTempFile("update_${System.currentTimeMillis()}", content) { tempFile ->
            when (val result = googleApiService.updateFile(fileId, tempFile, mimeType)) {
                is Result.Success -> {
                    val driveFile = result.data
                    if (driveFile != null) {
                        // After a successful update, get the full metadata
                        when (val metadataResult = getFileMetadata(driveFile.id)) {
                            is Result.Success -> Result.Success(metadataResult.data)
                            is Result.Error -> Result.Error(metadataResult.exception)
                            is Result.UserRecoverableError -> metadataResult
                            is Result.Loading -> metadataResult
                        }
                    } else {
                        Result.Error(Exception("Update returned a null file."))
                    }
                }
                is Result.Error -> result
                is Result.UserRecoverableError -> result
                is Result.Loading -> result
            }
        }
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        return when (val result = googleApiService.getFileMetadata(fileId)) {
            is Result.Success -> {
                val driveFile = result.data
                Result.Success(
                    CloudFile(
                        id = driveFile.id,
                        name = driveFile.name,
                        modifiedTime = driveFile.modifiedTime.value,
                    ),
                )
            }
            is Result.Error -> result
            is Result.UserRecoverableError -> result
            is Result.Loading -> result
        }
    }

    override suspend fun createFolder(
        folderName: String,
        parentFolderId: String,
    ): Result<String> {
        return googleApiService.createFolder(folderName, parentFolderId)
    }
}
