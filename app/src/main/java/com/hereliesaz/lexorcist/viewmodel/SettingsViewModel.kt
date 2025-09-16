package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.data.DropboxProvider
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val dropboxProvider: DropboxProvider,
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

    private val _dropboxUploadStatus = MutableStateFlow<String?>(null)
    val dropboxUploadStatus: StateFlow<String?> = _dropboxUploadStatus.asStateFlow()

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

    fun testDropboxUpload() {
        viewModelScope.launch {
            val content = "Hello, Dropbox!".toByteArray()
            when (val result = dropboxProvider.writeFile("", "test.txt", "text/plain", content)) {
                is Result.Success -> {
                    _dropboxUploadStatus.value = "Successfully uploaded file to Dropbox with ID: ${result.data.id}"
                }
                is Result.Error -> {
                    _dropboxUploadStatus.value = "Error uploading file to Dropbox: ${result.exception.message}"
                }
                is Result.UserRecoverableError -> { // Added this branch
                    _dropboxUploadStatus.value = "Dropbox user recoverable error: ${result.exception.message}"
                    // Optionally, you might want to expose result.exception.intent to the UI here
                }
                // Add else branch if there are other Result subtypes not covered, or if it's a sealed interface with more implementations
                // else -> {
                //     _dropboxUploadStatus.value = "Unknown result from Dropbox upload"
                // }
            }
        }
    }

    fun clearDropboxUploadStatus() {
        _dropboxUploadStatus.value = null
    }
}
