package com.hereliesaz.lexorcist.data

import com.hereliesaz.lexorcist.auth.OneDriveAuthManager
import com.hereliesaz.lexorcist.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class OneDriveProvider @Inject constructor(
    private val oneDriveAuthManager: OneDriveAuthManager
) : CloudStorageProvider {

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
        val accessToken = oneDriveAuthManager.getAccessToken()
        if (accessToken == null) {
            return@withContext Result.Error(Exception("OneDrive client not initialized. Please connect to OneDrive first."))
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://graph.microsoft.com/v1.0/me/drive/root:/$fileName:/content")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", mimeType)
            connection.doOutput = true
            connection.outputStream.use { os ->
                os.write(content)
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(response.toString())
                val fileId = jsonResponse.getString("id")
                val name = jsonResponse.getString("name")
                // The lastModifiedDateTime is not available in the response of the content upload.
                // We will use the current time as a fallback.
                val lastModified = System.currentTimeMillis()
                Result.Success(CloudFile(fileId, name, lastModified))
            } else {
                Result.Error(Exception("OneDrive upload failed with response code: $responseCode"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        } finally {
            connection?.disconnect()
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
