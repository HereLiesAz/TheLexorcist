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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt module that provides application-level singletons.
 *
 * This module is installed in the [SingletonComponent], meaning all dependencies provided here
 * live for the entire lifecycle of the application.
 */
@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    /**
     * Provides a [DispatcherProvider] to abstract Coroutine Dispatchers.
     * This allows swapping dispatchers for unit tests (e.g., using TestDispatcher instead of IO).
     */
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = StandardDispatchers()

    /**
     * Provides a global [CoroutineScope] for application-wide operations.
     * This scope uses a [SupervisorJob], ensuring that a failure in one child coroutine
     * does not cancel the entire scope.
     */
    @Provides
    @Singleton
    @ApplicationScope // Qualifier to distinguish this scope from others.
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    /**
     * Provides the [WorkManager] instance for scheduling background tasks.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    /**
     * Provides a secure [SharedPreferences] instance.
     *
     * **SECURITY CRITICAL:**
     * Instead of standard SharedPreferences, we use [EncryptedSharedPreferences] to store
     * sensitive data (like auth tokens and PII) at rest.
     * The keys and values are encrypted using keys from the Android Keystore.
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences {
        // Create or retrieve the Master Key for encryption (AES256_GCM).
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        return EncryptedSharedPreferences.create(
            "secret_shared_prefs", // Distinct filename to avoid conflicts/crashes with unencrypted prefs.
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Provides the [SettingsManager] wrapper around SharedPreferences.
     */
    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context
    ): SettingsManager = SettingsManager(context)

    /**
     * Provides the [GenerativeAIService] for AI-based content generation.
     */
    @Provides
    @Singleton
    fun provideGenerativeAIService(): GenerativeAIService = GenerativeAIService()

    /**
     * Provides the [ScriptRunner] service for executing user-defined scripts.
     * Injects the AI service, Google API service, and Semantic service as dependencies available to the script environment.
     */
    @Provides
    @Singleton
    fun provideScriptRunner(
        generativeAIService: GenerativeAIService,
        googleApiService: GoogleApiService,
        semanticService: com.hereliesaz.lexorcist.service.SemanticService
    ): ScriptRunner = ScriptRunner(generativeAIService, googleApiService, semanticService)

    /**
     * Provides the [CacheManager] for handling temporary file storage and cleanup.
     */
    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context,
    ): CacheManager = CacheManager(context)

    /**
     * Provides the [CredentialManager] for Credential Provider API operations.
     */
    @Provides
    @Singleton
    fun provideCredentialManager(
        @ApplicationContext context: Context,
    ): CredentialManager = CredentialManager.create(context)

    /**
     * Provides a global [Gson] instance for JSON serialization/deserialization.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /**
     * Provides the [GoogleApiService] for interacting with Google Drive, Sheets, and Gmail.
     */
    @Provides
    @Singleton
    fun provideGoogleApiService(
        credentialHolder: CredentialHolder,
        application: Application
    ): GoogleApiService {
        return GoogleApiService(credentialHolder, application)
    }

    /**
     * Provides the FusedLocationProviderClient for location services.
     */
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context) =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Provides the [EvidenceImporter] for processing external content (images, audio).
     */
    @Provides
    @Singleton
    fun provideEvidenceImporter(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: com.google.android.gms.location.FusedLocationProviderClient
    ): EvidenceImporter {
        return EvidenceImporter(context.contentResolver, fusedLocationProviderClient)
    }

    /**
     * Provides the [ChatHistoryParser] for parsing chat logs.
     */
    @Provides
    @Singleton
    fun provideChatHistoryParser(@ApplicationContext context: Context): ChatHistoryParser {
        return ChatHistoryParser(context)
    }

    /**
     * Provides the [GmailService] for interacting with the Gmail API.
     */
    @Provides
    @Singleton
    fun provideGmailService(credentialHolder: com.hereliesaz.lexorcist.auth.CredentialHolder): GmailService {
        return GmailService(credentialHolder)
    }

    /**
     * Provides the [OutlookAuthManager] for handling Microsoft Outlook authentication.
     */
    @Provides
    @Singleton
    fun provideOutlookAuthManager(@ApplicationContext context: Context): OutlookAuthManager {
        return OutlookAuthManager(context)
    }

    /**
     * Provides the [ImapService] for generic email processing via IMAP.
     */
    @Provides
    @Singleton
    fun provideImapService(): ImapService {
        return ImapService()
    }

    /**
     * Provides the [LocationHistoryParser] for parsing Google Location History JSON files.
     */
    @Provides
    @Singleton
    fun provideLocationHistoryParser(): LocationHistoryParser {
        return LocationHistoryParser()
    }
}
