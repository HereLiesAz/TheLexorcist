package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    case: Case,
    evidenceViewModel: EvidenceViewModel = viewModel(),
    navController: NavController,
) {
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()
    val searchQuery by evidenceViewModel.searchQuery.collectAsState()
    var showEvidenceDetailsDialog by remember { mutableStateOf<Evidence?>(null) }

    LaunchedEffect(case) {
        evidenceViewModel.loadEvidenceForCase(case.id.toLong(), case.spreadsheetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.timeline).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

            val isLoading by evidenceViewModel.isLoading.collectAsState()

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (evidenceList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("No evidence found for this case.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(), // LazyColumn fills the BoxWithConstraints
                    contentPadding =
                        PaddingValues(
                            top = halfScreenHeight,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(evidenceList) { evidence ->
                        EvidenceCard(
                            evidence = evidence,
                            onClick = { showEvidenceDetailsDialog = evidence },
                        )
                    }
                }
            }
        }
    }

    showEvidenceDetailsDialog?.let { evidence ->
        EvidenceDetailsDialog(
            evidence = evidence,
            onDismiss = { showEvidenceDetailsDialog = null },
            onNavigateToEvidenceDetails = {
                // Ensure evidence.id is a non-null string if your route expects that.
                // If evidence.id is Long, it should be passed as such if the navigation graph handles it.
                navController.navigate("evidence_details/${evidence.id}")
                showEvidenceDetailsDialog = null // Dismiss dialog after initiating navigation
            },
        )
    }
}

@Composable
fun EvidenceCard(
    evidence: Evidence,
    onClick: () -> Unit,
) {
    val isPlaceholder = evidence.id < 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (!isPlaceholder) onClick() },
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(), // Fill width for internal alignment
            horizontalAlignment = Alignment.End, // Right-align content within the Card
        ) {
            Text(
                text = evidence.content,
                style = if (isPlaceholder) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = if (isPlaceholder) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            )
            if (!isPlaceholder) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = evidence.category, style = MaterialTheme.typography.bodySmall)
                    Text(text = evidence.documentDate.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun EvidenceDetailsDialog(
    evidence: Evidence,
    onDismiss: () -> Unit,
    onNavigateToEvidenceDetails: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Evidence Details") },
        text = {
            Column(horizontalAlignment = Alignment.End) {
                // Right-align content in the dialog text area
                Text(text = "Content: ${evidence.content}")
                Text(text = "Category: ${evidence.category}")
                Text(text = "Source: ${evidence.sourceDocument}")
                Text(text = "Tags: ${evidence.tags.joinToString()}")
            }
        },
        confirmButton = {
            OutlinedButton(onClick = {
                onNavigateToEvidenceDetails()
                // onDismiss() // Already called after navigation
            }) {
                Text("Go to Details")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
