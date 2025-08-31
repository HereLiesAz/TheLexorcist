package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun ShareAddonScreen(
    onShare: (name: String, description: String, content: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Script") } // "Script" or "Template"

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") }
        )
        TextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") }
        )
        // A simple way to select the type. A dropdown would be better.
        Button(onClick = { type = if (type == "Script") "Template" else "Script" }) {
            Text("Type: $type")
        }
        Button(onClick = { onShare(name, description, content, type) }) {
            Text("Share")
        }
    }
}
