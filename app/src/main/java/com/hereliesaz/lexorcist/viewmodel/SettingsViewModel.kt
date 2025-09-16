package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    application: Application
) : AndroidViewModel(application) {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _caseFolderPath = MutableStateFlow<String?>(null)
    val caseFolderPath: StateFlow<String?> = _caseFolderPath.asStateFlow()

    private val _cloudSyncEnabled = MutableStateFlow(true)
    val cloudSyncEnabled: StateFlow<Boolean> = _cloudSyncEnabled.asStateFlow()

    private val _migrationStatus = MutableStateFlow<String?>(null)
    val migrationStatus: StateFlow<String?> = _migrationStatus.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val themeName = settingsManager.getTheme()
        _themeMode.value = ThemeMode.valueOf(themeName)
        _caseFolderPath.value = settingsManager.getCaseFolderPath()
        _cloudSyncEnabled.value = settingsManager.getCloudSyncEnabled()
    }

    fun setThemeMode(themeMode: ThemeMode) {
        settingsManager.saveTheme(themeMode.name)
        _themeMode.value = themeMode
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        settingsManager.saveCloudSyncEnabled(enabled)
        _cloudSyncEnabled.value = enabled
    }

    fun setCaseFolderPath(newPath: String) {
        // This feature is currently disabled in the UI.
        // The file migration logic has been removed to avoid confusion.
        // To re-enable, implement proper SAF URI handling and file migration.
        settingsManager.saveCaseFolderPath(newPath)
        _caseFolderPath.value = newPath
    }

    fun clearMigrationStatus() {
        _migrationStatus.value = null
    }
}
