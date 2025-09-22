package com.hereliesaz.lexorcist.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.CredentialManager
import androidx.work.WorkManager
import com.google.gson.Gson
import com.hereliesaz.lexorcist.auth.CredentialHolder
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.di.qualifiers.ApplicationScope
import com.hereliesaz.lexorcist.service.GenerativeAIService
import com.hereliesaz.lexorcist.service.GoogleApiService
import com.hereliesaz.lexorcist.service.ScriptRunner
// Ensure LegalBertService is here if needed by ScriptRunner, otherwise remove import
// import com.hereliesaz.lexorcist.service.nlp.LegalBertService 
import com.hereliesaz.lexorcist.utils.CacheManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    @ApplicationScope // Ensure this qualifier is defined
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("LexorcistAppPrefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context
    ): SettingsManager = SettingsManager(context)

    @Provides
    @Singleton
    fun provideGenerativeAIService(): GenerativeAIService = GenerativeAIService()

    @Provides
    @Singleton
    fun provideScriptRunner(
        generativeAIService: GenerativeAIService,
        googleApiService: GoogleApiService // Using the imported com.hereliesaz.lexorcist.service.GoogleApiService
    ): ScriptRunner = ScriptRunner(generativeAIService, googleApiService)

    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context,
    ): CacheManager = CacheManager(context)

    @Provides
    @Singleton
    fun provideCredentialManager(
        @ApplicationContext context: Context,
    ): CredentialManager = CredentialManager.create(context)

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideGoogleApiService(
        credentialHolder: CredentialHolder, // com.hereliesaz.lexorcist.auth.CredentialHolder
        application: Application
    ): GoogleApiService { // com.hereliesaz.lexorcist.service.GoogleApiService
        return GoogleApiService(credentialHolder, application)
    }
}
