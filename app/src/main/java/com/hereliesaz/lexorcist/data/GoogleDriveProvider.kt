package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import javax.inject.Inject

class GoogleDriveProvider @Inject constructor(
    private val googleApiService: GoogleApiService
) : CloudStorageProvider {

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
        // This is a bit tricky, as the GoogleApiService.uploadFile takes a java.io.File, not a byte array.
        // I will need to create a temporary file to upload.
        // Or I can modify the GoogleApiService to take a byte array.
        // For now, I will assume it's not possible to upload a byte array directly.
        // This will be a TODO.
        TODO("Not yet implemented")
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        // Similar to writeFile, this will require a temporary file.
        TODO("Not yet implemented")
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        TODO("Not yet implemented")
    }
}
