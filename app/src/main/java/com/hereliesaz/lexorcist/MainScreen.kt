package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // Added import
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Corrected import
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hereliesaz.aznavrail.*
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.ui.AllegationsScreen
import com.hereliesaz.lexorcist.ui.CasesScreen
import com.hereliesaz.lexorcist.ui.CreateCaseDialog
import com.hereliesaz.lexorcist.ui.EvidenceDetailsScreen
import com.hereliesaz.lexorcist.ui.EvidenceScreen
import com.hereliesaz.lexorcist.ui.ExtrasScreen
import com.hereliesaz.lexorcist.ui.ReviewScreen
import com.hereliesaz.lexorcist.ui.ScriptBuilderScreen
import com.hereliesaz.lexorcist.ui.SettingsScreen
import com.hereliesaz.lexorcist.ui.TemplatesScreen
import com.hereliesaz.lexorcist.ui.TimelineScreen
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel // Corrected import
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.MasterAllegationsViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptBuilderViewModel
import com.hereliesaz.lexorcist.viewmodel.SettingsViewModel

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

    LaunchedEffect(signInState) {
        when (signInState) {
            is SignInState.Success -> {
                caseViewModel.loadCasesFromRepository()
            }
            is SignInState.Idle -> {
                caseViewModel.clearCache()
            }
            else -> {
                // Do nothing for InProgress or Error states in this effect
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when (val currentSignInState = signInState) {
            is SignInState.Success -> {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Row(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    AzNavRail {
                        azRailItem(id = "cases", text = "Cases", onClick = { navController.navigate("cases") })
                        azRailItem(
                            id = "evidence",
                            text = "Evidence",
                            onClick = {
                                selectedCase?.let {
                                    navController.navigate("evidence/${it.id}/${it.spreadsheetId}")
                                } ?: run {
                                    // Optionally, show a snackbar message
                                }
                            }
                        )
                        // Renamed item id and route for case-specific allegations
                        azRailItem(
                            id = "case_allegations_item",
                            text = "Allegations",
                            onClick = { navController.navigate("case_allegations_route") },
                        )
                        azRailItem(id = "templates", text = "Templates", onClick = { navController.navigate("templates") })
                        azRailItem(id = "script_builder", text = "Script Builder", onClick = { navController.navigate("script_builder") })
                        azRailItem(id = "data_review", text = "Review", onClick = { navController.navigate("data_review") })
                        azRailItem(id = "timeline", text = "Timeline", onClick = { navController.navigate("timeline") })
                        azMenuItem(id = "extras", text = "Extras", onClick = { navController.navigate("extras") })
                        azMenuItem(id = "settings", text = "Settings", onClick = { navController.navigate("settings") })
                        azMenuItem(id = "logout", text = "Logout", onClick = { authViewModel.signOut() })
                    }

                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight().padding(paddingValues)) {
                        val halfContentAreaHeight = this@BoxWithConstraints.maxHeight / 2
                        val contentAreaViewportHeight = this@BoxWithConstraints.maxHeight

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            if (currentRoute == "home") {
                                Spacer(Modifier.height(halfContentAreaHeight))
                            }

                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.height(contentAreaViewportHeight),
                            ) {
                                composable("home") {
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.Top, // Content aligns to its top
                                    ) {
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            style = MaterialTheme.typography.headlineMedium,
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = stringResource(R.string.use_navigation_rail),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.tap_icon_to_open_menu),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Spacer(modifier = Modifier.height(32.dp))
                                        LexorcistOutlinedButton(onClick = { showCreateCaseDialog = true }, text = stringResource(R.string.create_new_case))
                                    }
                                }
                                composable("cases") { CasesScreen(caseViewModel = caseViewModel, navController = navController) }
                                composable("evidence/{caseId}/{spreadsheetId}") { backStackEntry ->
                                    val caseId = backStackEntry.arguments?.getString("caseId")?.toLongOrNull()
                                    val spreadsheetId = backStackEntry.arguments?.getString("spreadsheetId")
                                    if (caseId != null && spreadsheetId != null) {
                                        EvidenceScreen(
                                            navController = navController,
                                            caseId = caseId,
                                            spreadsheetId = spreadsheetId
                                        )
                                    } else {
                                        // Handle error, e.g., show a message or navigate back
                                        Text("Error: Case ID or Spreadsheet ID not found.")
                                    }
                                }
                                composable("extras") { ExtrasScreen(viewModel = hiltViewModel<AddonsBrowserViewModel>(), onShare = {}) }
                                composable("script_builder") {
                                    ScriptBuilderScreen(viewModel = hiltViewModel<ScriptBuilderViewModel>(), navController = navController)
                                }
                                // Renamed route for case-specific allegations
                                composable("case_allegations_route") {
                                    AllegationsScreen(hiltViewModel<MasterAllegationsViewModel>())
                                }
                                composable("templates") {
                                    TemplatesScreen(hiltViewModel<AddonsBrowserViewModel>())
                                }
                                composable("timeline") {
                                    selectedCase?.let {
                                        TimelineScreen(
                                            caseViewModel = caseViewModel,
                                            navController = navController,
                                        )
                                    }
                                }
                                composable("data_review") {
                                    ReviewScreen(
                                        evidenceViewModel = evidenceViewModel,
                                        caseViewModel = caseViewModel,
                                    )
                                }
                                composable("settings") { SettingsScreen(viewModel = hiltViewModel<SettingsViewModel>(), caseViewModel = caseViewModel) }
                                composable("evidence_details/{evidenceId}") { backStackEntry ->
                                    val evidenceIdString = backStackEntry.arguments?.getString("evidenceId")
                                    val evidenceId = remember(evidenceIdString) { evidenceIdString?.toIntOrNull() }

                                    if (evidenceId != null) {
                                        LaunchedEffect(evidenceId) {
                                            evidenceViewModel.loadEvidenceById(evidenceId)
                                        }
                                        val evidence by evidenceViewModel.selectedEvidenceDetails.collectAsState()
                                        evidence?.let { ev ->
                                            EvidenceDetailsScreen(
                                                evidence = ev,
                                                viewModel = evidenceViewModel,
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
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is SignInState.Idle, is SignInState.Error -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Spacer(Modifier.height(halfScreenHeight))
                        LexorcistOutlinedButton(onClick = onSignInClick, text = stringResource(R.string.sign_in_with_google))
                        if (currentSignInState is SignInState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentSignInState.message,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        if (showCreateCaseDialog) {
            CreateCaseDialog(
                caseViewModel = caseViewModel,
                navController = navController,
                onDismiss = { showCreateCaseDialog = false },
            )
        }
    }
}
