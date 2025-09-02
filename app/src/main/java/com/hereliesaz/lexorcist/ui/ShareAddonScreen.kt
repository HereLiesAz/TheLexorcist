package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material3.Button // Kept Button
import androidx.compose.material3.OutlinedButton // For Type toggle if preferred
import androidx.compose.material3.OutlinedTextField // Changed from TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ShareAddonScreen(onShare: (name: String, description: String, content: String, type: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Script") } // "Script" or "Template"

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp), // Apply padding to the outer Box
    ) {
        val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

        Column(
            modifier =
                Modifier
                    .fillMaxSize() // Column fills the BoxWithConstraints
                    .verticalScroll(rememberScrollState()),
            // Make content scrollable
            horizontalAlignment = Alignment.End, // Right-align children of this Column
        ) {
            Spacer(modifier = Modifier.height(halfScreenHeight)) // Push content to start halfway down

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth().weight(1f), // Allow content field to take more space
            )
            Spacer(modifier = Modifier.height(16.dp))
            // A simple way to select the type. A dropdown or RadioGroup would be better for more options.
            OutlinedButton(onClick = { type = if (type == "Script") "Template" else "Script" }, modifier = Modifier.fillMaxWidth()) {
                Text("Type: $type (Click to toggle)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onShare(name, description, content, type) }, modifier = Modifier.fillMaxWidth()) {
                Text("Share")
            }
        }
    }
}
