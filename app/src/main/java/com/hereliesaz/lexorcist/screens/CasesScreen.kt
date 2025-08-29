package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.db.Case
import com.hereliesaz.lexorcist.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(viewModel: MainViewModel) {
    val cases by viewModel.cases.collectAsState()
    val selectedCase by viewModel.selectedCase.collectAsState()
    var showCreateCaseDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateCaseDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Case")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Cases", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(cases) { case ->
                    CaseItem(
                        case = case,
                        isSelected = case.id == selectedCase?.id,
                        onClick = { viewModel.selectCase(case) }
                    )
                }
            }
        }
    }

    if (showCreateCaseDialog) {
        CreateCaseDialog(
            onDismiss = { showCreateCaseDialog = false },
            onCreate = { caseName, exhibitSheetName, caseNumber, caseSection, caseJudge ->
                viewModel.createCase(caseName, exhibitSheetName, caseNumber, caseSection, caseJudge)
                showCreateCaseDialog = false
            }
        )
    }
}

@Composable
fun CaseItem(case: Case, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = case.name,
            modifier = Modifier.padding(16.dp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CreateCaseDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String) -> Unit
) {
    var caseName by remember { mutableStateOf("") }
    var exhibitSheetName by remember { mutableStateOf("Exhibit Matrix - Exhibit List") }
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
                OutlinedTextField(
                    value = exhibitSheetName,
                    onValueChange = { exhibitSheetName = it },
                    label = { Text("Exhibit Sheet Name") }
                )
                OutlinedTextField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = { Text("Case Number") }
                )
                OutlinedTextField(
                    value = caseSection,
                    onValueChange = { caseSection = it },
                    label = { Text("Case Section") }
                )
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
                    if (caseName.isNotBlank()) {
                        onCreate(caseName, exhibitSheetName, caseNumber, caseSection, caseJudge)
                    }
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
