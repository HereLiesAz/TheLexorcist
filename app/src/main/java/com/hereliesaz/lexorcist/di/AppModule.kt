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
import com.google.android.gms.location.LocationServices
import com.hereliesaz.lexorcist.utils.CacheManager
import com.hereliesaz.lexorcist.auth.OutlookAuthManager
import com.hereliesaz.lexorcist.service.GmailService
import com.hereliesaz.lexorcist.service.ImapService
import com.hereliesaz.lexorcist.utils.ChatHistoryParser
import com.hereliesaz.lexorcist.utils.EvidenceImporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.hereliesaz.lexorcist.utils.DispatcherProvider
import com.hereliesaz.lexorcist.utils.LocationHistoryParser
import com.hereliesaz.lexorcist.utils.StandardDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = StandardDispatchers()
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

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context) =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideEvidenceImporter(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: com.google.android.gms.location.FusedLocationProviderClient
    ): EvidenceImporter {
        return EvidenceImporter(context.contentResolver, fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideChatHistoryParser(@ApplicationContext context: Context): ChatHistoryParser {
        return ChatHistoryParser(context)
    }

    @Provides
    @Singleton
    fun provideGmailService(credentialHolder: com.hereliesaz.lexorcist.auth.CredentialHolder): GmailService {
        return GmailService(credentialHolder)
    }

    @Provides
    @Singleton
    fun provideOutlookAuthManager(@ApplicationContext context: Context): OutlookAuthManager {
        return OutlookAuthManager(context)
    }

    @Provides
    @Singleton
    fun provideImapService(): ImapService {
        return ImapService()
    }

    @Provides
    @Singleton
    fun provideLocationHistoryParser(): LocationHistoryParser {
        return LocationHistoryParser()
    }
}
