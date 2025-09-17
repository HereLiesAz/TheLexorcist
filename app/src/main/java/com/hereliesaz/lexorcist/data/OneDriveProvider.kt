package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.auth.OneDriveAuthManager
import com.hereliesaz.lexorcist.utils.Result
import com.microsoft.graph.authentication.IAuthenticationProvider
import com.microsoft.graph.requests.GraphServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class OneDriveProvider @Inject constructor(
    private val oneDriveAuthManager: OneDriveAuthManager
) : CloudStorageProvider {

    private fun getGraphClient(): GraphServiceClient<out okhttp3.Request>? {
        val accessToken = oneDriveAuthManager.getAccessToken() ?: return null
        val authProvider = object : IAuthenticationProvider {
            override fun getAuthorizationTokenAsync(requestUrl: URL): CompletableFuture<String> {
                val future = CompletableFuture<String>()
                future.complete(accessToken)
                return future
            }
        }
        return GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient()
    }

    override suspend fun getRootFolderId(): Result<String> {
        return Result.Success("root")
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> = withContext(Dispatchers.IO) {
        val graphClient = getGraphClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first."))
        }

        try {
            val result = graphClient.me().drive().items(folderId).children()
                .buildRequest()
                .get()

            if (result != null) {
                val files = result.currentPage.map {
                    CloudFile(it.id ?: "", it.name ?: "", it.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: 0)
                }
                Result.Success(files)
            } else {
                Result.Error(Exception("OneDrive list files failed."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val graphClient = getGraphClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first."))
        }

        try {
            val stream = graphClient.me().drive().items(fileId).content()
                .buildRequest()
                .get()

            if (stream != null) {
                Result.Success(stream.readBytes())
            } else {
                Result.Error(Exception("OneDrive read file failed."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> = withContext(Dispatchers.IO) {
        val graphClient = getGraphClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first."))
        }

        try {
            val uploadedFile = graphClient.me().drive().root().child(fileName).content()
                .buildRequest()
                .put(content)

            if (uploadedFile != null) {
                Result.Success(CloudFile(uploadedFile.id ?: "", uploadedFile.name ?: "", uploadedFile.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: 0))
            } else {
                Result.Error(Exception("OneDrive upload failed."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> = withContext(Dispatchers.IO) {
        val graphClient = getGraphClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first."))
        }

        try {
            val uploadedFile = graphClient.me().drive().items(fileId).content()
                .buildRequest()
                .put(content)

            if (uploadedFile != null) {
                Result.Success(CloudFile(uploadedFile.id ?: "", uploadedFile.name ?: "", uploadedFile.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: 0))
            } else {
                Result.Error(Exception("OneDrive upload failed."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> = withContext(Dispatchers.IO) {
        val graphClient = getGraphClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first."))
        }

        try {
            val result = graphClient.me().drive().items(fileId)
                .buildRequest()
                .get()

            if (result != null) {
                Result.Success(CloudFile(result.id ?: "", result.name ?: "", result.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: 0))
            } else {
                Result.Error(Exception("OneDrive get file metadata failed."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> = withContext(Dispatchers.IO) {
        val graphClient = getGraphClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first."))
        }

        try {
            val folder = com.microsoft.graph.models.DriveItem()
            folder.name = folderName
            folder.folder = com.microsoft.graph.models.Folder()

            val createdFolder = graphClient.me().drive().items(parentFolderId).children()
                .buildRequest()
                .post(folder)

            if (createdFolder != null) {
                Result.Success(createdFolder.id ?: "")
            } else {
                Result.Error(Exception("OneDrive create folder failed."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
