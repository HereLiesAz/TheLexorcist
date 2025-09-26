package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExhibitsScreen(
    caseViewModel: CaseViewModel = hiltViewModel(),
) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val isLoading by caseViewModel.isLoading.collectAsState()
    val selectedEvidence by caseViewModel.selectedEvidence.collectAsState()
    val exhibits by caseViewModel.exhibits.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var selectedExhibit by remember { mutableStateOf<Exhibit?>(null) }

    LaunchedEffect(selectedCase) {
        selectedCase?.let {
            caseViewModel.loadExhibits()
        }
    }

    var showCleanupDialog by remember { mutableStateOf(false) }
    var showEditExhibitDialog by remember { mutableStateOf(false) }
    var exhibitToEdit by remember { mutableStateOf<Exhibit?>(null) }
    var showDeleteExhibitDialog by remember { mutableStateOf(false) }
    var exhibitToDelete by remember { mutableStateOf<Exhibit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.exhibits).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator()
                }
            } else if (selectedCase == null) {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.please_select_case_for_evidence).uppercase(Locale.getDefault()))
                }
            } else {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Assign") }
                    )
                }

                if (selectedTab == 0) {
                    Row(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        var showCreateExhibitDialog by remember { mutableStateOf(false) }

                        Column(modifier = Modifier.weight(1f)) {
                            Button(onClick = { showCreateExhibitDialog = true }) {
                                Text("Create Exhibit")
                            }
                            LazyColumn {
                                items(exhibits) { exhibit ->
                                    ExhibitItem(
                                        exhibit = exhibit,
                                        isSelected = selectedExhibit?.id == exhibit.id,
                                        onClick = { selectedExhibit = it },
                                        onEditClick = {
                                            exhibitToEdit = it
                                            showEditExhibitDialog = true
                                        },
                                        onDeleteClick = {
                                            exhibitToDelete = it
                                            showDeleteExhibitDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        if (showCreateExhibitDialog) {
                            CreateExhibitDialog(
                                onDismiss = { showCreateExhibitDialog = false },
                                onConfirm = { name, description ->
                                    caseViewModel.addExhibit(name, description)
                                    showCreateExhibitDialog = false
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(evidenceList.filter { it.exhibitId == null }) { evidence ->
                                EvidenceItem(
                                    evidence = evidence,
                                    isSelected = selectedEvidence.any { it.id == evidence.id },
                                    onClick = { caseViewModel.toggleEvidenceSelection(evidence.id) },
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    LexorcistOutlinedButton(
                        onClick = {
                            caseViewModel.generateCleanupSuggestions()
                            showCleanupDialog = true
                        },
                        text = "Clean Up"
                    )
                    if (selectedExhibit != null && selectedEvidence.isNotEmpty()) {
                        LexorcistOutlinedButton(
                            onClick = {
                                caseViewModel.addEvidenceToExhibit(selectedExhibit!!.id, selectedEvidence.map { it.id })
                            },
                            text = "Add to Exhibit",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCleanupDialog) {
        CleanupDialog(
            caseViewModel = caseViewModel,
            onDismiss = { showCleanupDialog = false }
        )
    }

    if (showEditExhibitDialog && exhibitToEdit != null) {
        EditExhibitDialog(
            exhibit = exhibitToEdit!!,
            onDismiss = { showEditExhibitDialog = false },
            onConfirm = { name, description ->
                val updatedExhibit = exhibitToEdit!!.copy(name = name, description = description)
                caseViewModel.updateExhibit(updatedExhibit)
                showEditExhibitDialog = false
            }
        )
    }

    if (showDeleteExhibitDialog && exhibitToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteExhibitDialog = false },
            title = { Text("Delete Exhibit") },
            text = { Text("Are you sure you want to delete this exhibit?") },
            confirmButton = {
                Button(onClick = {
                    caseViewModel.deleteExhibit(exhibitToDelete!!)
                    showDeleteExhibitDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteExhibitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExhibitItem(
    exhibit: Exhibit,
    isSelected: Boolean,
    onClick: (Exhibit) -> Unit,
    onEditClick: (Exhibit) -> Unit,
    onDeleteClick: (Exhibit) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .pointerInput(exhibit) {
                detectTapGestures(onTap = { onClick(exhibit) })
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exhibit.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = exhibit.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column {
                IconButton(onClick = { onEditClick(exhibit) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Exhibit")
                }
                IconButton(onClick = { onDeleteClick(exhibit) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Exhibit")
                }
            }
        }
    }
}

@Composable
fun CreateExhibitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Exhibit") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exhibit Name") }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, description)
                    onDismiss()
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditExhibitDialog(
    exhibit: Exhibit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(exhibit.name) }
    var description by remember { mutableStateOf(exhibit.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Exhibit") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exhibit Name") }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, description)
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

@Composable
fun EvidenceItem(
    evidence: Evidence,
    isSelected: Boolean,
    onClick: (Int) -> Unit,
) {
    Card(
        modifier =
        Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .pointerInput(evidence) {
                detectTapGestures(onTap = { onClick(evidence.id) })
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier =
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = evidence.sourceDocument,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (evidence.category.isNotBlank()) {
                    Text(
                        text = "Category: ${evidence.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (evidence.tags.isNotEmpty()) {
                    Text(
                        text = "Tags: ${evidence.tags.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = evidence.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}