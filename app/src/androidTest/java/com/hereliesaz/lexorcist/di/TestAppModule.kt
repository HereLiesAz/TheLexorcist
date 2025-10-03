package com.hereliesaz.lexorcist.di

import android.content.Context
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.hereliesaz.lexorcist.data.EvidenceRepository
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.hereliesaz.lexorcist.utils.CacheManager
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import com.hereliesaz.lexorcist.service.OcrProcessingService
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
    fun provideOcrProcessingService(): OcrProcessingService = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideEvidenceRepository(): EvidenceRepository = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideSettingsManager(): SettingsManager = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideScriptRunner(): ScriptRunner = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideOneTapClient(
        @ApplicationContext context: Context,
    ): SignInClient = Identity.getSignInClient(context)

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): android.content.SharedPreferences = context.getSharedPreferences("LexorcistAppPrefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context,
    ): CacheManager = CacheManager(context)
}
