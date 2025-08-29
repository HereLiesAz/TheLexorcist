package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataReviewScreen(
    viewModel: MainViewModel
) {
    val evidenceList by viewModel.selectedCaseEvidenceList.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var evidenceToEdit by remember { mutableStateOf<Evidence?>(null) }
    var evidenceToDelete by remember { mutableStateOf<Evidence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Review") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
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

    if (showEditDialog && evidenceToEdit != null) {
        EditEvidenceDialog(
            evidence = evidenceToEdit!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedEvidence ->
                viewModel.updateEvidence(updatedEvidence)
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
                Button(onClick = {
                    viewModel.deleteEvidence(evidenceToDelete!!)
                    showDeleteConfirmDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) {
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
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = evidence.content,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    if (evidence.category.isNotBlank()) {
                        Text(
                            text = evidence.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = evidence.tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
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
    var tags by remember { mutableStateOf(evidence.tags.joinToString(",")) }
    var category by remember { mutableStateOf(evidence.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Evidence") },
        text = {
            Column(horizontalAlignment = Alignment.End) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    placeholder = { Text("Enter the evidence content") }
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") },
                    placeholder = { Text("Enter tags, separated by commas") }
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("Enter a category for this evidence") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedEvidence = evidence.copy(
                    content = content,
                    tags = tags.split(",").map { it.trim() },
                    category = category
                )
                onSave(updatedEvidence)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}