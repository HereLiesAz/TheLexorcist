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
// import com.hereliesaz.lexorcist.viewmodel.OcrViewModel // Not directly used in MainScreen params
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
// import com.hereliesaz.lexorcist.viewmodel.EvidenceDetailsViewModel // Not directly used in MainScreen params
import com.hereliesaz.lexorcist.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    caseViewModel: CaseViewModel = viewModel(),
    evidenceViewModel: EvidenceViewModel = viewModel(),
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
        when (val currentSignInState = signInState) {
            is SignInState.Success -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues) // Apply scaffold padding to the Row
                ) {
                    AppNavRail(onNavigate = { screen -> navController.navigate(screen) })
                    
                    // Content area to the right of the NavRail
                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val halfContentAreaHeight = this@BoxWithConstraints.maxHeight / 2
                        // val contentAreaMaxHeight = this@BoxWithConstraints.maxHeight // Not strictly needed here for NavHost modifier

                        Column(
                            modifier = Modifier
                                .fillMaxSize() // Fills the BoxWithConstraints
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(halfContentAreaHeight)) // Spacer for NavHost content area

                            NavHost(
                                navController = navController, 
                                startDestination = "home",
                                modifier = Modifier.fillMaxHeight() // NavHost fills the remaining space in the scrollable column
                            ) {
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
            }
            is SignInState.InProgress -> {
                 BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)){
                    val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(halfScreenHeight))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            is SignInState.Idle, is SignInState.Error -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)){
                    val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2
                     Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp), // Padding for the content itself
                        horizontalAlignment = Alignment.End
                    ) {
                        Spacer(Modifier.height(halfScreenHeight))
                        Button(onClick = onSignInClick) {
                            Text(stringResource(R.string.sign_in_with_google))
                        }
                        if (currentSignInState is SignInState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentSignInState.message,
                                color = MaterialTheme.colorScheme.error
                            )
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
    Column(
        modifier = Modifier
            .fillMaxSize() 
            .verticalScroll(rememberScrollState()) 
            .padding(16.dp),
        horizontalAlignment = Alignment.End, 
        verticalArrangement = Arrangement.Top // Reverted to Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.use_navigation_rail),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tap_icon_to_open_menu),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
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
                horizontalAlignment = Alignment.End 
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
