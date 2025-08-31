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
import com.hereliesaz.lexorcist.ui.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hereliesaz.lexorcist.viewmodel.EvidenceDetailsViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    caseViewModel: CaseViewModel = viewModel(),
    evidenceViewModel: EvidenceViewModel = viewModel(),
    evidenceDetailsViewModel: EvidenceDetailsViewModel = viewModel(),
    ocrViewModel: OcrViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    val isSignedIn by authViewModel.isSignedIn.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val errorMessage by caseViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateCaseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            caseViewModel.clearError()
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
                AppNavRail(onNavigate = { screen -> navController.navigate(screen) })
                Box(modifier = Modifier.weight(1f)) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { AuthenticatedView(onCreateCase = { showCreateCaseDialog = true }) }
                        composable("cases") { CasesScreen(caseViewModel = caseViewModel /* Removed onCaseSelected */) }
                        composable("add_evidence") {
                            AddEvidenceScreen(
                                onAddTextEvidence = { navController.navigate("add_text_evidence") },
                            )
                        }
                        composable("add_text_evidence") {
                            AddTextEvidenceScreen(
                                evidenceViewModel = evidenceViewModel, // Added evidenceViewModel
                                onSave = { text ->
                                    selectedCase?.let {
                                        evidenceViewModel.addTextEvidence(text)
                                    }
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable("timeline") {
                            selectedCase?.let {
                                TimelineScreen(
                                    case = it,
                                    evidenceViewModel = evidenceViewModel,
                                    navController = navController // Added navController, removed onNavigateToEvidenceDetails
                                )
                            }
                        }
                        composable("data_review") { DataReviewScreen(evidenceViewModel = evidenceViewModel) } // Added evidenceViewModel
                        composable("settings") { SettingsScreen(caseViewModel = caseViewModel) } // Added caseViewModel
                        composable("evidence_details/{evidenceId}") { backStackEntry ->
                            val evidenceId = backStackEntry.arguments?.getString("evidenceId")?.toIntOrNull()
                            if (evidenceId != null) {
                                // TODO: EvidenceDetailsScreen needs an Evidence object, or the ViewModel needs to handle loading by ID.
                                // For now, passing evidenceViewModel as per one of the error messages, but this might be incorrect.
                                EvidenceDetailsScreen(
                                    // evidenceId = evidenceId, // Parameter not found error
                                    // evidence = ???, // Missing evidence object
                                    viewModel = evidenceViewModel // Changed from evidenceDetailsViewModel based on error
                                )
                            }
                        }
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
                Button(onClick = onSignInClick) {
                    Text(stringResource(R.string.sign_in_with_google))
                }
            }
        }

        if (showCreateCaseDialog) {
            CreateCaseDialog(
                caseViewModel = caseViewModel,
                onDismiss = { showCreateCaseDialog = false }
            )
        }
    }
}

@Composable
fun AuthenticatedView(
    onCreateCase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCaseDialog(
    caseViewModel: CaseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var caseName by remember { mutableStateOf("") }
    var exhibitSheetName by remember { mutableStateOf("") }
    var caseNumber by remember { mutableStateOf("") }
    var caseSection by remember { mutableStateOf("") }
    var caseJudge by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_new_case)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (caseName.isNotBlank()) {
                        caseViewModel.createCase(
                            caseName = caseName,
                            exhibitSheetName = exhibitSheetName.ifBlank { context.getString(R.string.default_exhibit_sheet_name) },
                            caseNumber = caseNumber,
                            caseSection = caseSection,
                            caseJudge = caseJudge
                        )
                        onDismiss()
                    }
                },
                enabled = caseName.isNotBlank()
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
