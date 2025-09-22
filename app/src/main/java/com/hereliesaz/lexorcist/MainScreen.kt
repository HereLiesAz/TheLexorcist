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
import com.hereliesaz.lexorcist.ui.components.CoinTossLoadingIndicator
// import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton // Removed import
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
import com.hereliesaz.lexorcist.ui.PhotoGroupScreen
// import com.hereliesaz.lexorcist.ui.ShareAddonScreen // Removed import
import com.hereliesaz.lexorcist.ui.TimelineScreen
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel // Corrected import
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
// import com.hereliesaz.lexorcist.viewmodel.ExtrasViewModel // ExtrasViewModel is used by hiltViewModel() directly
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.viewmodel.MasterAllegationsViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptBuilderViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptedMenuViewModel
import com.hereliesaz.lexorcist.ui.components.ScriptableAzNavRail

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
    val selectedCase by caseViewModel.selectedCase.collectAsState()
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
                caseViewModel.loadCasesFromRepository() // Updated
                mainViewModel.hideLoading()
            }
            is SignInState.Idle -> {
                caseViewModel.clearCache() // Updated
            }
            is SignInState.InProgress -> {
                mainViewModel.showLoading()
            }
            else -> {
                // Do nothing for Error state in this effect, already handled
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val currentSignInState = signInState) {
                is SignInState.Success -> {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    Row(
                        modifier =
                        Modifier
                            .fillMaxSize(),
                    ) {
                        ScriptableAzNavRail(
                            navController = navController,
                            scriptedMenuItems = scriptedMenuViewModel.menuItems,
                            onLogout = { authViewModel.signOut(mainViewModel) }
                        )

                        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                                            AzButton {
                                                onClick { showCreateCaseDialog = true }
                                                Text(stringResource(R.string.create_new_case))
                                            }
                                        }
                                    }
                                    composable("cases") { CasesScreen(caseViewModel = caseViewModel, navController = navController, mainViewModel = mainViewModel) }
                                    composable("evidence") {
                                        EvidenceScreen(
                                            navController = navController,
                                            caseViewModel = caseViewModel, // Pass the MainScreen's instance
                                            mainViewModel = mainViewModel
                                        )
                                    }
                                    composable("extras") {
                                        ExtrasScreen() // Removed onShare argument
                                    }
                                    // Removed "share_addon" route
                                    // Removed "share_addon_destination" route
                                    composable("script_builder") {
                                        ScriptBuilderScreen(viewModel = hiltViewModel<ScriptBuilderViewModel>(), navController = navController)
                                    }
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
                                                navController = navController
                                            )
                                        }
                                    }
                                    composable("photo_group") {
                                        PhotoGroupScreen(navController = navController, mainViewModel = mainViewModel)
                                    }
                                    composable("data_review") {
                                        ReviewScreen(caseViewModel = caseViewModel) // Updated
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
                                                mainViewModel = mainViewModel // Reinstated this line
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
                    }
                }
                is SignInState.InProgress -> {
                    // This state is now handled by the global isLoading state
                }
                is SignInState.Idle, is SignInState.Error -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                            AzButton {
                                onClick { onSignInClick() }
                                Text(stringResource(R.string.sign_in_with_google))
                            }
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
                    onDismiss = { showCreateCaseDialog = false } // Removed mainViewModel parameter
                )
            }
        }
    }
}
