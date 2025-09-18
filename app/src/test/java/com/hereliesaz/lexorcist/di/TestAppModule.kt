package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.CaseRepository
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.ScriptRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.mockito.Answers
import org.mockito.kotlin.mock // Import Mockito-Kotlin's mock function
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestAppModule {
    @Provides
    @Singleton
    fun provideEvidenceRepository(): EvidenceRepository = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)

    @Provides
    @Singleton
    fun provideSettingsManager(): SettingsManager = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)

    @Provides
    @Singleton
    fun provideScriptRunner(): ScriptRunner = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)

    @Provides
    @Singleton
    fun provideCaseRepository(): CaseRepository = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
}
