package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.AllegationElement
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.AllegationsViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExhibitsScreen(
    caseViewModel: CaseViewModel = hiltViewModel(),
    allegationsViewModel: AllegationsViewModel = hiltViewModel(),
    navController: NavController
) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val allegations by allegationsViewModel.allegations.collectAsState()
    val selectedAllegation by allegationsViewModel.selectedAllegation.collectAsState()
    val selectedEvidence by caseViewModel.selectedEvidence.collectAsState()
    val exhibits by caseViewModel.exhibits.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var evidenceToEdit by remember { mutableStateOf<Evidence?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var evidenceToDelete by remember { mutableStateOf<Evidence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exhibits)) }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(allegations) { allegation ->
                    AllegationItem(
                        allegation = allegation,
                        isSelected = selectedAllegation?.id == allegation.id,
                        onClick = { allegationsViewModel.onAllegationSelected(allegation) }
                    )
                }
            }
            if (selectedAllegation != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Elements for ${selectedAllegation!!.text}", style = MaterialTheme.typography.titleMedium)
                    selectedAllegation!!.elements.forEach { element ->
                        AllegationElementItem(
                            element = element,
                            onAssignEvidence = {
                                if (selectedEvidence.isNotEmpty()) {
                                    caseViewModel.assignEvidenceToElement(selectedAllegation!!.id.toString(), element.name, selectedEvidence.map { it.id })
                                }
                            }
                        )
                    }
                    CaseStrengthMeter(
                        evidenceList = evidenceList,
                        allegation = selectedAllegation!!
                    )
                }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(evidenceList) { evidence ->
                    EvidenceItem(
                        evidence = evidence,
                        isSelected = selectedEvidence.any { it.id == evidence.id },
                        allegationText = allegations.find { it.id.toString() == evidence.allegationId }?.text,
                        allegationElementName = evidence.allegationElementName,
                        exhibitNames = exhibits.filter { it.evidenceIds.contains(evidence.id) }.joinToString { it.name },
                        onClick = { caseViewModel.toggleEvidenceSelection(evidence.id) },
                        onEditClick = {
                            evidenceToEdit = evidence
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            evidenceToDelete = evidence
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
                caseViewModel.updateEvidence(updatedEvidence)
                showEditDialog = false
            },
        )
    }

    if (showDeleteConfirmDialog && evidenceToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_evidence)) },
            text = { Text(stringResource(R.string.delete_evidence_confirmation)) },
            confirmButton = {
                Button(onClick = {
                    caseViewModel.deleteEvidence(evidenceToDelete!!)
                    showDeleteConfirmDialog = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun CaseStrengthMeter(
    evidenceList: List<Evidence>,
    allegation: Allegation
) {
    Column {
        Text("Case Strength", style = MaterialTheme.typography.titleMedium)
        allegation.elements.forEach { element ->
            val evidenceCount = evidenceList.count { it.allegationElementName == element.name }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(element.name, modifier = Modifier.weight(1f))
                Row(modifier = Modifier.weight(1f)) {
                    (0 until evidenceCount).forEach {
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(20.dp)
                                .background(Color.Green)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AllegationElementItem(
    element: AllegationElement,
    onAssignEvidence: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(element.name, style = MaterialTheme.typography.titleMedium)
            Text(element.description, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAssignEvidence) {
                Text("Assign Selected Evidence")
            }
        }
    }
}

@Composable
fun AllegationItem(
    allegation: Allegation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = allegation.text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EvidenceItem(
    evidence: Evidence,
    isSelected: Boolean,
    allegationText: String?,
    allegationElementName: String?,
    exhibitNames: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(evidence.content, style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            allegationText?.let { Text("Allegation: $it", style = MaterialTheme.typography.bodySmall) }
            allegationElementName?.let { Text("Element: $it", style = MaterialTheme.typography.bodySmall) }
            if (exhibitNames.isNotEmpty()) {
                Text("Exhibits: $exhibitNames", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEvidenceDialog(
    evidence: Evidence,
    onDismiss: () -> Unit,
    onSave: (Evidence) -> Unit
) {
    var updatedContent by remember(evidence) { mutableStateOf(evidence.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Evidence") },
        text = {
            OutlinedTextField(
                value = updatedContent,
                onValueChange = { updatedContent = it },
                label = { Text("Evidence Content") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(evidence.copy(content = updatedContent))
                    onDismiss()
                }
            ) {
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