package com.hereliesaz.lexorcist.di

// import com.hereliesaz.lexorcist.service.GoogleCloudTranscriptionService // REMOVED or commented out
import com.hereliesaz.lexorcist.service.TranscriptionService
import com.hereliesaz.lexorcist.service.VoskTranscriptionService // UNCOMMENTED
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
        voskTranscriptionService: VoskTranscriptionService // CHANGED
    ): TranscriptionService
}
