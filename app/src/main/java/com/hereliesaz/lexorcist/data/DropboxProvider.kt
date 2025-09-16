package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.auth.DropboxAuthManager
import com.hereliesaz.lexorcist.utils.Result
import com.dropbox.core.v2.files.WriteMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.inject.Inject

class DropboxProvider @Inject constructor(
    private val dropboxAuthManager: DropboxAuthManager
) : CloudStorageProvider {

    override suspend fun getRootFolderId(): Result<String> {
        // For Dropbox, the root folder is just ""
        return Result.Success("")
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> {
        TODO("Not yet implemented for one-way sync")
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> {
        TODO("Not yet implemented for one-way sync")
    }

    override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> = withContext(Dispatchers.IO) {
        val client = dropboxAuthManager.getClient()
        if (client == null) {
            return@withContext Result.Error(Exception("Dropbox client not initialized. Please connect to Dropbox first."))
        }

        try {
            val path = if (folderId.isEmpty()) "/$fileName" else "/$folderId/$fileName"
            val inputStream = ByteArrayInputStream(content)
            val uploadedFile = client.files().uploadBuilder(path)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(inputStream)

            Result.Success(CloudFile(uploadedFile.id, uploadedFile.name, uploadedFile.clientModified.time))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        TODO("Not yet implemented for one-way sync")
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        TODO("Not yet implemented for one-way sync")
    }
}
