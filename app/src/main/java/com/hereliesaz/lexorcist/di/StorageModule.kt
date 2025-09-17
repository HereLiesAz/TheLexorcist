package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.CloudStorageService
import com.hereliesaz.lexorcist.data.LocalFileStorageService
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.data.StorageService
import com.hereliesaz.lexorcist.data.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideStorageService(
        localFileStorageService: LocalFileStorageService,
        syncManager: SyncManager,
        @Named("googleDrive") googleDriveProvider: CloudStorageProvider,
        @Named("dropbox") dropboxProvider: CloudStorageProvider,
        @Named("oneDrive") oneDriveProvider: CloudStorageProvider,
        settingsManager: SettingsManager
    ): StorageService {
        return CloudStorageService(
            localFileStorageService,
            syncManager,
            googleDriveProvider,
            dropboxProvider,
            oneDriveProvider,
            settingsManager
        )
    }
}
