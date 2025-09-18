package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.model.CloudUser
import com.hereliesaz.lexorcist.utils.Result
import javax.inject.Inject

// Placeholder implementation
class GoogleDriveCloudStorageProvider @Inject constructor() : CloudStorageProvider {
    override suspend fun getCurrentUser(): Result<CloudUser> {
        return Result.Error(Exception("GoogleDriveCloudStorageProvider.getCurrentUser not implemented"))
    }

    override suspend fun getRootFolderId(): Result<String> {
        return Result.Error(Exception("GoogleDriveCloudStorageProvider.getRootFolderId not implemented"))
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> {
        return Result.Error(Exception("GoogleDriveCloudStorageProvider.listFiles not implemented"))
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> {
        return Result.Error(Exception("GoogleDriveCloudStorageProvider.readFile not implemented"))
    }

    override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        return Result.Error(Exception("GoogleDriveCloudStorageProvider.writeFile not implemented"))
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        return Result.Error(Exception("GoogleDriveCloudStorageProvider.updateFile not implemented"))
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        return Result.Error(Exception("GoogleDriveCloudStorageProvider.getFileMetadata not implemented"))
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> {
        return Result.Error(Exception("GoogleDriveCloudStorageProvider.createFolder not implemented"))
    }
}
