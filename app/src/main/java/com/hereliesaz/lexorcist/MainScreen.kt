package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentScreen by remember { mutableStateOf(R.string.nav_home) }
    var showCreateCaseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isSignedIn) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AppNavRail(onNavigate = { screen -> currentScreen = context.resources.getIdentifier(screen, "string", context.packageName) })
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        R.string.nav_home -> AuthenticatedView(
                            // onCreateMasterTemplate parameter removed
                            onCreateCase = { showCreateCaseDialog = true }
                        )
                        R.string.nav_cases -> CasesScreen(viewModel = viewModel)
                        R.string.nav_add_evidence -> AddEvidenceScreen(
                            viewModel = viewModel,
                            onSelectImage = onSelectImage,
                            onTakePicture = onTakePicture,
                            onAddTextEvidence = { currentScreen = R.string.nav_add_text_evidence },
                            onAddDocument = onAddDocument,
                            onAddSpreadsheet = onAddSpreadsheet
                        )
                        R.string.nav_add_text_evidence -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            AddTextEvidenceScreen(viewModel = viewModel, onSave = { text ->
                                viewModel.addTextEvidenceToSelectedCase(text, context)
                                currentScreen = R.string.nav_cases
                            })
                        }
                        R.string.nav_timeline -> TimelineScreen(viewModel = viewModel)
                        R.string.nav_visualization -> VisualizationScreen(viewModel = viewModel)
                        R.string.nav_data_review -> DataReviewScreen(viewModel = viewModel)
                        R.string.nav_settings -> SettingsScreen(viewModel = viewModel)
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
                    Text(stringResource(R.string.sign_in_with_google))
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
    // onCreateMasterTemplate parameter removed
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
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = stringResource(R.string.use_navigation_rail),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.tap_icon_to_open_menu),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(onClick = onCreateCase) {
            Text(stringResource(R.string.create_new_case))
        }
        // Spacer and "Create New Master HTML Template" Button removed
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
    var exhibitSheetName by remember { mutableStateOf(stringResource(R.string.default_exhibit_sheet_name)) } // Default
    var caseNumber by remember { mutableStateOf("") }
    var caseSection by remember { mutableStateOf("") }
    var caseJudge by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    // Initialize with a placeholder or the first template if available
    var selectedTemplateName by remember {
        mutableStateOf(templates.firstOrNull()?.name ?: stringResource(R.string.select_a_template))
    }
    // Effect to update selectedTemplateName if the first template becomes available later
    LaunchedEffect(templates) {
        if (selectedTemplateId == null && templates.isNotEmpty()) {
            templates.firstOrNull()?.let {
                // selectedTemplateId = it.id // Optionally pre-select the first one
                // selectedTemplateName = it.name ?: "Unnamed Template"
            }
        } else if (selectedTemplateId == null && templates.isEmpty()) {
            selectedTemplateName = stringResource(R.string.no_templates_found)
        }
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_new_case)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()), // Make column scrollable
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    label = { Text(stringResource(R.string.case_name_required)) },
                    isError = caseName.isBlank()
                )
                TextField(
                    value = exhibitSheetName,
                    onValueChange = { exhibitSheetName = it },
                    label = { Text(stringResource(R.string.exhibit_sheet_name)) }
                )
                TextField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = { Text(stringResource(R.string.case_number)) }
                )
                TextField(
                    value = caseSection,
                    onValueChange = { caseSection = it },
                    label = { Text(stringResource(R.string.case_section)) }
                )
                TextField(
                    value = caseJudge,
                    onValueChange = { caseJudge = it },
                    label = { Text(stringResource(R.string.judge)) }
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedTemplateName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.master_template_required)) },
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
                                text = { Text(stringResource(R.string.no_templates_found_create_one)) }, // Text can be updated as the button is removed
                                onClick = { expanded = false },
                                enabled = false
                            )
                        }
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name ?: stringResource(R.string.unnamed_template, template.id.take(8))) },
                                onClick = {
                                    selectedTemplateId = template.id
                                    selectedTemplateName = template.name ?: stringResource(R.string.unnamed_template, template.id.take(8))
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
                            exhibitSheetName = exhibitSheetName.ifBlank { stringResource(R.string.default_exhibit_sheet_name) },
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
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
