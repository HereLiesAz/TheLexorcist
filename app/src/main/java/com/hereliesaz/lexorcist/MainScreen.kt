package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lexorcist.components.AppNavRail
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.ui.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onSignIn: () -> Unit,
    onSelectImage: () -> Unit,
    onTakePicture: () -> Unit,
    onAddDocument: () -> Unit,
    onAddSpreadsheet: () -> Unit
) {
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    var currentScreen by remember { mutableStateOf("home") }
    var showCreateCaseDialog by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        if (isSignedIn) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AppNavRail(onNavigate = { screen -> currentScreen = screen })
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        "home" -> AuthenticatedView(
                            onCreateMasterTemplate = {
                                // TODO: Implement Master Template Creation UI/Flow
                                viewModel.viewModelScope.launch {
                                    viewModel.googleApiService.value?.createMasterTemplate()
                                }
                            },
                            onCreateCase = { showCreateCaseDialog = true }
                        )
                        "cases" -> CasesScreen(viewModel = viewModel)
                        "add_evidence" -> AddEvidenceScreen(
                            viewModel = viewModel,
                            onSelectImage = onSelectImage,
                            onTakePicture = onTakePicture,
                            onAddTextEvidence = { currentScreen = "add_text_evidence" },
                            onAddDocument = onAddDocument,
                            onAddSpreadsheet = onAddSpreadsheet
                        )
                        "add_text_evidence" -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            AddTextEvidenceScreen(viewModel = viewModel, onSave = { text ->
                                viewModel.addTextEvidenceToSelectedCase(text, context)
                                currentScreen = "cases"
                            })
                        }
                        "timeline" -> TimelineScreen(viewModel = viewModel)
                        "settings" -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = onSignIn) {
                    Text("Sign in with Google")
                }
            }
        }

        if (showCreateCaseDialog) {
            CreateCaseDialog(
                viewModel = viewModel,
                onDismiss = { showCreateCaseDialog = false }
            )
        }
    }
}

@Composable
fun AuthenticatedView(
    onCreateMasterTemplate: () -> Unit,
    onCreateCase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // Centered for buttons
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "The Lexorcist",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Use the navigation rail to get started.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Tap the icon at the top to open the menu.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(onClick = onCreateCase) {
            Text("Create New Case")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateMasterTemplate) {
            Text("Create New Master HTML Template")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCaseDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val templates by viewModel.htmlTemplates.collectAsState()
    var caseName by remember { mutableStateOf("") }
    var exhibitSheetName by remember { mutableStateOf("Exhibits") } // Default
    var caseNumber by remember { mutableStateOf("") }
    var caseSection by remember { mutableStateOf("") }
    var caseJudge by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    // Initialize with a placeholder or the first template if available
    var selectedTemplateName by remember {
        mutableStateOf(templates.firstOrNull()?.name ?: "Select a Template")
    }
    // Effect to update selectedTemplateName if the first template becomes available later
    LaunchedEffect(templates) {
        if (selectedTemplateId == null && templates.isNotEmpty()) {
            templates.firstOrNull()?.let {
                // selectedTemplateId = it.id // Optionally pre-select the first one
                // selectedTemplateName = it.name ?: "Unnamed Template"
            }
        } else if (selectedTemplateId == null && templates.isEmpty()) {
            selectedTemplateName = "No templates found"
        }
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Case") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()), // Make column scrollable
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    label = { Text("Case Name*") },
                    isError = caseName.isBlank()
                )
                TextField(
                    value = exhibitSheetName,
                    onValueChange = { exhibitSheetName = it },
                    label = { Text("Exhibit Sheet Name") }
                )
                TextField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = { Text("Case Number") }
                )
                TextField(
                    value = caseSection,
                    onValueChange = { caseSection = it },
                    label = { Text("Case Section") }
                )
                TextField(
                    value = caseJudge,
                    onValueChange = { caseJudge = it },
                    label = { Text("Judge") }
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedTemplateName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Master Template*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        isError = selectedTemplateId == null
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (templates.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No templates found. Create one via Home screen.") },
                                onClick = { expanded = false },
                                enabled = false
                            )
                        }
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name ?: "Unnamed Template (ID: ${template.id.take(8)}...)") },
                                onClick = {
                                    selectedTemplateId = template.id
                                    selectedTemplateName = template.name ?: "Unnamed Template (ID: ${template.id.take(8)}...)"
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (caseName.isNotBlank() && selectedTemplateId != null) {
                        viewModel.createCase(
                            caseName = caseName,
                            exhibitSheetName = exhibitSheetName.ifBlank { "Exhibits" },
                            caseNumber = caseNumber,
                            caseSection = caseSection,
                            caseJudge = caseJudge,
                            selectedMasterHtmlTemplateId = selectedTemplateId!!
                        )
                        onDismiss()
                    } 
                    // Basic validation handled by enabling/disabling button and isError on fields
                },
                enabled = caseName.isNotBlank() && selectedTemplateId != null
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
