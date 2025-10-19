package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.service.DynamicTranscriptionService
import com.hereliesaz.lexorcist.service.TranscriptionService
import com.hereliesaz.lexorcist.service.VoskTranscriptionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TranscriptionModule {

    @Provides
    @Singleton
    fun provideTranscriptionService(
        voskTranscriptionService: VoskTranscriptionService
    ): TranscriptionService {
        // Provide the DynamicTranscriptionService which will delegate to the
        // appropriate service based on current settings.
        return DynamicTranscriptionService(voskTranscriptionService)
    }
}
