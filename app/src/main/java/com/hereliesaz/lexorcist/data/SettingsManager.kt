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
    ) { // Changed here

        private val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.settings_preferences_name), Context.MODE_PRIVATE)

        private val keyUserScript = "user_script"

        /**
         * Saves the user-defined script.
         *
         * @param script The script to save.
         */
        fun saveScript(script: String) {
            sharedPreferences.edit().putString(keyUserScript, script).apply()
        }

        /**
         * Retrieves the saved user script.
         *
         * @return The saved script, or an empty string if no script is set.
         */
        fun getScript(): String = sharedPreferences.getString(keyUserScript, "") ?: ""

        /**
         * Saves the selected application theme.
         *
         * @param theme The theme to save (e.g., "Light", "Dark", "System").
         */
        fun saveTheme(theme: String) {
            sharedPreferences.edit().putString(context.getString(R.string.settings_key_theme), theme).apply()
        }

        /**
         * Retrieves the saved application theme.
         *
         * @return The saved theme, or "System" if no theme is set.
         */
        fun getTheme(): String {
            val defaultTheme = context.getString(R.string.settings_theme_system)
            return sharedPreferences.getString(context.getString(R.string.settings_key_theme), defaultTheme) ?: defaultTheme
        }

        /**
         * Saves the selected export format.
         *
         * @param format The export format to save (e.g., "PDF", "CSV").
         */
        fun saveExportFormat(format: String) {
            sharedPreferences.edit().putString(context.getString(R.string.settings_key_export_format), format).apply()
        }

        /**
         * Retrieves the saved export format.
         *
         * @return The saved export format, or "PDF" if no format is set.
         */
        fun getExportFormat(): String {
            val defaultFormat = context.getString(R.string.settings_export_format_pdf)
            return sharedPreferences.getString(context.getString(R.string.settings_key_export_format), defaultFormat) ?: defaultFormat
        }
    }
