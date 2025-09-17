package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.utils.Result
import javax.inject.Inject

// Placeholder implementation
class GoogleDriveCloudStorageProvider @Inject constructor() : CloudStorageProvider {
    override suspend fun getRootFolderId(): Result<String> {
        return Result.Error(NotImplementedError("GoogleDriveCloudStorageProvider.getRootFolderId not implemented"))
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> {
        return Result.Error(NotImplementedError("GoogleDriveCloudStorageProvider.listFiles not implemented"))
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> {
        return Result.Error(NotImplementedError("GoogleDriveCloudStorageProvider.readFile not implemented"))
    }

    override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        return Result.Error(NotImplementedError("GoogleDriveCloudStorageProvider.writeFile not implemented"))
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        return Result.Error(NotImplementedError("GoogleDriveCloudStorageProvider.updateFile not implemented"))
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        return Result.Error(NotImplementedError("GoogleDriveCloudStorageProvider.getFileMetadata not implemented"))
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> {
        return Result.Error(NotImplementedError("GoogleDriveCloudStorageProvider.createFolder not implemented"))
    }
}