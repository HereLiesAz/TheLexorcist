package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.model.CloudUser
// import com.hereliesaz.lexorcist.service.GoogleApiService // Removed for diagnostics
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class GoogleDriveProvider @Inject constructor(
    // private val googleApiService: GoogleApiService, // Removed for diagnostics
    @ApplicationContext private val context: Context
) : CloudStorageProvider {

    private val placeholderError = Exception("GoogleDriveProvider temporarily disabled")

    override suspend fun getCurrentUser(): Result<CloudUser> {
        return Result.Error(placeholderError)
    }

    override suspend fun getRootFolderId(): Result<String> {
        return Result.Error(placeholderError)
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> {
        return Result.Success(emptyList())
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> {
        return Result.Error(placeholderError)
    }

    override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        return Result.Error(placeholderError)
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        return Result.Error(placeholderError)
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        return Result.Error(placeholderError)
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> {
        return Result.Error(placeholderError)
    }
}
