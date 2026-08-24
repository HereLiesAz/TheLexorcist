package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailImportDialog(
    title: String,
    onDismiss: () -> Unit,
    onImport: (from: String, subject: String, before: String, after: String) -> Unit
) {
    var from by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var before by remember { mutableStateOf("") }
    var after by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.enter_search_criteria_to_find_the_emails_you_want))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it },
                    label = { Text(stringResource(R.string.from_e_g_user_example_com)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.subject)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = after,
                        onValueChange = { after = it },
                        label = { Text(stringResource(R.string.after_yyyy_mm_dd)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = before,
                        onValueChange = { before = it },
                        label = { Text(stringResource(R.string.before_yyyy_mm_dd)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onImport(from, subject, before, after) }) {
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