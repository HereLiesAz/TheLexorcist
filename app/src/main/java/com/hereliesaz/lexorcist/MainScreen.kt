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
import com.hereliesaz.lexorcist.model.SignInState
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
    // evidenceDetailsViewModel: EvidenceDetailsViewModel = viewModel(), // Not directly used in MainScreen params
    // ocrViewModel: OcrViewModel = viewModel(), // Not directly used in MainScreen params
    mainViewModel: MainViewModel = viewModel(),
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    val signInState by authViewModel.signInState.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val caseSpecificErrorMessage by caseViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateCaseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(caseSpecificErrorMessage) {
        caseSpecificErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            caseViewModel.clearError()
        }
    }

    LaunchedEffect(signInState) {
        if (signInState is SignInState.Error) {
            val errorState = signInState as SignInState.Error
            snackbarHostState.showSnackbar("Sign-In Error: ${errorState.message}")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(halfScreenHeight)) // Push all content down initially

                when (signInState) {
                    is SignInState.Success -> {
                        Row(
                            modifier = Modifier.fillMaxSize() // This Row is now after the Spacer
                        ) {
                            AppNavRail(onNavigate = { screen -> navController.navigate(screen) })
                            Box(modifier = Modifier.weight(1f)) {
                                NavHost(navController = navController, startDestination = "home") {
                                    composable("home") { AuthenticatedView(onCreateCase = { showCreateCaseDialog = true }) }
                                    composable("cases") { CasesScreen(caseViewModel = caseViewModel) }
                                    composable("add_evidence") {
                                        AddEvidenceScreen(
                                            onAddTextEvidence = { navController.navigate("add_text_evidence") },
                                        )
                                    }
                                    composable("add_text_evidence") {
                                        AddTextEvidenceScreen(
                                            evidenceViewModel = evidenceViewModel,
                                            onSave = { text ->
                                                selectedCase?.let { case ->
                                                    evidenceViewModel.addTextEvidence(
                                                        text = text,
                                                        caseId = case.id.toLong(),
                                                        spreadsheetId = case.spreadsheetId
                                                    )
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
                                                navController = navController
                                            )
                                        }
                                    }
                                    composable("data_review") { 
                                        DataReviewScreen(
                                            evidenceViewModel = evidenceViewModel, 
                                            caseViewModel = caseViewModel
                                        ) 
                                    }
                                    composable("settings") { SettingsScreen(caseViewModel = caseViewModel) }
                                    composable("evidence_details/{evidenceId}") { backStackEntry ->
                                        val evidenceIdString = backStackEntry.arguments?.getString("evidenceId")
                                        val evidenceId = remember(evidenceIdString) { evidenceIdString?.toIntOrNull() }

                                        if (evidenceId != null) {
                                            LaunchedEffect(evidenceId) {
                                                evidenceViewModel.loadEvidenceDetails(evidenceId)
                                            }
                                            val evidence by evidenceViewModel.selectedEvidenceDetails.collectAsState()
                                            evidence?.let { ev ->
                                                EvidenceDetailsScreen(
                                                    evidence = ev,
                                                    viewModel = evidenceViewModel
                                                )
                                            }
                                            DisposableEffect(Unit) {
                                                onDispose {
                                                    evidenceViewModel.clearEvidenceDetails()
                                                }
                                            }
                                        } else {
                                            Text("Error: Evidence ID not found or invalid.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is SignInState.InProgress -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(), // Box takes full width to align content within it
                            contentAlignment = Alignment.CenterEnd // Right-align the CircularProgressIndicator
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is SignInState.Idle, is SignInState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth() // Column takes full width for alignment
                                .padding(horizontal = 16.dp), // Keep horizontal padding for content
                            horizontalAlignment = Alignment.End // Right-align Button and Text
                        ) {
                            Button(onClick = onSignInClick) {
                                Text(stringResource(R.string.sign_in_with_google))
                            }
                            if (signInState is SignInState.Error) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = (signInState as SignInState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) { // AuthenticatedView also starts halfway
        val halfViewHeight = this@BoxWithConstraints.maxHeight / 2
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.End, // Right-align Text and Button
            // verticalArrangement = Arrangement.Center - Replaced by Spacer
        ) {
            Spacer(Modifier.height(halfViewHeight)) // Push content down
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                // modifier = Modifier.padding(bottom = 16.dp) // padding handled by Spacers or direct layout
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.use_navigation_rail),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tap_icon_to_open_menu),
                style = MaterialTheme.typography.bodyLarge,
                // modifier = Modifier.padding(bottom = 32.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onCreateCase) {
                Text(stringResource(R.string.create_new_case))
            }
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
    val defaultExhibitSheetNameStr = stringResource(R.string.default_exhibit_sheet_name)
    var exhibitSheetName by remember { mutableStateOf(defaultExhibitSheetNameStr) }
    var caseNumber by remember { mutableStateOf("") }
    var caseSection by remember { mutableStateOf("") }
    var caseJudge by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_new_case)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End // Right-align TextFields
            ) {
                OutlinedTextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    label = { Text(stringResource(R.string.case_name_required)) },
                    isError = caseName.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = exhibitSheetName,
                    onValueChange = { exhibitSheetName = it },
                    label = { Text(stringResource(R.string.exhibit_sheet_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = { Text(stringResource(R.string.case_number)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseSection,
                    onValueChange = { caseSection = it },
                    label = { Text(stringResource(R.string.case_section)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseJudge,
                    onValueChange = { caseJudge = it },
                    label = { Text(stringResource(R.string.judge)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (caseName.isNotBlank()) {
                        caseViewModel.createCase(
                            caseName = caseName,
                            exhibitSheetName = exhibitSheetName.ifBlank { defaultExhibitSheetNameStr },
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
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
