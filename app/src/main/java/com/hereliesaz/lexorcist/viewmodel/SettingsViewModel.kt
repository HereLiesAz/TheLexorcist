package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.auth.DropboxAuthManager
import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.CloudUser
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale // Added for language list
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    @Named("dropbox") private val dropboxProvider: CloudStorageProvider,
    @Named("oneDrive") private val oneDriveProvider: CloudStorageProvider,
    private val dropboxAuthManager: DropboxAuthManager,
    application: Application
) : AndroidViewModel(application) {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _caseFolderPath = MutableStateFlow<String?>(null)
    val caseFolderPath: StateFlow<String?> = _caseFolderPath.asStateFlow()

    private val _cloudSyncEnabled = MutableStateFlow(true)
    val cloudSyncEnabled: StateFlow<Boolean> = _cloudSyncEnabled.asStateFlow()

    private val _selectedCloudProvider = MutableStateFlow("GoogleDrive")
    val selectedCloudProvider: StateFlow<String> = _selectedCloudProvider.asStateFlow()

    private val _migrationStatus = MutableStateFlow<String?>(null)
    val migrationStatus: StateFlow<String?> = _migrationStatus.asStateFlow()

    private val _dropboxUploadStatus = MutableStateFlow<String?>(null)
    val dropboxUploadStatus: StateFlow<String?> = _dropboxUploadStatus.asStateFlow()

    private val _oneDriveUploadStatus = MutableStateFlow<String?>(null)
    val oneDriveUploadStatus: StateFlow<String?> = _oneDriveUploadStatus.asStateFlow()

    val isDropboxAuthenticated = dropboxAuthManager.isAuthenticated
    private val _dropboxUser = MutableStateFlow<CloudUser?>(null)
    val dropboxUser = _dropboxUser.asStateFlow()

    // App Language
    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    // Available Languages (Code -> Display Name)
    val availableLanguages: Map<String, String> = mapOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "ja" to "日本語",
        "ko" to "한국어",
        "pt" to "Português",
        "ru" to "Русский",
        "zh" to "中文"
        // Add more as needed
    )

    // Transcription Service
    private val _selectedTranscriptionService = MutableStateFlow("Vosk") // Default to Vosk
    val selectedTranscriptionService: StateFlow<String> = _selectedTranscriptionService.asStateFlow()

    val availableTranscriptionServices: List<String> = listOf("Vosk", "Whisper", "Google Cloud")


    init {
        loadSettings()
        observeDropboxAuthState()
    }

    private fun observeDropboxAuthState() {
        viewModelScope.launch {
            isDropboxAuthenticated.collectLatest { authenticated ->
                if (authenticated) {
                    fetchDropboxUser()
                } else {
                    _dropboxUser.value = null
                }
            }
        }
    }

    private fun fetchDropboxUser() {
        viewModelScope.launch {
            when (val result = dropboxProvider.getCurrentUser()) {
                is Result.Loading -> {
                    _dropboxUser.value = null 
                }
                is Result.Success -> _dropboxUser.value = result.data
                is Result.Error -> _dropboxUser.value = null 
                is Result.UserRecoverableError -> {
                    _dropboxUser.value = null 
                }
            }
        }
    }

    fun disconnectDropbox() {
        dropboxAuthManager.clearAccessToken()
    }

    private fun loadSettings() {
        val themeName = settingsManager.getTheme()
        _themeMode.value = ThemeMode.values().firstOrNull { it.name.equals(themeName, ignoreCase = true) } ?: ThemeMode.SYSTEM
        _caseFolderPath.value = settingsManager.getCaseFolderPath()
        _cloudSyncEnabled.value = settingsManager.getCloudSyncEnabled()
        _selectedCloudProvider.value = settingsManager.getSelectedCloudProvider()
        _language.value = settingsManager.getLanguage()
        _selectedTranscriptionService.value = settingsManager.getTranscriptionService() // Load transcription service
    }

    fun setSelectedCloudProvider(provider: String) {
        settingsManager.saveSelectedCloudProvider(provider)
        _selectedCloudProvider.value = provider
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
        settingsManager.saveCaseFolderPath(newPath)
        _caseFolderPath.value = newPath
    }

    fun setLanguage(languageCode: String) { // Parameter changed to languageCode for clarity
        settingsManager.saveLanguage(languageCode)
        _language.value = languageCode
    }

    // Method to set the transcription service
    fun selectTranscriptionService(service: String) {
        settingsManager.saveTranscriptionService(service)
        _selectedTranscriptionService.value = service
    }

    fun clearMigrationStatus() {
        _migrationStatus.value = null
    }

    fun testDropboxUpload() {
        viewModelScope.launch {
            val content = "Hello, Dropbox!".toByteArray()
            _dropboxUploadStatus.value = "Starting Dropbox upload..." 
            when (val result = dropboxProvider.writeFile("", "test.txt", "text/plain", content)) {
                is Result.Loading -> {
                    _dropboxUploadStatus.value = "Uploading to Dropbox..."
                }
                is Result.Success -> {
                    _dropboxUploadStatus.value = "Successfully uploaded file to Dropbox with ID: ${result.data.id}"
                }
                is Result.Error -> {
                    _dropboxUploadStatus.value = "Error uploading file to Dropbox: ${result.exception.message}"
                }
                is Result.UserRecoverableError -> {
                    _dropboxUploadStatus.value = "A recoverable error occurred with Dropbox: ${result.exception.message}"
                }
            }
        }
    }

    fun clearDropboxUploadStatus() {
        _dropboxUploadStatus.value = null
    }

    fun testOneDriveUpload() {
        viewModelScope.launch {
            val content = "Hello, OneDrive!".toByteArray()
            _oneDriveUploadStatus.value = "Starting OneDrive upload..." 
            when (val result = oneDriveProvider.writeFile("root", "test.txt", "text/plain", content)) {
                is Result.Loading -> {
                    _oneDriveUploadStatus.value = "Uploading to OneDrive..."
                }
                is Result.Success -> {
                    _oneDriveUploadStatus.value = "Successfully uploaded file to OneDrive with ID: ${result.data.id}"
                }
                is Result.Error -> {
                    _oneDriveUploadStatus.value = "Error uploading file to OneDrive: ${result.exception.message}"
                }
                is Result.UserRecoverableError -> {
                    _oneDriveUploadStatus.value = "A recoverable error occurred with OneDrive: ${result.exception.message}"
                }
            }
        }
    }

    fun clearOneDriveUploadStatus() {
        _oneDriveUploadStatus.value = null
    }
}
