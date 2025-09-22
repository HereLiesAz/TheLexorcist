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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button // Keep this if used for other buttons like test upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem // Keep one
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox // Keep one
import androidx.compose.material3.ExposedDropdownMenuDefaults // Keep one Material 3 version
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton // For Theme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField // Used by ExposedDropdownMenuBox
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Corrected import
import com.dropbox.core.android.Auth
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.DownloadState
import com.hereliesaz.lexorcist.model.LanguageModel
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
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
    val themeMode by settingsViewModel.themeMode.collectAsState()
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
            Column(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { settingsViewModel.setThemeMode(mode) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(text = mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        RadioButton(
                            selected = (themeMode == mode),
                            onClick = { settingsViewModel.setThemeMode(mode) },
                        )
                    }
                }
            }

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
            var appLanguageExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = appLanguageExpanded,
                onExpandedChange = { appLanguageExpanded = !appLanguageExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = availableAppLanguages[currentAppLanguage] ?: currentAppLanguage,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = appLanguageExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true) // Corrected menuAnchor
                )
                ExposedDropdownMenu(
                    expanded = appLanguageExpanded,
                    onDismissRequest = { appLanguageExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableAppLanguages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                settingsViewModel.setLanguage(code)
                                appLanguageExpanded = false
                            }
                        )
                    }
                }
            }

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
            var transcriptionServiceExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = transcriptionServiceExpanded,
                onExpandedChange = { transcriptionServiceExpanded = !transcriptionServiceExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = selectedTranscriptionService,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.transcription_service)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transcriptionServiceExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true) // Corrected menuAnchor
                )
                ExposedDropdownMenu(
                    expanded = transcriptionServiceExpanded,
                    onDismissRequest = { transcriptionServiceExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableTranscriptionServices.forEach { service ->
                        DropdownMenuItem(
                            text = { Text(service) },
                            onClick = {
                                settingsViewModel.selectTranscriptionService(service)
                                transcriptionServiceExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transcription Language Settings
            val voskModels by settingsViewModel.voskLanguageModels.collectAsState()
            val whisperModels by settingsViewModel.whisperLanguageModels.collectAsState()
            val selectedLanguageCode by settingsViewModel.selectedTranscriptionLanguageCode.collectAsState()

            val models = if (selectedTranscriptionService == "Vosk") voskModels else whisperModels

            LanguageModelDownloader(
                models = models,
                selectedLanguageCode = selectedLanguageCode,
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
            LexorcistOutlinedButton(
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
            LexorcistOutlinedButton(
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

            Column(Modifier.fillMaxWidth()) {
                cloudProviders.forEach { provider ->
                    val isEnabled = when (provider) {
                        "GoogleDrive" -> signInState is SignInState.Success || selectedCloudProvider == provider
                        else -> true
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { if(isEnabled) settingsViewModel.setSelectedCloudProvider(provider) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(text = provider)
                        RadioButton(
                            selected = (selectedCloudProvider == provider),
                            onClick = { settingsViewModel.setSelectedCloudProvider(provider) },
                            enabled = isEnabled
                        )
                    }
                }
            }

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
                    LexorcistOutlinedButton(onClick = {
                        authViewModel.signOut(mainViewModel)
                        if (activity != null) {
                           authViewModel.signIn(activity, mainViewModel)
                        }
                    }, text = stringResource(R.string.switch_account).uppercase(Locale.getDefault()))
                     Spacer(modifier = Modifier.height(8.dp))
                    LexorcistOutlinedButton(onClick = {
                        authViewModel.signOut(mainViewModel)
                    }, text = stringResource(R.string.sign_out).uppercase(Locale.getDefault()))
                }
                else -> {
                    Text(stringResource(R.string.not_signed_in))
                    Spacer(modifier = Modifier.height(8.dp))
                    LexorcistOutlinedButton(onClick = {
                         if (activity != null) {
                           authViewModel.signIn(activity, mainViewModel)
                        }
                    }, text = stringResource(R.string.sign_in).uppercase(Locale.getDefault()))
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
                LexorcistOutlinedButton(onClick = {
                    settingsViewModel.disconnectDropbox()
                }, text = stringResource(R.string.disconnect_from_dropbox).uppercase(Locale.getDefault()))
            } else {
                LexorcistOutlinedButton(onClick = {
                    Auth.startOAuth2Authentication(context, context.getString(R.string.dropbox_app_key))
                }, text = stringResource(R.string.connect_to_dropbox).uppercase(Locale.getDefault()))
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
            // val oneDriveViewModel: OneDriveViewModel = hiltViewModel() // Already injected as parameter
            val oneDriveSignInState by oneDriveViewModel.oneDriveSignInState.collectAsState()

            when (val state = oneDriveSignInState) {
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Idle -> {
                    LexorcistOutlinedButton(onClick = {
                        if (activity != null) {
                            oneDriveViewModel.connectToOneDrive(activity)
                        }
                     }, text = stringResource(R.string.connect_to_onedrive).uppercase(Locale.getDefault()))
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.InProgress -> {
                    com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator()
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Success -> {
                    Text(stringResource(R.string.connected_to_onedrive_as_placeholder, state.accountName ?: stringResource(R.string.unknown_account)))
                     Spacer(modifier = Modifier.height(8.dp))
                    LexorcistOutlinedButton(onClick = {
                        oneDriveViewModel.disconnectFromOneDrive()
                    }, text = stringResource(R.string.disconnect_from_onedrive).uppercase(Locale.getDefault()))
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Error -> {
                    Text(stringResource(R.string.error_connecting_to_onedrive_placeholder, state.message ?: stringResource(R.string.unknown_error)))
                     Spacer(modifier = Modifier.height(8.dp))
                     LexorcistOutlinedButton(onClick = {
                        if (activity != null) {
                            oneDriveViewModel.connectToOneDrive(activity)
                        }
                     }, text = stringResource(R.string.retry).uppercase(Locale.getDefault()))
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache_title)) },
            text = { Text(stringResource(R.string.clear_cache_confirmation)) },
            confirmButton = {
                LexorcistOutlinedButton(
                    onClick = {
                        caseViewModel.clearCache()
                        showClearCacheDialog = false
                    },
                    text = stringResource(R.string.delete).uppercase(Locale.getDefault())
                )
            },
            dismissButton = {
                LexorcistOutlinedButton(
                    onClick = { showClearCacheDialog = false },
                    text = stringResource(R.string.cancel).uppercase(Locale.getDefault())
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageModelDownloader(
    models: List<LanguageModel>,
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDownload: (LanguageModel) -> Unit,
    onDelete: (LanguageModel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedModel = models.find { it.code == selectedLanguageCode }

    Column(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            TextField(
                value = selectedModel?.name ?: "Select Language",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.transcription_language)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true) // Corrected menuAnchor
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                models.forEach { model ->
                    val downloadState by model.downloadState.collectAsState()
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(model.name)
                                ModelStatusIcon(
                                    downloadState = downloadState,
                                    onDownload = { onDownload(model) },
                                    onDelete = { onDelete(model) }
                                )
                            }
                        },
                        onClick = {
                            onLanguageSelected(model.code)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Show progress bar for any model that is currently downloading
        val downloadingModel = models.find { it.downloadState.collectAsState().value is DownloadState.Downloading }
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
                Icon(Icons.Default.Download, contentDescription = "Download")
            }
        }
        is DownloadState.Downloading -> {
            com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator(modifier = Modifier.size(24.dp).padding(8.dp))
        }
        is DownloadState.Downloaded -> {
            Row {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
        is DownloadState.Error -> {
            // TODO: Maybe show an error icon and allow retry
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "Retry Download")
            }
        }
    }
}
