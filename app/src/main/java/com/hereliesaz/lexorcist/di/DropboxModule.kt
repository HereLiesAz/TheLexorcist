package com.hereliesaz.lexorcist.di

import com.dropbox.core.DbxRequestConfig
import com.hereliesaz.lexorcist.auth.DropboxAuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun provideDropboxAuthManager(requestConfig: DbxRequestConfig): DropboxAuthManager {
        return DropboxAuthManager(requestConfig)
    }
}
