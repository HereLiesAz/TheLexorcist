package com.hereliesaz.lexorcist.di

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.CredentialManager
import androidx.work.WorkManager
import com.google.gson.Gson
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.di.qualifiers.ApplicationScope // Import the qualifier
// import com.hereliesaz.lexorcist.data.objectbox.MyObjectBox // REMOVED
import com.hereliesaz.lexorcist.model.AllegationsSheet
import com.hereliesaz.lexorcist.model.CaseInfoSheet
import com.hereliesaz.lexorcist.model.EvidenceSheet
import com.hereliesaz.lexorcist.model.SpreadsheetSchema
// import com.hereliesaz.lexorcist.service.ScriptRunner // REMOVED for now
import com.hereliesaz.lexorcist.utils.CacheManager
// import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel // REMOVED for now
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
// import io.objectbox.BoxStore // REMOVED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    @ApplicationScope // Add the qualifier
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
    ): SettingsManager = SettingsManager(context) // Pass only Context to constructor

    // @Provides // REMOVED for now
    // @Singleton // REMOVED for now
    // fun provideScriptRunner(scriptedMenuViewModel: ScriptedMenuViewModel): ScriptRunner = ScriptRunner(scriptedMenuViewModel) // REMOVED for now

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

    // Removed provideSpreadsheetSchema method
}
