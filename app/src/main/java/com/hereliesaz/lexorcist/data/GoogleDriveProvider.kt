package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.model.CloudUser
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class GoogleDriveProvider @Inject constructor(
    private val googleApiService: GoogleApiService,
    @ApplicationContext private val context: Context
) : CloudStorageProvider {

    override suspend fun getCurrentUser(): Result<CloudUser> {
        return Result.Error(NotImplementedError("getCurrentUser is not implemented for GoogleDriveProvider"))
    }

    override suspend fun getRootFolderId(): Result<String> {
        return googleApiService.getOrCreateAppRootFolder()
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> {
        val result = googleApiService.listFiles(folderId)
        return when (result) {
            is Result.Success -> {
                val cloudFiles = result.data.map {
                    CloudFile(it.id, it.name, it.modifiedTime.value)
                }
                Result.Success(cloudFiles)
            }
            is Result.Error -> Result.Error(result.exception)
            is Result.UserRecoverableError -> Result.UserRecoverableError(result.exception)
        }
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> {
        return googleApiService.downloadFile(fileId)
    }

    override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        val tempFile = File(context.cacheDir, fileName)
        FileOutputStream(tempFile).use { it.write(content) }

        val result = googleApiService.uploadFile(tempFile, folderId, mimeType)
        tempFile.delete() // Clean up the temporary file

        return when (result) {
            is Result.Success -> {
                val uploadedFile = result.data
                if (uploadedFile != null) {
                    Result.Success(CloudFile(uploadedFile.id, uploadedFile.name, System.currentTimeMillis()))
                } else {
                    Result.Error(Exception("Google Drive upload failed to return a file."))
                }
            }
            is Result.Error -> Result.Error(result.exception)
            is Result.UserRecoverableError -> Result.UserRecoverableError(result.exception)
        }
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        val tempFile = File(context.cacheDir, "temp_update_file")
        FileOutputStream(tempFile).use { it.write(content) }

        val result = googleApiService.updateFile(fileId, tempFile, mimeType)
        tempFile.delete() // Clean up the temporary file

        return when (result) {
            is Result.Success -> {
                val uploadedFile = result.data
                if (uploadedFile != null) {
                    Result.Success(CloudFile(uploadedFile.id, uploadedFile.name, System.currentTimeMillis()))
                } else {
                    Result.Error(Exception("Google Drive update failed to return a file."))
                }
            }
            is Result.Error -> Result.Error(result.exception)
            is Result.UserRecoverableError -> Result.UserRecoverableError(result.exception)
        }
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        val result = googleApiService.getFileMetadata(fileId)
        return when (result) {
            is Result.Success -> {
                val file = result.data
                Result.Success(CloudFile(file.id, file.name, file.modifiedTime.value))
            }
            is Result.Error -> Result.Error(result.exception)
            is Result.UserRecoverableError -> Result.UserRecoverableError(result.exception)
        }
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> {
        return googleApiService.createFolder(folderName, parentFolderId)
    }
}
