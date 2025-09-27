package com.hereliesaz.lexorcist.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
// import androidx.compose.material3.AlertDialog // Not used directly, AzAlertDialog is used
// import androidx.compose.material3.Button // Not used directly
// import androidx.compose.material3.CircularProgressIndicator // Not used directly
// import androidx.compose.material3.DropdownMenuItem // Not used directly
import androidx.compose.material3.ExperimentalMaterial3Api
// import androidx.compose.material3.ExposedDropdownMenuAnchorType // Not used directly
// import androidx.compose.material3.ExposedDropdownMenuBox // Not used directly
// import androidx.compose.material3.ExposedDropdownMenuDefaults // Not used directly
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.RadioButton // Not used directly
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
// import androidx.compose.material3.TextField // Not used directly
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dropbox.core.android.Auth
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.DownloadState
import com.hereliesaz.lexorcist.model.LanguageModel
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.lexorcist.ui.components.AzAlertDialog
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.model.OutlookSignInState
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.OneDriveViewModel
import com.hereliesaz.lexorcist.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    caseViewModel: CaseViewModel,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    oneDriveViewModel: OneDriveViewModel = hiltViewModel()
) {
    val themeMode by settingsViewModel.themeMode.collectAsState() // Used to determine current state for AzCycler logic if needed, though AzCycler manages its own display state.
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? android.app.Activity // Safe cast

    val signInState by authViewModel.signInState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Theme Settings
            Text(
                text = stringResource(R.string.theme_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AzCycler(
                options = ThemeMode.entries.map { it.name.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } },
                selectedOption = themeMode.name.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                onCycle = {
                    val nextIndex = (themeMode.ordinal + 1) % ThemeMode.entries.size
                    settingsViewModel.setThemeMode(ThemeMode.entries[nextIndex])
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Language Settings (App Language)
            Text(
                text = stringResource(R.string.language_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            val currentAppLanguage by settingsViewModel.language.collectAsState()
            val availableAppLanguages = settingsViewModel.availableAppLanguages
            AzCycler(
                options = availableAppLanguages.values.toList(),
                selectedOption = availableAppLanguages[currentAppLanguage] ?: "",
                onCycle = {
                    val currentIndex = availableAppLanguages.keys.indexOf(currentAppLanguage)
                    val nextIndex = (currentIndex + 1) % availableAppLanguages.size
                    settingsViewModel.setLanguage(availableAppLanguages.keys.elementAt(nextIndex))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Transcription Service Settings
            Text(
                text = stringResource(R.string.transcription_service),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            val selectedTranscriptionService by settingsViewModel.selectedTranscriptionService.collectAsState()
            val availableTranscriptionServices = settingsViewModel.availableTranscriptionServices
            AzCycler(
                options = availableTranscriptionServices,
                selectedOption = selectedTranscriptionService,
                onCycle = {
                    val currentIndex = availableTranscriptionServices.indexOf(selectedTranscriptionService)
                    val nextIndex = (currentIndex + 1) % availableTranscriptionServices.size
                    settingsViewModel.selectTranscriptionService(availableTranscriptionServices[nextIndex])
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Transcription Language Settings
            val voskModels by settingsViewModel.voskLanguageModels.collectAsState()
            val whisperModels by settingsViewModel.whisperLanguageModels.collectAsState()
            val selectedLanguageCode by settingsViewModel.selectedTranscriptionLanguageCode.collectAsState()

            val models = if (selectedTranscriptionService == "Vosk") voskModels else whisperModels

            LanguageModelDownloader(
                models = models,
                selectedLanguageCode = selectedLanguageCode, // This prop might be unused by downloader's AzCycler now
                onLanguageSelected = { settingsViewModel.selectTranscriptionLanguage(it) },
                onDownload = { settingsViewModel.downloadLanguage(it) },
                onDelete = { settingsViewModel.deleteLanguage(it) }
            )


            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Cache Settings
            Text(
                text = stringResource(R.string.cache_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            AzButton(
                onClick = { showClearCacheDialog = true },
                text = stringResource(R.string.clear_cache).uppercase(Locale.getDefault())
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Storage Settings
            Text(
                text = stringResource(R.string.storage_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            val storageLocation by caseViewModel.storageLocation.collectAsState()
            val directoryPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
                onResult = { uri ->
                    uri?.let {
                        caseViewModel.setStorageLocation(it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.current_location_colon_placeholder, storageLocation ?: stringResource(R.string.default_text)))
            Spacer(modifier = Modifier.height(8.dp))
            AzButton(
                onClick = { directoryPickerLauncher.launch(null) },
                text = stringResource(R.string.change_storage_location).uppercase(Locale.getDefault())
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Cloud Service Settings
            Text(
                text = stringResource(R.string.cloud_service_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            val selectedCloudProvider by settingsViewModel.selectedCloudProvider.collectAsState()
            val cloudProviders = listOf("GoogleDrive", "Dropbox", "OneDrive", "None")
            AzCycler(
                options = cloudProviders,
                selectedOption = selectedCloudProvider,
                onCycle = {
                    val currentIndex = cloudProviders.indexOf(selectedCloudProvider)
                    val nextIndex = (currentIndex + 1) % cloudProviders.size
                    settingsViewModel.setSelectedCloudProvider(cloudProviders[nextIndex])
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In Status
            Text(
                text = stringResource(R.string.google_account),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (val currentSignInState = signInState) {
                is SignInState.Success -> {
                    val userInfo = currentSignInState.userInfo
                    Text(stringResource(R.string.signed_in_as_placeholder, userInfo?.email ?: stringResource(R.string.unknown_email)))
                    Spacer(modifier = Modifier.height(8.dp))
                    AzButton(
                        onClick = {
                            authViewModel.signOut(mainViewModel)
                            if (activity != null) {
                               authViewModel.signIn(activity, mainViewModel)
                            }
                        },
                        text = stringResource(R.string.switch_account).uppercase(Locale.getDefault())
                    )
                     Spacer(modifier = Modifier.height(8.dp))
                    AzButton(
                        onClick = {
                            authViewModel.signOut(mainViewModel)
                        },
                        text = stringResource(R.string.sign_out).uppercase(Locale.getDefault())
                    )
                }
                else -> {
                    Text(stringResource(R.string.not_signed_in))
                    Spacer(modifier = Modifier.height(8.dp))
                    AzButton(
                        onClick = {
                             if (activity != null) {
                               authViewModel.signIn(activity, mainViewModel)
                            }
                        },
                        text = stringResource(R.string.sign_in).uppercase(Locale.getDefault())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Dropbox
            Text(
                text = stringResource(R.string.dropbox),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            val isDropboxAuthenticated by settingsViewModel.isDropboxAuthenticated.collectAsState()
            val dropboxUser by settingsViewModel.dropboxUser.collectAsState()

            if (isDropboxAuthenticated) {
                Text(stringResource(R.string.connected_as_placeholder, dropboxUser?.email ?: "..."))
                Spacer(modifier = Modifier.height(8.dp))
                AzButton(
                    onClick = {
                        settingsViewModel.disconnectDropbox()
                    },
                    text = stringResource(R.string.disconnect_from_dropbox).uppercase(Locale.getDefault())
                )
            } else {
                AzButton(
                    onClick = {
                        Auth.startOAuth2Authentication(context, context.getString(R.string.dropbox_app_key))
                    },
                    text = stringResource(R.string.connect_to_dropbox).uppercase(Locale.getDefault())
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // OneDrive
            Text(
                text = stringResource(R.string.onedrive),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            val oneDriveSignInState by oneDriveViewModel.oneDriveSignInState.collectAsState()

            when (val stateVal = oneDriveSignInState) {
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Idle -> {
                    AzButton(
                        onClick = {
                            if (activity != null) {
                                oneDriveViewModel.connectToOneDrive(activity)
                            }
                        },
                        text = stringResource(R.string.connect_to_onedrive).uppercase(Locale.getDefault())
                    )
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.InProgress -> {
                    com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator()
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Success -> {
                    Text(stringResource(R.string.connected_to_onedrive_as_placeholder, stateVal.accountName ?: stringResource(R.string.unknown_account)))
                    Spacer(modifier = Modifier.height(8.dp))
                    AzButton(
                        onClick = { oneDriveViewModel.disconnectFromOneDrive() },
                        text = stringResource(R.string.disconnect_from_onedrive).uppercase(Locale.getDefault())
                    )
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Error -> {
                    Text(stringResource(R.string.error_connecting_to_onedrive_placeholder, stateVal.message ?: stringResource(R.string.unknown_error)))
                    Spacer(modifier = Modifier.height(8.dp))
                    AzButton(
                        onClick = {
                            if (activity != null) {
                                oneDriveViewModel.connectToOneDrive(activity)
                            }
                        },
                        text = stringResource(R.string.retry).uppercase(Locale.getDefault())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Outlook
            Text(
                text = "Outlook",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            val outlookSignInState by authViewModel.outlookSignInState.collectAsState()

            when (val stateVal = outlookSignInState) {
                is OutlookSignInState.Idle -> {
                    AzButton(
                        onClick = {
                            if (activity != null) {
                                authViewModel.signInWithOutlook(activity)
                            }
                        },
                        text = "Connect to Outlook".uppercase(Locale.getDefault())
                    )
                }
                is OutlookSignInState.InProgress -> {
                    com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator()
                }
                is OutlookSignInState.Success -> {
                    Text("Connected as: ${stateVal.accountName ?: "Unknown"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    AzButton(
                        onClick = { authViewModel.signOutFromOutlook() },
                        text = "Disconnect from Outlook".uppercase(Locale.getDefault())
                    )
                }
                is OutlookSignInState.Error -> {
                    Text("Error: ${stateVal.message ?: "Unknown error"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    AzButton(
                        onClick = {
                            if (activity != null) {
                                authViewModel.signInWithOutlook(activity)
                            }
                        },
                        text = stringResource(R.string.retry).uppercase(Locale.getDefault())
                    )
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AzAlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache_title)) },
            text = { Text(stringResource(R.string.clear_cache_confirmation)) },
            confirmButton = {
                AzButton(
                    onClick = {
                        caseViewModel.clearCache()
                        showClearCacheDialog = false
                    },
                    text = stringResource(R.string.delete).uppercase(Locale.getDefault())
                )
            },
            dismissButton = {
                AzButton(
                    onClick = { showClearCacheDialog = false },
                    text = stringResource(R.string.cancel).uppercase(Locale.getDefault())
                )
            }
        )
    }
}

@Composable
fun LanguageModelDownloader(
    models: List<LanguageModel>,
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDownload: (LanguageModel) -> Unit,
    onDelete: (LanguageModel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AzCycler(
                options = models.map { it.name },
                selectedOption = models.find { it.code == selectedLanguageCode }?.name ?: "",
                onCycle = {
                    val currentIndex = models.indexOfFirst { it.code == selectedLanguageCode }
                    val nextIndex = (currentIndex + 1) % models.size
                    onLanguageSelected(models[nextIndex].code)
                }
            )

            val currentSelectedModelForStatus = models.firstOrNull { it.code == selectedLanguageCode }
            if (currentSelectedModelForStatus != null) {
                Spacer(modifier = Modifier.width(8.dp))
                val downloadState by currentSelectedModelForStatus.downloadState.collectAsState()
                ModelStatusIcon(
                    downloadState = downloadState,
                    onDownload = { onDownload(currentSelectedModelForStatus) },
                    onDelete = { onDelete(currentSelectedModelForStatus) }
                )
            }
        }

        val downloadingModel = models.firstOrNull { it.downloadState.collectAsState().value is DownloadState.Downloading }
        if (downloadingModel != null) {
            val progress by downloadingModel.progress.collectAsState()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Downloading: ${"%.0f".format(progress * 100)}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator(modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ModelStatusIcon(
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    when (downloadState) {
        is DownloadState.NotDownloaded -> {
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = stringResource(id = R.string.download_model_description))
            }
        }
        is DownloadState.Downloading -> {
            com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator(modifier = Modifier.size(24.dp).padding(8.dp))
        }
        is DownloadState.Downloaded -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(id = R.string.model_downloaded_description),
                    tint = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_model_description))
                }
            }
        }
        is DownloadState.Error -> {
            IconButton(onClick = onDownload) { // Retry
                Icon(Icons.Default.Download, contentDescription = stringResource(id = R.string.retry_download_description))
            }
        }
    }
}
