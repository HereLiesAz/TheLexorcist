package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailImportDialog(
    onDismiss: () -> Unit,
    onImport: (from: String, subject: String, before: String, after: String) -> Unit
) {
    var from by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var before by remember { mutableStateOf("") }
    var after by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Gmail") },
        text = {
            Column {
                Text("Enter search criteria to find the emails you want to import.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it },
                    label = { Text("From (e.g., user@example.com)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = after,
                        onValueChange = { after = it },
                        label = { Text("After (YYYY/MM/DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = before,
                        onValueChange = { before = it },
                        label = { Text("Before (YYYY/MM/DD)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onImport(from, subject, before, after) }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}