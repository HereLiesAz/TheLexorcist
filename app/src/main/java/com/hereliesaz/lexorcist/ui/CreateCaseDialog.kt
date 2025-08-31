package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel

@Composable
fun CreateCaseDialog(
    caseViewModel: CaseViewModel,
    onDismiss: () -> Unit
) {
    var caseName by remember { mutableStateOf("") }
    var exhibitSheetName by remember { mutableStateOf("") }
    var caseNumber by remember { mutableStateOf("") }
    var caseSection by remember { mutableStateOf("") }
    var caseJudge by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Case") },
        text = {
            Column {
                OutlinedTextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    label = { Text("Case Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exhibitSheetName,
                    onValueChange = { exhibitSheetName = it },
                    label = { Text("Exhibit Sheet Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = { Text("Case Number") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = caseSection,
                    onValueChange = { caseSection = it },
                    label = { Text("Case Section") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = caseJudge,
                    onValueChange = { caseJudge = it },
                    label = { Text("Case Judge") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    caseViewModel.createNewCaseWithRepository(
                        caseName,
                        exhibitSheetName,
                        caseNumber,
                        caseSection,
                        caseJudge
                    )
                    onDismiss()
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
