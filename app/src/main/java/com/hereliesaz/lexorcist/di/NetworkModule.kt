package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.GoogleApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGoogleApiService(): GoogleApiService {
        return GoogleApiService()
    }
}
