package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.GoogleDriveCloudStorageProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoogleDriveModule {

    /**
     * Binds the real Google Drive implementation ([GoogleDriveCloudStorageProvider], which delegates
     * to [com.hereliesaz.lexorcist.service.GoogleApiService]) as the "googleDrive" cloud provider.
     * Hilt constructs the provider via its @Inject constructor.
     */
    @Provides
    @Singleton
    @Named("googleDrive")
    fun provideGoogleDriveProvider(
        provider: GoogleDriveCloudStorageProvider
    ): CloudStorageProvider = provider
}
