package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.utils.Result
import javax.inject.Inject

// Placeholder implementation
class DropboxCloudStorageProvider @Inject constructor() : CloudStorageProvider {
    override suspend fun getRootFolderId(): Result<String> {
        return Result.Error(NotImplementedError("DropboxCloudStorageProvider.getRootFolderId not implemented"))
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> {
        return Result.Error(NotImplementedError("DropboxCloudStorageProvider.listFiles not implemented"))
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> {
        return Result.Error(NotImplementedError("DropboxCloudStorageProvider.readFile not implemented"))
    }

    override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        return Result.Error(NotImplementedError("DropboxCloudStorageProvider.writeFile not implemented"))
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        return Result.Error(NotImplementedError("DropboxCloudStorageProvider.updateFile not implemented"))
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        return Result.Error(NotImplementedError("DropboxCloudStorageProvider.getFileMetadata not implemented"))
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> {
        return Result.Error(NotImplementedError("DropboxCloudStorageProvider.createFolder not implemented"))
    }
}