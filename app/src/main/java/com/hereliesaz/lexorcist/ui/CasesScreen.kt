package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.CreateCaseDialog // Import the dialog from the parent package

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(viewModel: MainViewModel) {
    val cases by viewModel.cases.collectAsState()
    val selectedCase by viewModel.selectedCase.collectAsState()
    var showCreateCaseDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateCaseDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create New Case")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Cases", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            if (cases.isEmpty()) {
                Text("No cases found. Click the '+' button to create a new case.")
            } else {
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
    }

    if (showCreateCaseDialog) {
        // Use the CreateCaseDialog from com.hereliesaz.lexorcist package (MainScreen.kt)
        CreateCaseDialog(
            viewModel = viewModel,
            onDismiss = { showCreateCaseDialog = false }
        )
    }
}

// Internal CreateCaseDialog composable removed

@Composable
fun CaseItem(case: Case, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = case.name,
            style = if (isSelected) {
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyLarge
            }
        )
    }
}
