package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.utils.Result

import com.hereliesaz.lexorcist.model.CloudUser

interface CloudStorageProvider {
    suspend fun getCurrentUser(): Result<CloudUser>
    suspend fun getRootFolderId(): Result<String>
    suspend fun listFiles(folderId: String): Result<List<CloudFile>>
    suspend fun readFile(fileId: String): Result<ByteArray>
    suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile>
    suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile>
    suspend fun getFileMetadata(fileId: String): Result<CloudFile>
    suspend fun createFolder(folderName: String, parentFolderId: String): Result<String>
}

data class CloudFile(
    val id: String,
    val name: String,
    val modifiedTime: Long
)
