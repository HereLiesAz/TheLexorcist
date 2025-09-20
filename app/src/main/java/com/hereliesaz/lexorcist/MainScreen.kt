package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.hereliesaz.aznavrail.*
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.ui.*
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.ui.components.ScriptableAzNavRail
import com.hereliesaz.lexorcist.viewmodel.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    caseViewModel: CaseViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
    scriptedMenuViewModel: ScriptedMenuViewModel = hiltViewModel(),
    onSignOutClick: () -> Unit,
) {
    val signInState by authViewModel.signInState.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val caseSpecificErrorMessage by caseViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var screenTitle by remember { mutableStateOf("The Lexorcist") }

    // --- Event Handling ---

    // Handle navigation events from the ScriptedMenuViewModel
    LaunchedEffect(Unit) {
        scriptedMenuViewModel.events.collect { event ->
            when (event) {
                is ScriptedMenuEvent.NavigateToScreen -> {
                    val encodedJson = URLEncoder.encode(event.screenJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("scripted_screen/$encodedJson")
                }
                is ScriptedMenuEvent.ExecuteJs -> {
                    // This assumes MainViewModel has a way to run JS code.
                    // We will need to implement this.
                    // mainViewModel.runJs(event.functionName)
                }
            }
        }
    }

    LaunchedEffect(caseSpecificErrorMessage) {
        caseSpecificErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            caseViewModel.clearError()
        }
    }

    LaunchedEffect(signInState) {
        if (signInState is SignInState.Error) {
            snackbarHostState.showSnackbar("Sign-In Error: ${(signInState as SignInState.Error).message}")
        } else if (signInState is SignInState.Success) {
            caseViewModel.loadCasesFromRepository()
        } else if (signInState is SignInState.Idle) {
            caseViewModel.clearCache()
        }
    }

    // --- UI ---

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    if (navBackStackEntry?.destination?.route != "home") {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (signInState) {
            is SignInState.Success -> {
                val scriptedMenuItems by scriptedMenuViewModel.menuItems.collectAsState()

                Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    ScriptableAzNavRail(
                        navController = navController,
                        scriptedMenuItems = scriptedMenuItems,
                        onScriptedMenuItemClick = { item -> scriptedMenuViewModel.onMenuItemClicked(item) },
                        onLogout = {
                            authViewModel.signOut()
                            onSignOutClick()
                        }
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(navController = navController, startDestination = "home") {
                            // --- Standard Routes ---
                            composable("home") {
                                LaunchedEffect(Unit) { screenTitle = "The Lexorcist" }
                                Column(
                                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Top,
                                ) {
                                    Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = stringResource(R.string.use_navigation_rail), style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.height(32.dp))
                                    LexorcistOutlinedButton(onClick = { showCreateCaseDialog = true }, text = stringResource(R.string.create_new_case))
                                }
                            }
                            composable("cases") { LaunchedEffect(Unit) { screenTitle = "Cases" }; CasesScreen(caseViewModel, navController) }
                            composable("evidence") { LaunchedEffect(Unit) { screenTitle = "Evidence" }; EvidenceScreen(navController, caseViewModel) }
                            composable("extras") { LaunchedEffect(Unit) { screenTitle = "Extras" }; ExtrasScreen { navController.navigate("share_addon") } }
                            composable("share_addon") { LaunchedEffect(Unit) { screenTitle = "Share Addon" }; ShareAddonScreen(hiltViewModel(), navController) }
                            composable("script_builder") { LaunchedEffect(Unit) { screenTitle = "Script Builder" }; ScriptBuilderScreen(hiltViewModel(), navController) }
                            composable("case_allegations_route") { LaunchedEffect(Unit) { screenTitle = "Allegations" }; AllegationsScreen(hiltViewModel()) }
                            composable("templates") { LaunchedEffect(Unit) { screenTitle = "Templates" }; TemplatesScreen(hiltViewModel()) }
                            composable("timeline") { LaunchedEffect(Unit) { screenTitle = "Timeline" }; selectedCase?.let { TimelineScreen(caseViewModel, navController) } }
                            composable("data_review") { LaunchedEffect(Unit) { screenTitle = "Review" }; ReviewScreen(caseViewModel) }
                            composable("settings") { LaunchedEffect(Unit) { screenTitle = "Settings" }; SettingsScreen(caseViewModel) }

                            // --- Routes with Arguments ---
                            composable("evidence_details/{evidenceId}") { /* ... existing code ... */ }

                            // --- New Scripted Screen Route ---
                            composable(
                                "scripted_screen/{screenJson}",
                                arguments = listOf(navArgument("screenJson") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val json = backStackEntry.arguments?.getString("screenJson") ?: ""
                                val decodedJson = remember(json) { URLDecoder.decode(json, StandardCharsets.UTF_8.toString()) }

                                ScriptedScreen(
                                    screenJson = decodedJson,
                                    onJsAction = { functionName -> /* mainViewModel.runJs(functionName) */ },
                                    onTitleResolved = { title -> screenTitle = title }
                                )
                            }
                        }
                    }
                }
            }
            is SignInState.InProgress -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SignInState.Idle, is SignInState.Error -> {
                // --- Sign-In Screen ---
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    val halfScreenHeight = this.maxHeight / 2
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Spacer(Modifier.height(halfScreenHeight))
                        LexorcistOutlinedButton(onClick = { authViewModel.startSignIn() }, text = stringResource(R.string.sign_in_with_google))
                        if (signInState is SignInState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = (signInState as SignInState.Error).message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (showCreateCaseDialog) {
            CreateCaseDialog(caseViewModel, navController) { showCreateCaseDialog = false }
        }
    }
}
