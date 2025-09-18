package com.hereliesaz.lexorcist.data

import android.util.Log
import com.hereliesaz.lexorcist.auth.OneDriveAuthManager
import com.hereliesaz.lexorcist.utils.Result
import com.microsoft.graph.authentication.IAuthenticationProvider
import com.microsoft.graph.models.DriveItem // Added for explicit DriveItem type
import com.microsoft.graph.models.Folder // Added for explicit Folder type
import com.microsoft.graph.requests.GraphServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class OneDriveProvider @Inject constructor(
    private val oneDriveAuthManager: OneDriveAuthManager
) : CloudStorageProvider {

    private fun getGraphServiceClient(): com.microsoft.graph.requests.IGraphServiceClient? {
        val accessToken = oneDriveAuthManager.getAccessToken()
        if (accessToken.isNullOrEmpty()) {
            Log.e("OneDriveProvider", "OneDrive Access Token is null or empty")
            return null
        }

        val authProvider = IAuthenticationProvider {
            CompletableFuture.completedFuture(accessToken)
        }

        return try {
            GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient()
        } catch (e: Exception) {
            Log.e("OneDriveProvider", "Failed to build GraphServiceClient: ${e.message}", e)
            null
        }
    }

    override suspend fun getRootFolderId(): Result<String> {
        return Result.Success("root") // "root" is the typical ID for the root drive in OneDrive Graph API
    }

    override suspend fun listFiles(folderId: String): Result<List<CloudFile>> = withContext(Dispatchers.IO) {
        val graphClient = this.getGraphServiceClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first or check token."))
        }

        try {
            // Ensure folderId is not empty, if it's "root", use the root drive, otherwise specify items/folderId/children
            val request = if (folderId.equals("root", ignoreCase = true)) {
                graphClient.me().drive().root().children().buildRequest()
            } else {
                graphClient.me().drive().items(folderId).children().buildRequest()
            }
            
            val result = request.get()

            if (result != null) {
                val files = result.currentPage.mapNotNull { driveItem ->
                    // Filter out null id or name, which shouldn't happen for valid drive items
                    driveItem.id?.let { id ->
                        driveItem.name?.let { name ->
                             CloudFile(id, name, driveItem.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: 0)
                        }
                    }
                }
                Result.Success(files)
            } else {
                Result.Error(Exception("OneDrive list files failed: null result from API."))
            }
        } catch (e: Exception) {
            Log.e("OneDriveProvider", "listFiles error: ${e.message}", e)
            Result.Error(e)
        }
    }

    override suspend fun readFile(fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val graphClient = this.getGraphServiceClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first or check token."))
        }

        try {
            val stream = graphClient.me().drive().items(fileId).content()
                .buildRequest()
                .get()

            if (stream != null) {
                // The stream needs to be read. For Graph SDK, it's an InputStream.
                Result.Success(stream.readBytes())
            } else {
                Result.Error(Exception("OneDrive read file failed: null stream from API."))
            }
        } catch (e: Exception) {
            Log.e("OneDriveProvider", "readFile error: ${e.message}", e)
            Result.Error(e)
        }
    }

    // writeFile using raw HTTP since Graph SDK upload can be more complex for simple byte arrays
    // and this implementation was already partially in place.
    override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> = withContext(Dispatchers.IO) {
        val accessToken = oneDriveAuthManager.getAccessToken()
        if (accessToken.isNullOrEmpty()) {
            return@withContext Result.Error(Exception("OneDrive access token not available. Please sign in."))
        }

        // Determine the correct path: if folderId is "root" or empty, upload to root. Otherwise, to specific folder.
        val pathSegment = if (folderId.equals("root", ignoreCase = true) || folderId.isEmpty()) {
            "root:/$fileName:/content"
        } else {
            "items/$folderId:/$fileName:/content" // This path assumes fileName is just the name, not a sub-path
        }
        val urlString = "https://graph.microsoft.com/v1.0/me/drive/$pathSegment"
        
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", mimeType)
            // connection.setRequestProperty("Content-Length", content.size.toString()) // Usually set automatically
            connection.doOutput = true // Indicates that we will write to the connection output stream
            
            connection.outputStream.use { os ->
                os.write(content)
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(response.toString())
                val fileIdResp = jsonResponse.optString("id")
                val nameResp = jsonResponse.optString("name")
                val lastModifiedResp = jsonResponse.optJSONObject("lastModifiedDateTime")?.let { 
                    java.time.OffsetDateTime.parse(it.toString()).toInstant().toEpochMilli()
                } ?: System.currentTimeMillis() // Fallback if not in response
                
                Result.Success(CloudFile(fileIdResp, nameResp, lastModifiedResp))
            } else {
                 val errorStream = connection.errorStream?.bufferedReader()?.readText()
                Log.e("OneDriveProvider", "writeFile error: $responseCode $responseMessage. Details: $errorStream")
                Result.Error(Exception("OneDrive upload failed with response code: $responseCode. Details: $errorStream"))
            }
        } catch (e: Exception) {
            Log.e("OneDriveProvider", "writeFile exception: ${e.message}", e)
            Result.Error(e)
        } finally {
            connection?.disconnect()
        }
    }


    override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> = withContext(Dispatchers.IO) {
        val graphClient = this.getGraphServiceClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first or check token."))
        }

        try {
            val uploadedFile: DriveItem? = graphClient.me().drive().items(fileId).content()
                .buildRequest()
                .put(content) // SDK's put method for content update

            if (uploadedFile != null) {
                 Result.Success(CloudFile(uploadedFile.id ?: "", uploadedFile.name ?: "", uploadedFile.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: 0))
            } else {
                Result.Error(Exception("OneDrive updateFile failed: null result from API."))
            }
        } catch (e: Exception) {
            Log.e("OneDriveProvider", "updateFile error: ${e.message}", e)
            Result.Error(e)
        }
    }

    override suspend fun getFileMetadata(fileId: String): Result<CloudFile> = withContext(Dispatchers.IO) {
        val graphClient = this.getGraphServiceClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first or check token."))
        }

        try {
            val result: DriveItem? = graphClient.me().drive().items(fileId)
                .buildRequest()
                .get()

            if (result != null) {
                 Result.Success(CloudFile(result.id ?: "", result.name ?: "", result.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: 0))
            } else {
                Result.Error(Exception("OneDrive get file metadata failed: null result from API."))
            }
        } catch (e: Exception) {
            Log.e("OneDriveProvider", "getFileMetadata error: ${e.message}", e)
            Result.Error(e)
        }
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> = withContext(Dispatchers.IO) {
        val graphClient = this.getGraphServiceClient()
        if (graphClient == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first or check token."))
        }

        try {
            val driveItem = DriveItem()
            driveItem.name = folderName
            driveItem.folder = Folder()
            // To ensure it doesn't conflict with existing items, you might want to add:
            // driveItem.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive("rename"));
            // However, this requires Gson JsonPrimitive. For simplicity, let's omit conflict behavior for now.

            // Determine parent: if parentFolderId is "root", create in root. Otherwise, in specified parent.
             val request = if (parentFolderId.equals("root", ignoreCase = true) || parentFolderId.isEmpty()) {
                graphClient.me().drive().root().children().buildRequest()
            } else {
                graphClient.me().drive().items(parentFolderId).children().buildRequest()
            }

            val createdFolder: DriveItem? = request.post(driveItem)

            if (createdFolder?.id != null) {
                Result.Success(createdFolder.id!!)
            } else {
                Result.Error(Exception("OneDrive create folder failed: null result or ID from API."))
            }
        } catch (e: Exception) {
            Log.e("OneDriveProvider", "createFolder error: ${e.message}", e)
            Result.Error(e)
        }
    }
}
