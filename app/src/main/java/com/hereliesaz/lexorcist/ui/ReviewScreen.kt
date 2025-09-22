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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType // Added import
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.AllegationsViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    caseViewModel: CaseViewModel = hiltViewModel(),
    allegationsViewModel: AllegationsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel,
) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val allegations by allegationsViewModel.allegations.collectAsState()
    val selectedAllegation by allegationsViewModel.selectedAllegation.collectAsState()
    val selectedEvidence by caseViewModel.selectedEvidence.collectAsState()
    val exhibits by caseViewModel.exhibits.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var selectedExhibit by remember { mutableStateOf<Exhibit?>(null) }

    LaunchedEffect(selectedCase) {
        selectedCase?.let {
            allegationsViewModel.loadAllegations(it.id.toString())
            caseViewModel.loadExhibits()
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var showGenerateDocumentDialog by remember { mutableStateOf(false) }
    var showPackageFilesDialog by remember { mutableStateOf(false) }
    var evidenceToEdit by remember { mutableStateOf<Evidence?>(null) }
    var evidenceToDelete by remember { mutableStateOf<Evidence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (
                            if (selectedCase != null) {
                                stringResource(R.string.data_review_title_case, selectedCase!!.name)
                            } else {
                                stringResource(R.string.data_review)
                            }
                            ).uppercase(Locale.getDefault()),
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
            if (selectedCase == null) {
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
            } else if (evidenceList.isEmpty()) {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.no_evidence_for_case).uppercase(Locale.getDefault()))
                }
            } else {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Allegations") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Exhibits") }
                    )
                }
                Row(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    if (selectedTab == 0) {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(allegations) { allegation ->
                                AllegationItem(
                                    allegation = allegation,
                                    isSelected = selectedAllegation?.id == allegation.id,
                                    onClick = { allegationsViewModel.onAllegationSelected(allegation) }
                                )
                            }
                        }
                    } else {
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
                                        onClick = { selectedExhibit = it }
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(evidenceList) { evidence ->
                            EvidenceItem(
                                evidence = evidence,
                                isSelected = selectedEvidence.any { it.id == evidence.id },
                                onClick = { caseViewModel.toggleEvidenceSelection(evidence.id) },
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
                    if (selectedAllegation != null && selectedEvidence.isNotEmpty()) {
                        LexorcistOutlinedButton(
                            onClick = {
                                caseViewModel.assignAllegationToSelectedEvidence(selectedAllegation!!.id, mainViewModel)
                            },
                            text = stringResource(R.string.assign_to_allegation),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (selectedExhibit != null && selectedEvidence.isNotEmpty()) {
                        LexorcistOutlinedButton(
                            onClick = {
                                caseViewModel.addEvidenceToExhibit(selectedExhibit!!.id, selectedEvidence.map { it.id })
                            },
                            text = "Add to Exhibit",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    LexorcistOutlinedButton(
                        onClick = { showGenerateDocumentDialog = true },
                        text = "Generate Document",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    LexorcistOutlinedButton(
                        onClick = { showPackageFilesDialog = true },
                        text = "Package Files",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

    if (showPackageFilesDialog) {
        PackageFilesDialog(
            caseViewModel = caseViewModel,
            onDismiss = { showPackageFilesDialog = false }
        )
    }

    if (showGenerateDocumentDialog) {
        GenerateDocumentDialog(
            caseViewModel = caseViewModel,
            onDismiss = { showGenerateDocumentDialog = false }
        )
    }

    if (showPackageFilesDialog) {
        PackageFilesDialog(
            caseViewModel = caseViewModel,
            onDismiss = { showPackageFilesDialog = false }
        )
    }

    if (showCleanupDialog) {
        CleanupDialog(
            caseViewModel = caseViewModel,
            mainViewModel = mainViewModel, // Added mainViewModel
            onDismiss = { showCleanupDialog = false }
        )
    }

    if (showEditDialog && evidenceToEdit != null) {
        EditEvidenceDialog(
            evidence = evidenceToEdit!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedEvidence ->
                caseViewModel.updateEvidence(updatedEvidence, mainViewModel)
                showEditDialog = false
            },
        )
    }

    if (showDeleteConfirmDialog && evidenceToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_evidence).uppercase(Locale.getDefault())) },
            text = { Text(stringResource(R.string.delete_evidence_confirmation)) },
            confirmButton = {
                LexorcistOutlinedButton(onClick = {
                    caseViewModel.deleteEvidence(evidenceToDelete!!, mainViewModel)
                    showDeleteConfirmDialog = false
                }, text = stringResource(R.string.delete).uppercase(Locale.getDefault()))
            },
            dismissButton = {
                LexorcistOutlinedButton(onClick = { showDeleteConfirmDialog = false }, text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
            },
        )
    }
}

@Composable
fun AllegationItem(
    allegation: Allegation,
    isSelected: Boolean,
    onClick: (Allegation) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .pointerInput(allegation) {
                detectTapGestures(onTap = { onClick(allegation) })
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
            Text(
                text = allegation.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EvidenceItem(
    evidence: Evidence,
    isSelected: Boolean,
    onClick: (Int) -> Unit,
    onEditClick: (Evidence) -> Unit,
    onDeleteClick: (Evidence) -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEvidenceDialog(
    evidence: Evidence,
    onDismiss: () -> Unit,
    onSave: (Evidence) -> Unit,
) {
    var content by remember { mutableStateOf(evidence.content) }
    var sourceDocument by remember { mutableStateOf(evidence.sourceDocument) }
    var category by remember { mutableStateOf(evidence.category) }
    var tags by remember { mutableStateOf(evidence.tags.joinToString(", ")) }
    val categories = listOf("Financial", "Communication", "Legal", "Personal", "Other")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_evidence).uppercase(Locale.getDefault())) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.content)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                )
                OutlinedTextField(
                    value = sourceDocument,
                    onValueChange = { sourceDocument = it },
                    label = { Text(stringResource(R.string.source_document)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(), // Updated line
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    category = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.tags_comma_separated)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            LexorcistOutlinedButton(onClick = {
                val updatedEvidence =
                    evidence.copy(
                        content = content,
                        sourceDocument = sourceDocument,
                        category = category,
                        tags = tags.split(", ").map { it.trim() }.filter { it.isNotEmpty() },
                    )
                onSave(updatedEvidence)
            }, text = stringResource(R.string.save).uppercase(Locale.getDefault()))
        },
        dismissButton = {
            LexorcistOutlinedButton(onClick = onDismiss, text = stringResource(R.string.cancel).uppercase(Locale.getDefault()))
        },
    )
}

@Composable
fun ExhibitItem(
    exhibit: Exhibit,
    isSelected: Boolean,
    onClick: (Exhibit) -> Unit
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
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
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
fun PackageFilesDialog(
    caseViewModel: CaseViewModel,
    onDismiss: () -> Unit
) {
    val case by caseViewModel.selectedCase.collectAsState()
    val files = remember(case) {
        case?.let {
            val caseDir = java.io.File(caseViewModel.storageLocation.value, it.spreadsheetId)
            caseDir.walk().filter { it.isFile }.toList()
        } ?: emptyList()
    }
    var selectedFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var packageName by remember { mutableStateOf("") }
    var extension by remember { mutableStateOf("zip") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Package Files") },
        text = {
            Column {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") }
                )
                Row {
                    RadioButton(
                        selected = extension == "zip",
                        onClick = { extension = "zip" }
                    )
                    Text("ZIP")
                    RadioButton(
                        selected = extension == "lex",
                        onClick = { extension = "lex" }
                    )
                    Text("LEX")
                }
                LazyColumn {
                    items(files) { file ->
                        Row {
                            Checkbox(
                                checked = selectedFiles.contains(file.absolutePath),
                                onCheckedChange = {
                                    selectedFiles = if (it) {
                                        selectedFiles + file.absolutePath
                                    } else {
                                        selectedFiles - file.absolutePath
                                    }
                                }
                            )
                            Text(file.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    caseViewModel.packageFiles(selectedFiles.map { java.io.File(it) }, packageName, extension)
                    onDismiss()
                },
                enabled = selectedFiles.isNotEmpty() && packageName.isNotBlank()
            ) {
                Text("Package")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateDocumentDialog(
    caseViewModel: CaseViewModel,
    onDismiss: () -> Unit
) {
    val exhibits by caseViewModel.exhibits.collectAsState()
    val templates by caseViewModel.htmlTemplates.collectAsState()
    var selectedExhibit by remember { mutableStateOf<Exhibit?>(null) }
    var selectedTemplate by remember { mutableStateOf<com.google.api.services.drive.model.File?>(null) }
    var exhibitExpanded by remember { mutableStateOf(false) }
    var templateExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Document") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = exhibitExpanded,
                    onExpandedChange = { exhibitExpanded = !exhibitExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedExhibit?.name ?: "Select Exhibit",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exhibitExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = exhibitExpanded,
                        onDismissRequest = { exhibitExpanded = false }
                    ) {
                        exhibits.forEach { exhibit ->
                            DropdownMenuItem(
                                text = { Text(exhibit.name) },
                                onClick = {
                                    selectedExhibit = exhibit
                                    exhibitExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = templateExpanded,
                    onExpandedChange = { templateExpanded = !templateExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTemplate?.name ?: "Select Template",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = templateExpanded,
                        onDismissRequest = { templateExpanded = false }
                    ) {
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    selectedTemplate = template
                                    templateExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedExhibit?.let { exhibit ->
                        selectedTemplate?.let { template ->
                            caseViewModel.generateDocument(exhibit, template)
                        }
                    }
                    onDismiss()
                },
                enabled = selectedExhibit != null && selectedTemplate != null
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
