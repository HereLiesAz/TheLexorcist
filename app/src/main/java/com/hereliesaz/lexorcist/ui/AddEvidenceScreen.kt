package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
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
    evidenceViewModel: EvidenceViewModel = viewModel(),
    onAddTextEvidence: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onAddTextEvidence) {
            Icon(Icons.Default.Add, contentDescription = "Add Text Evidence")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Text Evidence")
        }
    }
}
