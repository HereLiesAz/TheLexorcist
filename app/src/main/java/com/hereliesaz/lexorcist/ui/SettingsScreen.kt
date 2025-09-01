package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel

@Composable
fun SettingsScreen(caseViewModel: CaseViewModel) {
    val isDarkMode by caseViewModel.isDarkMode.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // Apply padding to the outer Box
    ) {
        val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

        Column(
            modifier = Modifier
                .fillMaxSize() // Column fills the BoxWithConstraints
                .verticalScroll(rememberScrollState()), // Make content scrollable
            horizontalAlignment = Alignment.End // Align children (the Row) to the End (right)
        ) {
            Spacer(modifier = Modifier.height(halfScreenHeight)) // Push content to start halfway down

            Row(
                modifier = Modifier.fillMaxWidth(), // Row takes full width to allow its content to be aligned to End
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End // Align content of Row (Text + Switch) to the End
            ) {
                Text(
                    text = stringResource(R.string.dark_mode),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(16.dp)) // Added Spacer for better visual separation
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { caseViewModel.setDarkMode(it) }
                )
            }
            // Add other settings here if needed, they will follow the same alignment rules
        }
    }
}
