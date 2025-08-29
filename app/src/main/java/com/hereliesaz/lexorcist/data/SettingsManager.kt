package com.hereliesaz.lexorcist.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("lexorcist_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME = "theme"
        const val KEY_EXPORT_FORMAT = "export_format"
    }

    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(): String {
        return sharedPreferences.getString(KEY_THEME, "System") ?: "System"
    }

    fun saveExportFormat(format: String) {
        sharedPreferences.edit().putString(KEY_EXPORT_FORMAT, format).apply()
    }

    fun getExportFormat(): String {
        return sharedPreferences.getString(KEY_EXPORT_FORMAT, "PDF") ?: "PDF"
    }
}
