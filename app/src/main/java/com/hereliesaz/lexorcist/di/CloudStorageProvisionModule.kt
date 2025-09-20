package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.CloudStorageProvider
// Keep imports for now if other parts use them, or if we revert
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

    // Removed to resolve conflict with GoogleDriveModule.provideGoogleDriveProvider
    // @Provides
    // @Singleton
    // @Named("googleDrive")
    // fun provideGoogleDriveCloudStorageProvider(): CloudStorageProvider {
    //     return GoogleDriveCloudStorageProvider()
    // }

    // Removed to resolve conflict with DropboxModule.provideDropboxProvider
    // @Provides
    // @Singleton
    // @Named("dropbox")
    // fun provideDropboxCloudStorageProvider(): CloudStorageProvider {
    //     return DropboxCloudStorageProvider()
    // }

    // Removed to resolve conflict with OneDriveModule.provideOneDriveProvider
    // @Provides
    // @Singleton
    // @Named("oneDrive")
    // fun provideOneDriveCloudStorageProvider(): CloudStorageProvider {
    //     return OneDriveCloudStorageProvider()
    // }
}
