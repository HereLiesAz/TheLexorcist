package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataReviewScreen(
    evidenceViewModel: EvidenceViewModel,
    caseViewModel: CaseViewModel
) {
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val isLoading by evidenceViewModel.isLoading.collectAsState()

    LaunchedEffect(selectedCase) {
        selectedCase?.let {
            evidenceViewModel.loadEvidenceForCase(it.id.toLong(), it.spreadsheetId)
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var evidenceToEdit by remember { mutableStateOf<Evidence?>(null) }
    var evidenceToDelete by remember { mutableStateOf<Evidence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedCase != null) "Review: ${selectedCase!!.name}" else "Data Review") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedCase == null) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Please select a case to review its evidence.")
                }
            } else if (evidenceList.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No evidence found for this case.")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(evidenceList) { evidence ->
                        EvidenceItem(
                            evidence = evidence,
                            onEditClick = {
                                evidenceToEdit = it
                                showEditDialog = true
                            },
                            onDeleteClick = {
                                evidenceToDelete = it
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && evidenceToEdit != null) {
        EditEvidenceDialog(
            evidence = evidenceToEdit!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedEvidence ->
                evidenceViewModel.updateEvidence(updatedEvidence)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog && evidenceToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Evidence") },
            text = { Text("Are you sure you want to delete this evidence?") },
            confirmButton = {
                TextButton(onClick = {
                    evidenceViewModel.deleteEvidence(evidenceToDelete!!)
                    showDeleteConfirmDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EvidenceItem(
    evidence: Evidence, 
    onEditClick: (Evidence) -> Unit,
    onDeleteClick: (Evidence) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                 horizontalAlignment = Alignment.Start, 
                 verticalArrangement = Arrangement.spacedBy(4.dp) 
            ) {
                Text(
                    text = evidence.sourceDocument,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (evidence.category.isNotBlank()) {
                    Text(
                        text = "Category: ${evidence.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (evidence.tags.isNotEmpty()) {
                    Text(
                        text = "Tags: ${evidence.tags.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = evidence.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3, 
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) { 
                IconButton(onClick = { onEditClick(evidence) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { onDeleteClick(evidence) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun EditEvidenceDialog(
    evidence: Evidence, 
    onDismiss: () -> Unit,
    onSave: (Evidence) -> Unit
) {
    var content by remember { mutableStateOf(evidence.content) }
    var sourceDocument by remember { mutableStateOf(evidence.sourceDocument) }
    var category by remember { mutableStateOf(evidence.category) }
    var tags by remember { mutableStateOf(evidence.tags.joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Evidence") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), 
                   verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5 
                )
                 OutlinedTextField(
                    value = sourceDocument,
                    onValueChange = { sourceDocument = it },
                    label = { Text("Source Document") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("Enter a category for this evidence") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updatedEvidence = evidence.copy(
                    content = content,
                    sourceDocument = sourceDocument,
                    category = category,
                    tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
                onSave(updatedEvidence)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
