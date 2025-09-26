package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.ui.AllegationsScreen
import com.hereliesaz.lexorcist.ui.CasesScreen
import com.hereliesaz.lexorcist.ui.CreateCaseDialog
import com.hereliesaz.lexorcist.ui.EvidenceDetailsScreen
import com.hereliesaz.lexorcist.ui.EvidenceScreen
import com.hereliesaz.lexorcist.ui.ExhibitsScreen
import com.hereliesaz.lexorcist.ui.ExtrasScreen
import com.hereliesaz.lexorcist.ui.PhotoGroupScreen
import com.hereliesaz.lexorcist.ui.ReviewScreen
import com.hereliesaz.lexorcist.ui.ScriptBuilderScreen
import com.hereliesaz.lexorcist.ui.SettingsScreen
import com.hereliesaz.lexorcist.ui.TemplatesScreen
import com.hereliesaz.lexorcist.ui.TimelineScreen
import com.hereliesaz.lexorcist.ui.components.CoinTossLoadingIndicator
import com.hereliesaz.lexorcist.ui.components.ScriptableAzNavRail
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.MasterAllegationsViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    caseViewModel: CaseViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
    scriptedMenuViewModel: ScriptedMenuViewModel,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    val signInState by authViewModel.signInState.collectAsState()
    val caseSpecificErrorMessage by caseViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    val isLoading by mainViewModel.isLoading.collectAsState()

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
                mainViewModel.hideLoading()
            }
            is SignInState.Idle -> {
                caseViewModel.clearCache()
            }
            is SignInState.InProgress -> {
                mainViewModel.showLoading()
            }
            else -> {
                // Do nothing for Error state
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val currentSignInState = signInState) {
                is SignInState.Success -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        ScriptableAzNavRail(
                            navController = navController,
                            scriptedMenuItems = scriptedMenuViewModel.menuItems,
                            onLogout = { authViewModel.signOut(mainViewModel) }
                        )

                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.weight(1f)
                        ) {
                            composable("home") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center,
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
                                    val createCaseText = stringResource(R.string.create_new_case)
                                    AzButton(
                                        text = createCaseText,
                                        onClick = { showCreateCaseDialog = true }
                                    )
                                }
                            }
                            composable("cases") { CasesScreen(caseViewModel = caseViewModel, navController = navController, mainViewModel = mainViewModel) }
                            composable("evidence") {
                                EvidenceScreen(
                                    navController = navController,
                                    caseViewModel = caseViewModel,
                                    mainViewModel = mainViewModel
                                )
                            }
                            composable("extras") { ExtrasScreen() }
                            composable("script_builder") {
                                ScriptBuilderScreen(navController = navController, caseViewModel = caseViewModel)
                            }
                            composable("case_allegations_route") {
                                AllegationsScreen(hiltViewModel<MasterAllegationsViewModel>())
                            }
                            composable("templates") {
                                TemplatesScreen(hiltViewModel<AddonsBrowserViewModel>())
                            }
                            composable("timeline") { TimelineScreen() }
                            composable("photo_group") {
                                PhotoGroupScreen(navController = navController, mainViewModel = mainViewModel)
                            }
                            composable("data_review") {
                                ReviewScreen(caseViewModel = caseViewModel)
                            }
                            composable("exhibits") {
                                ExhibitsScreen(caseViewModel = caseViewModel)
                            }
                            composable("settings") { SettingsScreen(caseViewModel = caseViewModel, mainViewModel = mainViewModel) }
                            composable("evidence_details/{evidenceId}") { backStackEntry ->
                                val evidenceIdString = backStackEntry.arguments?.getString("evidenceId")
                                val evidenceId = remember(evidenceIdString) { evidenceIdString?.toIntOrNull() }
                                val evidence = caseViewModel.selectedCaseEvidenceList.collectAsState().value.find { it.id == evidenceId }

                                if (evidence != null) {
                                    EvidenceDetailsScreen(
                                        evidence = evidence,
                                        caseViewModel = caseViewModel,
                                        navController = navController,
                                        mainViewModel = mainViewModel
                                    )
                                } else {
                                    Text("Error: Evidence not found.")
                                }
                            }
                            composable("transcription/{evidenceId}") { backStackEntry ->
                                val evidenceIdString = backStackEntry.arguments?.getString("evidenceId")
                                val evidenceId = remember(evidenceIdString) { evidenceIdString?.toIntOrNull() }
                                val evidence = caseViewModel.selectedCaseEvidenceList.collectAsState().value.find { it.id == evidenceId }

                                if (evidence != null) {
                                    com.hereliesaz.lexorcist.ui.TranscriptionScreen(
                                        evidence = evidence,
                                        caseViewModel = caseViewModel,
                                        navController = navController,
                                        mainViewModel = mainViewModel
                                    )
                                } else {
                                    Text("Error: Evidence not found.")
                                }
                            }
                            composable("video_evidence/{evidenceId}") { backStackEntry ->
                                val evidenceIdString = backStackEntry.arguments?.getString("evidenceId")
                                val evidenceId = remember(evidenceIdString) { evidenceIdString?.toIntOrNull() }
                                if (evidenceId != null) {
                                    com.hereliesaz.lexorcist.ui.VideoEvidenceScreen(
                                        navController = navController,
                                        caseViewModel = caseViewModel,
                                        evidenceId = evidenceId
                                    )
                                } else {
                                    Text("Error: Invalid evidence ID.")
                                }
                            }
                        }
                    }
                }
                is SignInState.InProgress -> {
                    // Handled by global isLoading
                }
                is SignInState.Idle, is SignInState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            val signInText = stringResource(R.string.sign_in_with_google)
                            AzButton(
                                text = signInText,
                                onClick = { onSignInClick() }
                            )
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

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CoinTossLoadingIndicator()
                }
            }

            if (showCreateCaseDialog) {
                CreateCaseDialog(
                    caseViewModel = caseViewModel,
                    navController = navController,
                    onDismiss = { showCreateCaseDialog = false }
                )
            }
        }
    }
}
