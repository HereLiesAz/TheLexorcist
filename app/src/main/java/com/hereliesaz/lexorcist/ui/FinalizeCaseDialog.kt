package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizeCaseDialog(
    caseViewModel: CaseViewModel,
    onDismiss: () -> Unit,
    onConfirm: (List<File>, String, String) -> Unit
) {
    val case by caseViewModel.selectedCase.collectAsState()
    val files = remember(case) {
        case?.let {
            val caseDir = File(caseViewModel.storageLocation.value, it.spreadsheetId)
            if (caseDir.exists() && caseDir.isDirectory) {
                caseDir.walk().filter { file -> file.isFile }.toList()
            } else {
                emptyList()
            }
        } ?: emptyList()
    }

    var selectedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var packageName by remember { mutableStateOf("") }
    var extension by remember { mutableStateOf("zip") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalize Case") },
        text = {
            Column {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") }
                )
                Row {
                    RadioButton(
                        selected = extension == "zip",
                        onClick = { extension = "zip" }
                    )
                    Text("ZIP")
                    RadioButton(
                        selected = extension == "lex",
                        onClick = { extension = "lex" }
                    )
                    Text("LEX")
                }
                LazyColumn {
                    items(files) { file ->
                        Row {
                            Checkbox(
                                checked = selectedFiles.contains(file),
                                onCheckedChange = {
                                    selectedFiles = if (it) {
                                        selectedFiles + file
                                    } else {
                                        selectedFiles - file
                                    }
                                }
                            )
                            Text(file.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AzButton(
                onClick = {
                    if (selectedFiles.isNotEmpty() && packageName.isNotBlank()) {
                        onConfirm(selectedFiles, packageName, extension)
                        onDismiss()
                    }
                },
                text = "Package"
            )
        },
        dismissButton = {
            AzButton(onClick = onDismiss, text = "Cancel")
        }
    )
}
