package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.MainViewModel
import com.hereliesaz.lexorcist.db.Case

@Composable
fun CasesScreen(viewModel: MainViewModel) {
    val cases by viewModel.cases.collectAsState()
    val selectedCase by viewModel.selectedCase.collectAsState()
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var caseName by remember { mutableStateOf("") }
    var exhibitSheetName by remember { mutableStateOf("Exhibit Matrix - Exhibit List") }
    var caseNumber by remember { mutableStateOf("") }
    var caseSection by remember { mutableStateOf("") }
    var caseJudge by remember { mutableStateOf("") }

    var showLoadCaseDialog by remember { mutableStateOf(false) }
    var spreadsheetIdToLoad by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End, // Align children to the End (right)
        verticalArrangement = Arrangement.Center // Center children vertically as a group
    ) {
        Text("Cases", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth() // LazyColumn itself should span width to allow items to align within it
        ) {
            items(cases) { case ->
                CaseItem(case = case, isSelected = case.id == selectedCase?.id) {
                    viewModel.selectCase(case)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Column for buttons to stack them and ensure they are part of the right-alignment
        Column(horizontalAlignment = Alignment.End) {
            Button(onClick = { showCreateCaseDialog = true }) {
                Text("Create New Case")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showLoadCaseDialog = true }) {
                Text("Load Case from Sheet")
            }
        }
    }

    if (showCreateCaseDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCaseDialog = false },
            title = { Text("New Case Details") },
            text = {
                Column {
                    OutlinedTextField(
                        value = caseName,
                        onValueChange = { caseName = it },
                        label = { Text("Case Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exhibitSheetName,
                        onValueChange = { exhibitSheetName = it },
                        label = { Text("Exhibit Sheet Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = caseNumber,
                        onValueChange = { caseNumber = it },
                        label = { Text("Case Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = caseSection,
                        onValueChange = { caseSection = it },
                        label = { Text("Case Section") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = caseJudge,
                        onValueChange = { caseJudge = it },
                        label = { Text("Case Judge") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (caseName.isNotBlank()) {
                            viewModel.createCase(caseName, exhibitSheetName, caseNumber, caseSection, caseJudge)
                            showCreateCaseDialog = false
                            caseName = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showCreateCaseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLoadCaseDialog) {
        AlertDialog(
            onDismissRequest = { showLoadCaseDialog = false },
            title = { Text("Load Case from Spreadsheet") },
            text = {
                OutlinedTextField(
                    value = spreadsheetIdToLoad,
                    onValueChange = { spreadsheetIdToLoad = it },
                    label = { Text("Spreadsheet ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (spreadsheetIdToLoad.isNotBlank()) {
                            viewModel.importSpreadsheet(spreadsheetIdToLoad)
                            showLoadCaseDialog = false
                            spreadsheetIdToLoad = ""
                        }
                    }
                ) {
                    Text("Load")
                }
            },
            dismissButton = {
                Button(onClick = { showLoadCaseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CaseItem(case: Case, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth() // Row still fills width to be clickable across the line
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End // Align text to the end (right) of the Row
    ) {
        Text(
            text = case.name,
            // modifier = Modifier.weight(1f), // Removed weight so horizontalArrangement can take effect
            style = if (isSelected) {
                androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            } else {
                androidx.compose.material3.MaterialTheme.typography.bodyLarge
            }
        )
    }
}
