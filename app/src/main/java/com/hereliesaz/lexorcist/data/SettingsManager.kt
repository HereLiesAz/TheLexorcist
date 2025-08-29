package com.hereliesaz.lexorcist.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages application settings using [SharedPreferences].
 *
 * This class provides a simple way to save and retrieve user preferences, such as the
 * application theme and export format.
 *
 * @param context The application context, used to access [SharedPreferences].
 */
class SettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("lexorcist_settings", Context.MODE_PRIVATE)

    companion object {
        /** The key for storing the application theme preference. */
        const val KEY_THEME = "theme"
        /** The key for storing the export format preference. */
        const val KEY_EXPORT_FORMAT = "export_format"
    }

    /**
     * Saves the selected application theme.
     *
     * @param theme The theme to save (e.g., "Light", "Dark", "System").
     */
    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply()
    }

    /**
     * Retrieves the saved application theme.
     *
     * @return The saved theme, or "System" if no theme is set.
     */
    fun getTheme(): String {
        return sharedPreferences.getString(KEY_THEME, "System") ?: "System"
    }

    /**
     * Saves the selected export format.
     *
     * @param format The export format to save (e.g., "PDF", "CSV").
     */
    fun saveExportFormat(format: String) {
        sharedPreferences.edit().putString(KEY_EXPORT_FORMAT, format).apply()
    }

    /**
     * Retrieves the saved export format.
     *
     * @return The saved export format, or "PDF" if no format is set.
     */
    fun getExportFormat(): String {
        return sharedPreferences.getString(KEY_EXPORT_FORMAT, "PDF") ?: "PDF"
    }
}
