package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.* 
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel

@Composable
fun AddEvidenceScreen(
    evidenceViewModel: EvidenceViewModel = viewModel(), // Parameter kept
    onAddTextEvidence: () -> Unit,
) {
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
            horizontalAlignment = Alignment.End // Right-align children of this Column
        ) {
            Spacer(modifier = Modifier.height(halfScreenHeight)) // Push content to start halfway down

            Button(onClick = onAddTextEvidence) {
                Icon(Icons.Default.Add, contentDescription = "Add Text Evidence")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Text Evidence")
            }
        }
    }
}
