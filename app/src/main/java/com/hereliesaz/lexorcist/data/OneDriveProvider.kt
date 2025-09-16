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

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> {
        TODO("Not yet implemented for one-way sync")
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> {
        TODO("Not yet implemented for one-way sync")
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

    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> {
        TODO("Not yet implemented for one-way sync")
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> {
        TODO("Not yet implemented for one-way sync")
    }
}
