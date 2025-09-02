package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Covers CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lexorcist.components.AppNavRail
import com.hereliesaz.lexorcist.ui.EvidenceScreen
import com.hereliesaz.lexorcist.ui.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// import android.content.Intent // Not used directly in this snippet
// import androidx.compose.ui.platform.LocalContext // Not used directly in this snippet
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.ui.ScriptEditorScreen
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptEditorViewModel
// import java.util.Locale // Not used directly in this snippet
import com.hereliesaz.lexorcist.ui.CreateCaseDialog

@OptIn(ExperimentalMaterial3Api::class) // ENSURED THIS IS CORRECT
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

    LaunchedEffect(Unit) {
        mainViewModel.createAllegationsSheet()
        // The user will need to get the spreadsheet ID from the logs and replace the placeholder
        mainViewModel.populateAllegationsSheet("PLACEHOLDER_SPREADSHEET_ID")
    }

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
                        .padding(paddingValues) 
                ) {
                    AppNavRail(onNavigate = { screen -> navController.navigate(screen) })
                    
                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val halfContentAreaHeight = this@BoxWithConstraints.maxHeight / 2
                        val contentAreaViewportHeight = this@BoxWithConstraints.maxHeight

                        Column(
                            modifier = Modifier
                                .fillMaxSize() 
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(halfContentAreaHeight)) 

                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.height(contentAreaViewportHeight)
                            ) {
                                composable("home") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.Top
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
                                        Button(onClick = { showCreateCaseDialog = true }) {
                                            Text(stringResource(R.string.create_new_case))
                                        }
                                    }
                                }
                                composable("cases") { CasesScreen(caseViewModel = caseViewModel) }
                                composable("evidence") {
                                    EvidenceScreen(
                                        evidenceViewModel = evidenceViewModel,
                                        navController = navController
                                    )
                                }
                                composable("extras") { ExtrasScreen(onShare = {}) }
                                composable("script_editor") {
                                    val scriptEditorViewModel: ScriptEditorViewModel = hiltViewModel()
                                    ScriptEditorScreen(viewModel = scriptEditorViewModel)
                                }
                                composable("allegations") {
                                    AllegationsScreen()
                                }
                                composable("templates") {
                                    TemplatesScreen()
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
                                composable("data_review") { // Changed "review" to "data_review"
                                    val allegationsViewModel: com.hereliesaz.lexorcist.viewmodel.AllegationsViewModel = hiltViewModel()
                                    ReviewScreen(
                                        evidenceViewModel = evidenceViewModel,
                                        caseViewModel = caseViewModel,
                                        allegationsViewModel = allegationsViewModel
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
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center 
                ) {
                    CircularProgressIndicator() // ENSURED THIS IS CORRECT
                }
            }
            is SignInState.Idle, is SignInState.Error -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)){
                    val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2
                     Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp), 
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Top 
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

