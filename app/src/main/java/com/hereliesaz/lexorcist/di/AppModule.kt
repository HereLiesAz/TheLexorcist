package com.hereliesaz.lexorcist.di

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.CredentialManager
import androidx.work.WorkManager
import com.google.gson.Gson
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.data.objectbox.MyObjectBox // Added import for MyObjectBox
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.hereliesaz.lexorcist.utils.CacheManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.BoxStore // Added import for BoxStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
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

    // Provider for BoxStore
    @Provides
    @Singleton
    fun provideBoxStore(@ApplicationContext context: Context): BoxStore {
        return MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context,
        boxStore: BoxStore // Added BoxStore parameter
    ): SettingsManager = SettingsManager(context, boxStore) // Pass BoxStore to constructor

    @Provides
    @Singleton
    fun provideScriptRunner(): ScriptRunner = ScriptRunner()

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

}
