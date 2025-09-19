package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.service.LogService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogServiceModule {

    @Provides
    @Singleton
    fun provideLogService(): LogService {
        return LogService()
    }
}
