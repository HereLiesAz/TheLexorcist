package com.hereliesaz.lexorcist.di

import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.ScriptRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class],
)
object TestAppModule {
    @Provides
    @Singleton
    fun provideEvidenceRepository(): EvidenceRepository = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideSettingsManager(): SettingsManager = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideScriptRunner(): ScriptRunner = mockk(relaxed = true)
}
