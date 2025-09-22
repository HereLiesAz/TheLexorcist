package com.hereliesaz.lexorcist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.lexorcist.auth.DropboxAuthManager
import com.hereliesaz.lexorcist.data.CloudStorageProvider
import com.hereliesaz.lexorcist.data.ExtrasRepository // Added import
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.CloudUser
import com.hereliesaz.lexorcist.model.DownloadState
import com.hereliesaz.lexorcist.model.LanguageModel
import com.hereliesaz.lexorcist.model.TranscriptionModels
import com.hereliesaz.lexorcist.service.VoskTranscriptionService
import com.hereliesaz.lexorcist.service.WhisperTranscriptionService
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    @Named("dropbox") private val dropboxProvider: CloudStorageProvider,
    @Named("oneDrive") private val oneDriveProvider: CloudStorageProvider,
    private val dropboxAuthManager: DropboxAuthManager,
    private val voskTranscriptionService: VoskTranscriptionService,
    private val whisperTranscriptionService: WhisperTranscriptionService,
    private val extrasRepository: ExtrasRepository, // Injected ExtrasRepository
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

    // Available Languages (Code -> Display Name) - This is for the UI, not transcription
    val availableAppLanguages: Map<String, String> = mapOf(
        "en" to "English",
        "es" to "Español"
        // Add more UI languages as needed
    )

    // Transcription Service
    private val _selectedTranscriptionService = MutableStateFlow("Vosk")
    val selectedTranscriptionService: StateFlow<String> = _selectedTranscriptionService.asStateFlow()
    val availableTranscriptionServices: List<String> = listOf("Vosk", "Whisper")

    // Transcription Language Models
    private val _voskLanguageModels = MutableStateFlow<List<LanguageModel>>(emptyList())
    val voskLanguageModels: StateFlow<List<LanguageModel>> = _voskLanguageModels.asStateFlow()

    private val _whisperLanguageModels = MutableStateFlow<List<LanguageModel>>(emptyList())
    val whisperLanguageModels: StateFlow<List<LanguageModel>> = _whisperLanguageModels.asStateFlow()

    private val _selectedTranscriptionLanguageCode = MutableStateFlow("en-us") // Default to english
    val selectedTranscriptionLanguageCode: StateFlow<String> = _selectedTranscriptionLanguageCode.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    // Author details for sharing - MOVED BEFORE INIT
    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName.asStateFlow()

    private val _authorEmail = MutableStateFlow("")
    val authorEmail: StateFlow<String> = _authorEmail.asStateFlow()

    init {
        _voskLanguageModels.value = TranscriptionModels.voskModels
        _whisperLanguageModels.value = TranscriptionModels.whisperModels
        loadSettings()
        observeDropboxAuthState()
        checkAllModelsStatus()
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
        _selectedTranscriptionService.value = settingsManager.getTranscriptionService()
        _selectedTranscriptionLanguageCode.value = settingsManager.getTranscriptionLanguage()
        // Load author name and email from settingsManager
        _authorName.value = settingsManager.getAuthorName() ?: ""
        _authorEmail.value = settingsManager.getAuthorEmail() ?: ""
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

    fun setLanguage(languageCode: String) { // This is for app's UI language
        settingsManager.saveLanguage(languageCode)
        _language.value = languageCode
    }

    fun selectTranscriptionService(service: String) {
        settingsManager.saveTranscriptionService(service)
        _selectedTranscriptionService.value = service
        // When switching services, update selected language to a valid one for that service
        val currentLangCode = _selectedTranscriptionLanguageCode.value
        val models = if (service == "Vosk") voskLanguageModels.value else whisperLanguageModels.value
        if (models.none { it.code == currentLangCode }) {
            models.firstOrNull()?.let { selectTranscriptionLanguage(it.code) }
        }
    }

    fun selectTranscriptionLanguage(languageCode: String) {
        settingsManager.saveTranscriptionLanguage(languageCode)
        _selectedTranscriptionLanguageCode.value = languageCode
    }

    private fun checkAllModelsStatus() {
        viewModelScope.launch {
            (voskLanguageModels.value + whisperLanguageModels.value).forEach { model ->
                checkModelStatus(model)
            }
        }
    }

    private fun checkModelStatus(model: LanguageModel) {
        val modelFile = File(getApplication<Application>().filesDir, model.modelName)
        if (modelFile.exists()) {
            // This check works for both Vosk (directory) and Whisper (file).
            model.downloadState.value = DownloadState.Downloaded
        } else {
            model.downloadState.value = DownloadState.NotDownloaded
        }
    }

    fun downloadLanguage(model: LanguageModel) {
        viewModelScope.launch {
            model.downloadState.value = DownloadState.Downloading
            model.progress.value = 0f

            val resultFlow = if (selectedTranscriptionService.value == "Vosk") {
                voskTranscriptionService.downloadModel(model)
            } else {
                whisperTranscriptionService.downloadModel(model)
            }

            resultFlow.collect { state ->
                when (state) {
                    is DownloadState.Downloading -> {
                        // Progress is handled by collecting the progress flow separately
                    }
                    is DownloadState.Downloaded -> {
                        model.downloadState.value = DownloadState.Downloaded
                        checkModelStatus(model) // Verify
                    }
                    is DownloadState.Error -> {
                        model.downloadState.value = state
                    }
                    else -> {} // Ignore NotDownloaded state from flow
                }
            }
        }

        // Separately collect progress
        viewModelScope.launch {
            val progressFlow = if (selectedTranscriptionService.value == "Vosk") {
                voskTranscriptionService.getDownloadProgress(model.modelName)
            } else {
                whisperTranscriptionService.getDownloadProgress(model.modelName)
            }
            progressFlow.collect { progress ->
                model.progress.value = progress
            }
        }
    }

    fun deleteLanguage(model: LanguageModel) {
        viewModelScope.launch {
            val modelFile = File(getApplication<Application>().filesDir, model.modelName)
            if (modelFile.exists()) {
                modelFile.deleteRecursively()
            }
            checkModelStatus(model) // Update state to NotDownloaded
        }
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

    fun setAuthorName(name: String) {
        settingsManager.saveAuthorName(name)
        _authorName.value = name
    }

    fun setAuthorEmail(email: String) {
        settingsManager.saveAuthorEmail(email)
        _authorEmail.value = email
    }

    // Function to be called from UI/ExtrasViewModel to initiate sharing
    suspend fun shareAddon(
        name: String,
        description: String,
        content: String,
        type: String,
        authorName: String,
        authorEmail: String,
        court: String?
    ): Result<Unit> {
        // Persist authorName and authorEmail if they are being set/confirmed here
        // though typically they'd be set via dedicated UI in settings
        setAuthorName(authorName)
        setAuthorEmail(authorEmail)
        return extrasRepository.shareItem(name, description, content, type, authorName, authorEmail, court ?: "")
    }
}
