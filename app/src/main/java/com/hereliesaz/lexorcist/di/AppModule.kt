package com.hereliesaz.lexorcist.di

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.CredentialManager
import androidx.work.WorkManager
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.service.ScriptRunner
import com.hereliesaz.lexorcist.utils.CacheManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
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
        @ApplicationContext context: Context,
    ): SettingsManager = SettingsManager(context)

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
    fun provideGson(): com.google.gson.Gson = com.google.gson.Gson()
}
