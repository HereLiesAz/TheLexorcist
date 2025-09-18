package com.hereliesaz.lexorcist.di

import android.content.SharedPreferences
import com.dropbox.core.DbxRequestConfig
import com.hereliesaz.lexorcist.auth.DropboxAuthManager
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
        sharedPreferences: SharedPreferences // Added SharedPreferences dependency
    ): DropboxAuthManager {
        return DropboxAuthManager(requestConfig, sharedPreferences) // Pass SharedPreferences to constructor
    }

    @Provides
    @Singleton
    @Named("dropbox")
    fun provideDropboxProvider(dropboxAuthManager: DropboxAuthManager): CloudStorageProvider {
        return DropboxProvider(dropboxAuthManager)
    }
}
