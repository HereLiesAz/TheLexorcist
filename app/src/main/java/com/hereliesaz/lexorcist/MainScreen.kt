package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onSignIn: () -> Unit,
    onCreateMasterTemplate: () -> Unit,
    onCreateCase: (String) -> Unit // Corrected this line
) {
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var caseName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lexorcist") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSignedIn) {
                AuthenticatedView(
                    onCreateMasterTemplate = onCreateMasterTemplate,
                    onCreateCase = { showCreateCaseDialog = true }
                )
            } else {
                Button(onClick = onSignIn) {
                    Text("Sign in with Google")
                }
            }
        }
    }

    if (showCreateCaseDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCaseDialog = false },
            title = { Text("New Case Name") },
            text = {
                OutlinedTextField(
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
                            onCreateCase(caseName)
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
fun AuthenticatedView(
    onCreateMasterTemplate: () -> Unit,
    onCreateCase: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onCreateMasterTemplate) {
            Text("Create/Update Master Template")
        }
        Button(onClick = onCreateCase) {
            Text("Create New Case")
        }
        // Add other authenticated UI elements here (e.g., list cases, etc.)
    }
}