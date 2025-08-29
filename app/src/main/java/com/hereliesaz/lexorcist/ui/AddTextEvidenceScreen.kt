package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Corrected import for the MainViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel

@Composable
fun AddTextEvidenceScreen(
    evidenceViewModel: EvidenceViewModel,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End, // Align children to the End (right)
        verticalArrangement = Arrangement.Center // Center children vertically as a group
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Evidence Text") },
            modifier = Modifier
                .fillMaxWidth() // TextField still fills width
                .weight(1f)      // TextField takes available vertical space
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { onSave(text) }) {
            Text("Save")
        }
    }
}
