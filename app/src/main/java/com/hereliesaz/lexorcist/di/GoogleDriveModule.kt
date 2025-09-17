package com.hereliesaz.lexorcist.di

import android.content.Context
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.data.CloudFile
import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.GoogleDriveProvider
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.utils.Result
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoogleDriveModule {

    @Provides
    @Singleton
    fun provideGoogleApiService(credentialHolder: CredentialHolder): GoogleApiService? {
        return credentialHolder.googleApiService
    }

    @Provides
    @Singleton
    @Named("googleDrive")
    fun provideGoogleDriveProvider(
        googleApiService: GoogleApiService?,
        @ApplicationContext context: Context
    ): CloudStorageProvider {
        if (googleApiService == null) {
            // Return a provider that always fails
            return object : CloudStorageProvider {
                override suspend fun getRootFolderId(): Result<String> = Result.Error(Exception("Google account not signed in"))
                override suspend fun listFiles(folderId: String): Result<List<CloudFile>> = Result.Error(Exception("Google account not signed in"))
                override suspend fun readFile(fileId: String): Result<ByteArray> = Result.Error(Exception("Google account not signed in"))
                override suspend fun writeFile(folderId: String, fileName: String, mimeType: String, content: ByteArray): Result<CloudFile> = Result.Error(Exception("Google account not signed in"))
                override suspend fun updateFile(fileId: String, mimeType: String, content: ByteArray): Result<CloudFile> = Result.Error(Exception("Google account not signed in"))
                override suspend fun getFileMetadata(fileId: String): Result<CloudFile> = Result.Error(Exception("Google account not signed in"))
                override suspend fun createFolder(folderName: String, parentFolderId: String): Result<String> = Result.Error(Exception("Google account not signed in"))
            }
        }
        return GoogleDriveProvider(googleApiService, context)
    }
}
