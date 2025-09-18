package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.DropboxCloudStorageProvider
import com.hereliesaz.lexorcist.data.GoogleDriveCloudStorageProvider
import com.hereliesaz.lexorcist.data.OneDriveCloudStorageProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudStorageProvisionModule {

    // Removed provideGoogleDriveCloudStorageProvider
    // Removed provideDropboxCloudStorageProvider
    // Removed provideOneDriveCloudStorageProvider
}
