package com.hereliesaz.lexorcist.data

import android.content.Context
import android.content.SharedPreferences
import com.hereliesaz.lexorcist.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages application settings using [SharedPreferences].
 *
 * This class provides a simple way to save and retrieve user preferences, such as the
 * application theme and export format.
 *
 * @param context The application context, used to access [SharedPreferences].
 */
@Singleton
class SettingsManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {

        private val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.settings_preferences_name), Context.MODE_PRIVATE)

        private val keyUserScript = "user_script"
        private val keyStorageLocation = "storage_location"
        private val keyCloudProvider = "cloud_provider"
        private val keyTranscriptionService = "transcription_service"
        private val keyTranscriptionLanguage = "transcription_language"
        private val keyAuthorName = "author_name" // Added key
        private val keyAuthorEmail = "author_email" // Added key

        fun saveSelectedCloudProvider(provider: String) {
            sharedPreferences.edit().putString(keyCloudProvider, provider).apply()
        }

        fun getSelectedCloudProvider(): String {
            return sharedPreferences.getString(keyCloudProvider, "GoogleDrive") ?: "GoogleDrive"
        }

        fun saveStorageLocation(uri: String) {
            sharedPreferences.edit().putString(keyStorageLocation, uri).apply()
        }

        fun getStorageLocation(): String? = sharedPreferences.getString(keyStorageLocation, null)

        fun saveScript(script: String) {
            sharedPreferences.edit().putString(keyUserScript, script).apply()
        }

        fun getScript(): String = sharedPreferences.getString(keyUserScript, "") ?: ""

        fun saveTheme(theme: String) {
            sharedPreferences.edit().putString(context.getString(R.string.settings_key_theme), theme).apply()
        }

        fun getTheme(): String {
            val defaultTheme = context.getString(R.string.settings_theme_system)
            return sharedPreferences.getString(context.getString(R.string.settings_key_theme), defaultTheme) ?: defaultTheme
        }

        fun saveExportFormat(format: String) {
            sharedPreferences.edit().putString(context.getString(R.string.settings_key_export_format), format).apply()
        }

        fun getExportFormat(): String {
            val defaultFormat = context.getString(R.string.settings_export_format_pdf)
            return sharedPreferences.getString(context.getString(R.string.settings_key_export_format), defaultFormat) ?: defaultFormat
        }

        fun saveCaseFolderPath(path: String) {
            sharedPreferences.edit().putString("case_folder_path", path).apply()
        }

        fun getCaseFolderPath(): String? {
            return sharedPreferences.getString("case_folder_path", null)
        }

        fun saveCloudSyncEnabled(enabled: Boolean) {
            sharedPreferences.edit().putBoolean("cloud_sync_enabled", enabled).apply()
        }

        fun getCloudSyncEnabled(): Boolean {
            return sharedPreferences.getBoolean("cloud_sync_enabled", true)
        }

        fun saveLanguage(language: String) {
            sharedPreferences.edit().putString("language", language).apply()
        }

        fun getLanguage(): String {
            return sharedPreferences.getString("language", "en") ?: "en"
        }

        // Added methods for transcription service
        fun saveTranscriptionService(service: String) {
            sharedPreferences.edit().putString(keyTranscriptionService, service).apply()
        }

        fun getTranscriptionService(): String {
            return sharedPreferences.getString(keyTranscriptionService, "Vosk") ?: "Vosk" // Default to Vosk
        }

        fun saveTranscriptionLanguage(languageCode: String) {
            sharedPreferences.edit().putString(keyTranscriptionLanguage, languageCode).apply()
        }

        fun getTranscriptionLanguage(): String {
            return sharedPreferences.getString(keyTranscriptionLanguage, "en-us") ?: "en-us" // Default to vosk english
        }

        // Methods for author details
        fun saveAuthorName(name: String) {
            sharedPreferences.edit().putString(keyAuthorName, name).apply()
        }

        fun getAuthorName(): String {
            return sharedPreferences.getString(keyAuthorName, "") ?: ""
        }

        fun saveAuthorEmail(email: String) {
            sharedPreferences.edit().putString(keyAuthorEmail, email).apply()
        }

        fun getAuthorEmail(): String {
            return sharedPreferences.getString(keyAuthorEmail, "") ?: ""
        }
    }
