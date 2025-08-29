package com.hereliesaz.lexorcist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lexorcist.components.AppNavRail
// Corrected import for the MainViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    // Explicitly type viewModel to the one from .viewmodel package
    viewModel: com.hereliesaz.lexorcist.viewmodel.MainViewModel = viewModel(),
    onSignIn: () -> Unit,
    onSelectImage: () -> Unit,
    onTakePicture: () -> Unit,
    onAddDocument: () -> Unit,
    onAddSpreadsheet: () -> Unit
) {
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    var currentScreen by remember { mutableStateOf("home") }

    Scaffold { paddingValues ->
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
                        "add_evidence" -> AddEvidenceScreen(
                            viewModel = viewModel,
                            onSelectImage = onSelectImage,
                            onTakePicture = onTakePicture,
                            onAddTextEvidence = { currentScreen = "add_text_evidence" },
                            onAddDocument = onAddDocument,
                            onAddSpreadsheet = onAddSpreadsheet
                        )
                        "add_text_evidence" -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            AddTextEvidenceScreen(viewModel = viewModel, onSave = { text ->
                                // Corrected method call to the one in the merged ViewModel
                                viewModel.addTextEvidenceToSelectedCase(text, context)
                                currentScreen = "cases"
                            })
                        }
                        "timeline" -> TimelineScreen(viewModel = viewModel)
                        // Now viewModel is of the correct type for SettingsScreen
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
                horizontalAlignment = Alignment.End, // Changed to End
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
        horizontalAlignment = Alignment.End, // Changed to End
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "The Lexorcist",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Use the navigation rail to get started." ,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Tap the icon at the top to open the menu.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
    }
}
