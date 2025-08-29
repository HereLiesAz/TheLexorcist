package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.CreateCaseDialog // Import the dialog from the parent package

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(caseViewModel: CaseViewModel) {
    val cases by caseViewModel.cases.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var caseToDelete by remember { mutableStateOf<Case?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cases)) },
                actions = {
                    IconButton(onClick = { /* TODO: Implement search */ }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = { /* TODO: Implement archive */ }) {
                        Icon(Icons.Filled.Archive, contentDescription = stringResource(R.string.archive))
                    }
                    IconButton(onClick = { /* TODO: Implement sort */ }) {
                        Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.sort))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateCaseDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_new_case))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (cases.isEmpty()) {
                    Text(stringResource(R.string.no_cases_found))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(cases) { case ->
                            CaseItem(
                                case = case,
                                isSelected = case.id == selectedCase?.id,
                                onClick = { caseViewModel.selectCase(case) },
                                onDelete = { caseToDelete = case }
                            )
                        }
                    }
                }
            }
        }
    }

    if (caseToDelete != null) {
        AlertDialog(
            onDismissRequest = { caseToDelete = null },
            title = { Text(stringResource(R.string.delete_case)) },
            text = { Text(stringResource(R.string.delete_case_confirmation, caseToDelete!!.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        caseViewModel.deleteCase(caseToDelete!!)
                        caseToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                Button(onClick = { caseToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCreateCaseDialog) {
        // Use the CreateCaseDialog from com.hereliesaz.lexorcist package (MainScreen.kt)
        CreateCaseDialog(
            caseViewModel = caseViewModel,
            onDismiss = { showCreateCaseDialog = false }
        )
    }
}

// Internal CreateCaseDialog composable removed

@Composable
fun CaseItem(case: Case, isSelected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
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
            },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_case))
        }
    }
}
