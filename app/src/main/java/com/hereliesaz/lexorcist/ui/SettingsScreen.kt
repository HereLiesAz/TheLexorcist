package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    caseViewModel: CaseViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val themeMode by settingsViewModel.themeMode.collectAsState()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as android.app.Activity

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
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Theme Settings
            Text(
                text = stringResource(R.string.theme_settings),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
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

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Transcription Service Settings
            Text(
                text = "Transcription Service",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            val selectedTranscriptionService by settingsViewModel.transcriptionService.collectAsState()
            val transcriptionServices = listOf("Vosk", "Whisper")

            Column(Modifier.fillMaxWidth()) {
                transcriptionServices.forEach { service ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(text = service)
                        RadioButton(
                            selected = (selectedTranscriptionService == service),
                            onClick = { settingsViewModel.setTranscriptionService(service) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Cache Settings
            Text(
                text = stringResource(R.string.cache_settings),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            LexorcistOutlinedButton(onClick = { showClearCacheDialog = true }, text = stringResource(R.string.clear_cache))

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Storage Settings
            Text(
                text = "Storage Settings",
                style = MaterialTheme.typography.headlineSmall,
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
            Text("Current location: ${storageLocation ?: "Default"}")
            Spacer(modifier = Modifier.height(8.dp))
            LexorcistOutlinedButton(onClick = { directoryPickerLauncher.launch(null) }, text = "Change Storage Location")

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Cloud Service Settings
            Text(
                text = "Cloud Service Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )

            val selectedCloudProvider by settingsViewModel.selectedCloudProvider.collectAsState()
            val cloudProviders = listOf("GoogleDrive", "Dropbox", "OneDrive", "None")

            Column(Modifier.fillMaxWidth()) {
                cloudProviders.forEach { provider ->
                    val isEnabled = when (provider) {
                        "GoogleDrive" -> signInState is SignInState.Success
                        else -> true // Assuming other providers don't depend on Google Sign-In
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
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

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign-In Status
            when (val currentSignInState = signInState) { // Use a different name to avoid conflict
                is SignInState.Success -> {
                    val userInfo = currentSignInState.userInfo
                    Text("Signed in as: ${userInfo?.email ?: "Unknown Email"}") // Safe call and provide a default
                    Spacer(modifier = Modifier.height(8.dp))
                    LexorcistOutlinedButton(onClick = {
                        authViewModel.signOut()
                        authViewModel.signIn(activity) // Or provide a new activity instance if needed
                    }, text = "Switch Account")
                }
                else -> {
                    Text("Not signed in.")
                    Spacer(modifier = Modifier.height(8.dp))
                    LexorcistOutlinedButton(onClick = { authViewModel.signIn(activity) }, text = "Sign In")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Dropbox",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            val isDropboxAuthenticated by settingsViewModel.isDropboxAuthenticated.collectAsState()
            val dropboxUser by settingsViewModel.dropboxUser.collectAsState()

            if (isDropboxAuthenticated) {
                Text("Connected as: ${dropboxUser?.email ?: "..."}")
                Spacer(modifier = Modifier.height(8.dp))
                LexorcistOutlinedButton(onClick = {
                    settingsViewModel.disconnectDropbox()
                }, text = "Disconnect from Dropbox")
            } else {
                LexorcistOutlinedButton(onClick = {
                    Auth.startOAuth2Authentication(context, "kc574fk4ljbmxeu")
                }, text = "Connect to Dropbox")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "OneDrive",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            val oneDriveViewModel: com.hereliesaz.lexorcist.viewmodel.OneDriveViewModel = hiltViewModel()
            val oneDriveSignInState by oneDriveViewModel.oneDriveSignInState.collectAsState()

            when (val state = oneDriveSignInState) {
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Idle -> {
                    LexorcistOutlinedButton(onClick = { oneDriveViewModel.connectToOneDrive(activity) }, text = "Connect to OneDrive")
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.InProgress -> {
                    Text("Connecting to OneDrive...")
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Success -> {
                    Text("Connected to OneDrive as ${state.accountName}")
                }
                is com.hereliesaz.lexorcist.model.OneDriveSignInState.Error -> {
                    Text("Error connecting to OneDrive: ${state.message}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { settingsViewModel.testOneDriveUpload() }) {
                Text("Test OneDrive Upload")
            }

            val oneDriveUploadStatus by settingsViewModel.oneDriveUploadStatus.collectAsState()
            oneDriveUploadStatus?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { settingsViewModel.clearOneDriveUploadStatus() }) {
                    Text("Clear Status")
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
                    text = stringResource(R.string.delete)
                )
            },
            dismissButton = {
                LexorcistOutlinedButton(onClick = { showClearCacheDialog = false }, text = stringResource(R.string.cancel))
            },
        )
    }
}
