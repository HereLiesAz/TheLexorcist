package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import java.util.Locale

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

    Scaffold() { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.End
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (selectedCase == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ){
                    Text(stringResource(R.string.please_select_case_for_evidence).uppercase(Locale.getDefault()))
                }
            } else if (evidenceList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ){
                    Text(stringResource(R.string.no_evidence_for_case).uppercase(Locale.getDefault()))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp) 
                ) {
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
        } // Closing brace for the Scaffold's main Column
     // } Closing brace for the Scaffold's content lambda

    // Dialogs are now correctly placed within the DataReviewScreen composable scope
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
            title = { Text(stringResource(R.string.delete_evidence).uppercase(Locale.getDefault())) },
            text = { Text(stringResource(R.string.delete_evidence_confirmation)) },
            confirmButton = {
                Button(onClick = {
                    evidenceViewModel.deleteEvidence(evidenceToDelete!!)
                    showDeleteConfirmDialog = false
                }) {
                    Text(stringResource(R.string.delete).uppercase(Locale.getDefault()))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(Locale.getDefault()))
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
            .padding(vertical = 4.dp) 
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
                 horizontalAlignment = Alignment.End, 
                 verticalArrangement = Arrangement.spacedBy(4.dp) 
            ) {
                Text(
                    text = evidence.sourceDocument,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth() 
                )
                if (evidence.category.isNotBlank()) {
                    Text(
                        text = "Category: ${evidence.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (evidence.tags.isNotEmpty()) {
                    Text(
                        text = "Tags: ${evidence.tags.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = evidence.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3, 
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(horizontalAlignment = Alignment.End) { 
                IconButton(onClick = { onEditClick(evidence) }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit).uppercase(Locale.getDefault()))
                }
                IconButton(onClick = { onDeleteClick(evidence) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete).uppercase(Locale.getDefault()))
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
        title = { Text(stringResource(R.string.edit_evidence).uppercase(Locale.getDefault())) }, 
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()), 
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) { 
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.content)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5 
                )
                 OutlinedTextField(
                    value = sourceDocument,
                    onValueChange = { sourceDocument = it },
                    label = { Text(stringResource(R.string.source_document)) }, 
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.category)) },
                    placeholder = { Text(stringResource(R.string.enter_category)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.tags_comma_separated)) }, 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val updatedEvidence = evidence.copy(
                    content = content,
                    sourceDocument = sourceDocument,
                    category = category,
                    tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
                onSave(updatedEvidence)
            }) {
                Text(stringResource(R.string.save).uppercase(Locale.getDefault())) 
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { 
                Text(stringResource(R.string.cancel).uppercase(Locale.getDefault())) 
            }
        }
    )
}

// Need to add these to strings.xml:
// R.string.data_review_title_case = "REVIEW: %1$s"
// R.string.please_select_case_for_evidence = "Please select a case to review its evidence."
// R.string.no_evidence_for_case = "No evidence found for this case."
// R.string.source_document = "Source Document"
// R.string.tags_comma_separated = "Tags (comma-separated)"
