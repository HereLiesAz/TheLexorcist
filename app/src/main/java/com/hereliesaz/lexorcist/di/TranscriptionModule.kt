package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.service.TranscriptionService
import com.hereliesaz.lexorcist.service.VoskTranscriptionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranscriptionModule {

    @Binds
    @Singleton
    abstract fun bindTranscriptionService(
        voskTranscriptionService: VoskTranscriptionService
    ): TranscriptionService
}
