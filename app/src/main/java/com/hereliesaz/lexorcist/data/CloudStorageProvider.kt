package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.utils.Result

interface CloudStorageProvider {
    suspend fun getRootFolderId(): Result<String>
    suspend fun listFiles(folderId: String): Result<List<CloudFile>>
    suspend fun readFile(fileId: String): Result<ByteArray>
    suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile>
    suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile>
    suspend fun getFileMetadata(fileId: String): Result<CloudFile>
}

data class CloudFile(
    val id: String,
    val name: String,
    val modifiedTime: Long
)
