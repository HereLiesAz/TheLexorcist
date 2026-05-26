package com.hereliesaz.lexorcist.di

import com.dropbox.core.DbxRequestConfig
import com.hereliesaz.lexorcist.auth.DropboxAuthManager
import com.hereliesaz.lexorcist.auth.TinkSecureStorage
import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.DropboxProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DropboxModule {

    @Provides
    @Singleton
    fun provideDbxRequestConfig(): DbxRequestConfig {
        return DbxRequestConfig.newBuilder("TheLexorcist").build()
    }

    @Provides
    @Singleton
    fun provideDropboxAuthManager(
        requestConfig: DbxRequestConfig,
        secureStorage: TinkSecureStorage
    ): DropboxAuthManager {
        return DropboxAuthManager(requestConfig, secureStorage)
    }

    @Provides
    @Singleton
    @Named("dropbox")
    fun provideDropboxProvider(dropboxAuthManager: DropboxAuthManager): CloudStorageProvider {
        return DropboxProvider(dropboxAuthManager)
    }
}
