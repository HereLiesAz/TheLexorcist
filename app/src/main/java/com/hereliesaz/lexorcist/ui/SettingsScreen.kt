package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog // Added import
import androidx.compose.material3.Button // Added import
import androidx.compose.material3.Divider // Added import
import androidx.compose.material3.ExperimentalMaterial3Api // Added
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton // Added import
import androidx.compose.material3.RadioButton // Added import
import androidx.compose.material3.Switch // Keep if used, seems not from snippet
import androidx.compose.material3.Text // Added import (explicit)
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf // Added import
import androidx.compose.runtime.remember // Added import
import androidx.compose.runtime.setValue // Added import (implicitly used by `by` delegate for mutableStateOf)
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class) // Added
@Composable
fun SettingsScreen(caseViewModel: CaseViewModel) {
    val themeMode by caseViewModel.themeMode.collectAsState()
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Theme Settings
        Text(
            text = stringResource(R.string.theme_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth()) {
            ThemeMode.values().forEach { mode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(text = mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    RadioButton(
                        selected = (themeMode == mode),
                        onClick = { caseViewModel.setThemeMode(mode) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        // Cache Settings
        Text(
            text = stringResource(R.string.cache_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { showClearCacheDialog = true }) {
            Text(text = stringResource(R.string.clear_cache))
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache_title)) },
            text = { Text(stringResource(R.string.clear_cache_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        // caseViewModel.clearCache() // TODO: Implement in ViewModel
                        showClearCacheDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete)) // Changed to R.string.delete as a placeholder
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
