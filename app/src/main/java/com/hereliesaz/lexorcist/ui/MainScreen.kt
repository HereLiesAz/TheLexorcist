package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.components.AppNavRail
// Ensure this is the only MainViewModel import and it's the correct one.
import com.hereliesaz.lexorcist.viewmodel.MainViewModel

@Composable
fun MainScreen(
    navController: NavController,
    mainViewModel: MainViewModel, // This should now correctly refer to the ViewModel in the .viewmodel package
    onSignInClick: () -> Unit,
    onExportClick: () -> Unit
) {
    // Corrected to use uiEvidenceList from the merged ViewModel
    val evidenceList by mainViewModel.uiEvidenceList.collectAsState()

    Scaffold { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize() // Fill the whole screen
                .padding(paddingValues)
        ) {
            AppNavRail(onNavigate = { navController.navigate(it) })
            Column( // Main Content Column
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End // Align children (Spacers, Button Column, LazyColumn) to the right
            ) {
                Spacer(modifier = Modifier.weight(1f)) // Pushes content down

                // Buttons in a Column, each on a new line, aligned to End
                Column(
                    modifier = Modifier.fillMaxWidth(), // Takes available width to allow its children to align End
                    horizontalAlignment = Alignment.End // Align buttons to the right
                ) {
                    Button(onClick = onSignInClick, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("Sign In")
                    }
                    Button(onClick = onExportClick, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("Export to Sheet")
                    }
                    Button(onClick = {
                        // Corrected to use processUiEvidenceForReview from the merged ViewModel
                        mainViewModel.processUiEvidenceForReview()
                        navController.navigate("data_review")
                    }, modifier = Modifier.padding(bottom = 16.dp)) { // Added padding below last button before list
                        Text("Review Data")
                    }
                }

                // Evidence list, items aligned to End
                LazyColumn(
                    modifier = Modifier
                        // No weight here, its height is intrinsic or constrained by parent Column's weighted spacers
                        .fillMaxWidth()
                        .padding(top = 16.dp), // Add space above the list if Buttons Column is directly above
                    horizontalAlignment = Alignment.End // Align items in LazyColumn to the right
                ) {
                    items(evidenceList) { evidence ->
                        Text(
                            text = evidence.content, // Assuming Evidence model has a 'content' property
                            modifier = Modifier
                                .fillMaxWidth() // Make text take full width to respect alignment
                                .padding(vertical = 4.dp) // Add padding to each text item
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f)) // Pushes content up
            }
        }
    }
}
