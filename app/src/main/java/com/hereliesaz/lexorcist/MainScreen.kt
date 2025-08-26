package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lexorcist.components.AppNavRail
import com.hereliesaz.lexorcist.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onSignIn: () -> Unit,
    onSelectImage: () -> Unit
) {
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    var currentScreen by remember { mutableStateOf("home") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lexorcist") })
        }
    ) { paddingValues ->
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
                            onCreateMasterTemplate = {},
                            onCreateCase = {}
                        )
                        "cases" -> CasesScreen(viewModel = viewModel)
                        "add_evidence" -> AddEvidenceScreen(viewModel = viewModel, onSelectImage = onSelectImage)
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = onSignIn) {
                    Text("Sign in with Google")
                }
            }
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to The Lexorcist!",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Use the navigation rail on the left to get started.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
    }
}