package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImapImportDialog(
    onDismiss: () -> Unit,
    onImport: (host: String, user: String, pass: String, from: String, subject: String) -> Unit
) {
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_from_imap)) },
        text = {
            Column {
                Text("Enter your email credentials and search criteria.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your password is used only for this import session and is not saved.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.imap_server_e_g_imap_mail_com)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text(stringResource(R.string.email_address)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Search Filters (Optional)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it },
                    label = { Text(stringResource(R.string.from)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.subject)) }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onImport(host, user, pass, from, subject) }) {
                Text(stringResource(R.string.import_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}