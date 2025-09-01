package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel

@Composable
fun AddTextEvidenceScreen(
    evidenceViewModel: EvidenceViewModel, // Parameter kept
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

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
            horizontalAlignment = Alignment.End // Existing: Align children to the End (right)
        ) {
            Spacer(modifier = Modifier.height(halfScreenHeight)) // Push content to start halfway down

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Evidence Text") },
                modifier = Modifier
                    .fillMaxWidth() // TextField still fills width
                    .weight(1f)      // TextField takes available vertical space after the Spacer
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { onSave(text) }) {
                Text("Save")
            }
        }
    }
}
