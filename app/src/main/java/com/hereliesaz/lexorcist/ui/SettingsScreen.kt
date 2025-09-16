package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider // Corrected import
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.ui.theme.ThemeMode
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Switch
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.lexorcist.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, caseViewModel: CaseViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val caseFolderPath by viewModel.caseFolderPath.collectAsState()
    val cloudSyncEnabled by viewModel.cloudSyncEnabled.collectAsState()
    val migrationStatus by viewModel.migrationStatus.collectAsState()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.setCaseFolderPath(it.toString())
            }
        }
    )

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
                text = "Theme",
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
                            onClick = { viewModel.setThemeMode(mode) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Storage Settings
            Text(
                text = "Storage",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Current Location: ${caseFolderPath ?: "Default"}")
            Spacer(modifier = Modifier.height(8.dp))
            LexorcistOutlinedButton(
                onClick = { /* TODO: Implement in future */ },
                text = "Change Folder Location (Coming Soon)",
                enabled = false
            )
            migrationStatus?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it)
                if (it != "Migrating files...") {
                    LaunchedEffect(it) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearMigrationStatus()
                    }
                }
            }


            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Sync Settings
            Text(
                text = "Synchronization",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(text = "Enable Cloud Sync")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = cloudSyncEnabled,
                    onCheckedChange = { viewModel.setCloudSyncEnabled(it) }
                )
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
