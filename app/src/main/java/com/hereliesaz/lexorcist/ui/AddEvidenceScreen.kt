package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.* 
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    // Removed BoxWithConstraints and internal Spacer for "start halfway down"
    // MainScreen.kt now handles the "start halfway down" for the NavHost area.
    Column(
        modifier = Modifier
            .fillMaxSize() // Fills the space given by NavHost
            .verticalScroll(rememberScrollState()) // Make content scrollable
            .padding(16.dp), // Apply padding to the Column for its content
        horizontalAlignment = Alignment.End, // Right-align children of this Column
        verticalArrangement = Arrangement.Top // Content should start from the top of this screen's area
    ) {
        // Content of AddEvidenceScreen starts here, at the top of the area NavHost provides
        Button(onClick = onAddTextEvidence) {
            Icon(Icons.Default.Add, contentDescription = "Add Text Evidence")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Text Evidence")
        }
        // Add other UI elements for this screen here if needed, they will be right-aligned.
    }
}
