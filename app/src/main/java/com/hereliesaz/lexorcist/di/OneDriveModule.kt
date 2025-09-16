package com.hereliesaz.lexorcist.di

import android.content.Context
import com.hereliesaz.lexorcist.auth.OneDriveAuthManager
import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.OneDriveProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OneDriveModule {

    @Provides
    @Singleton
    fun provideOneDriveAuthManager(@ApplicationContext context: Context): OneDriveAuthManager {
        return OneDriveAuthManager(context)
    }

    @Provides
    @Singleton
    @Named("oneDrive")
    fun provideOneDriveProvider(oneDriveAuthManager: OneDriveAuthManager): CloudStorageProvider {
        return OneDriveProvider(oneDriveAuthManager)
    }
}
