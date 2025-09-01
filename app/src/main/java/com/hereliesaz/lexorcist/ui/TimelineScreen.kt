package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    case: Case,
    evidenceViewModel: EvidenceViewModel = viewModel(),
    navController: NavController
) {
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()
    val searchQuery by evidenceViewModel.searchQuery.collectAsState()
    var showEvidenceDetailsDialog by remember { mutableStateOf<Evidence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = case.name) },
                actions = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { evidenceViewModel.onSearchQueryChanged(it) },
                        label = { Text("Search") },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(evidenceList) { evidence ->
                EvidenceCard(
                    evidence = evidence,
                    onClick = { showEvidenceDetailsDialog = evidence }
                )
            }
        }
    }

    showEvidenceDetailsDialog?.let { evidence ->
        EvidenceDetailsDialog(
            evidence = evidence,
            onDismiss = { showEvidenceDetailsDialog = null },
            onNavigateToEvidenceDetails = {
                navController.navigate("evidence_details/${evidence.id}")
            }
        )
    }
}

@Composable
fun EvidenceCard(
    evidence: Evidence,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = evidence.content, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = evidence.category, style = MaterialTheme.typography.bodySmall)
                Text(text = evidence.documentDate.toString(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun EvidenceDetailsDialog(
    evidence: Evidence,
    onDismiss: () -> Unit,
    onNavigateToEvidenceDetails: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Evidence Details") },
        text = {
            Column {
                Text(text = "Content: ${evidence.content}")
                Text(text = "Category: ${evidence.category}")
                Text(text = "Source: ${evidence.sourceDocument}")
                Text(text = "Tags: ${evidence.tags.joinToString()}")
            }
        },
        confirmButton = {
            Button(onClick = {
                onNavigateToEvidenceDetails()
                onDismiss()
            }) {
                Text("Go to Details")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
