package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Cases", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(cases) { case ->
                CaseItem(case = case, isSelected = case == selectedCase) {
                    viewModel.selectCase(case)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showCreateCaseDialog = true }) {
            Text("Create New Case")
        }
    }

    if (showCreateCaseDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateCaseDialog = false },
            title = { Text("New Case Name") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    label = { Text("Case Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (caseName.isNotBlank()) {
                            viewModel.createCase(caseName)
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
}

@Composable
fun CaseItem(case: Case, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = case.name,
            modifier = Modifier.weight(1f),
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
