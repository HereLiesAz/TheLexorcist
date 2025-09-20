package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.DynamicTranscriptionService
import com.hereliesaz.lexorcist.service.TranscriptionService
import com.hereliesaz.lexorcist.service.VoskTranscriptionService
import com.hereliesaz.lexorcist.service.WhisperTranscriptionService
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
        settingsManager: SettingsManager,
        voskTranscriptionService: VoskTranscriptionService,
        whisperTranscriptionService: WhisperTranscriptionService
    ): TranscriptionService {
        // Provide the DynamicTranscriptionService which will delegate to the
        // appropriate service based on current settings.
        return DynamicTranscriptionService(settingsManager, voskTranscriptionService, whisperTranscriptionService)
    }
}
