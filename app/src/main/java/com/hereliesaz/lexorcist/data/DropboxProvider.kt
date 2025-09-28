package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.auth.DropboxAuthManager
import com.hereliesaz.lexorcist.utils.Result
import com.dropbox.core.v2.files.WriteMode
import com.hereliesaz.lexorcist.model.CloudUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.inject.Inject

class DropboxProvider @Inject constructor(
    private val dropboxAuthManager: DropboxAuthManager
) : CloudStorageProvider {

    override suspend fun getCurrentUser(): Result<CloudUser> = withContext(Dispatchers.IO) {
        val client = dropboxAuthManager.getClient()
        if (client == null) {
            return@withContext Result.Error(Exception("Dropbox client not initialized."))
        }

        try {
            val account = client.users().currentAccount
            Result.Success(CloudUser(account.accountId, account.email, account.name.displayName, account.profilePhotoUrl))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getRootFolderId(): Result<String> {
        // For Dropbox, the root folder is just ""
        return Result.Success("")
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> = withContext(Dispatchers.IO) {
        val client = dropboxAuthManager.getClient()
        if (client == null) {
            return@withContext Result.Error(Exception("Dropbox client not initialized. Please connect to Dropbox first."))
        }

        try {
            val result = client.files().listFolder(folderId)
            val files = result.entries.map {
                CloudFile(it.pathLower ?: "", it.name ?: "", (it as? com.dropbox.core.v2.files.FileMetadata)?.clientModified?.time ?: 0)
            }
            Result.Success(files)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val client = dropboxAuthManager.getClient()
        if (client == null) {
            return@withContext Result.Error(Exception("Dropbox client not initialized. Please connect to Dropbox first."))
        }

        try {
            val outputStream = java.io.ByteArrayOutputStream()
            client.files().download(fileId).download(outputStream)
            Result.Success(outputStream.toByteArray())
        } catch (e: Exception) {
            Result.Error(e)
        }
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

            Result.Success(CloudFile(path, uploadedFile.name ?: "", uploadedFile.clientModified.time)) // Added ?: "" for name
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> = withContext(Dispatchers.IO) {
        val client = dropboxAuthManager.getClient()
        if (client == null) {
            return@withContext Result.Error(Exception("Dropbox client not initialized. Please connect to Dropbox first."))
        }

        try {
            val path = fileId // The fileId is the path
            val inputStream = ByteArrayInputStream(content)
            val uploadedFile = client.files().uploadBuilder(path)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(inputStream)

            Result.Success(CloudFile(path, uploadedFile.name ?: "", uploadedFile.clientModified.time)) // Added ?: "" for name
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> = withContext(Dispatchers.IO) {
        val client = dropboxAuthManager.getClient()
        if (client == null) {
            return@withContext Result.Error(Exception("Dropbox client not initialized. Please connect to Dropbox first."))
        }

        try {
            val metadata = client.files().getMetadata(fileId)
            val fileMetadata = metadata as com.dropbox.core.v2.files.FileMetadata
            Result.Success(CloudFile(fileMetadata.pathLower ?: "", fileMetadata.name ?: "", fileMetadata.clientModified.time))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> = withContext(Dispatchers.IO) {
        val client = dropboxAuthManager.getClient()
        if (client == null) {
            return@withContext Result.Error(Exception("Dropbox client not initialized. Please connect to Dropbox first."))
        }

        try {
            val path = if (parentFolderId.isEmpty()) "/$folderName" else "/$parentFolderId/$folderName"
            val createdFolder = client.files().createFolderV2(path)
            Result.Success(createdFolder.metadata.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
