package com.hereliesaz.lexorcist.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.CleanupSuggestion
import com.hereliesaz.lexorcist.model.DisplayExhibit
import com.hereliesaz.lexorcist.ui.components.DragAndDropContainer
import com.hereliesaz.lexorcist.ui.components.DraggableItem
import com.hereliesaz.lexorcist.ui.components.DropTarget
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExhibitsScreen(
    caseViewModel: CaseViewModel = hiltViewModel()
) {
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val isLoading by caseViewModel.isLoading.collectAsState()
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("View", "Organize", "Assign")

    LaunchedEffect(selectedCase) {
        selectedCase?.let {
            caseViewModel.loadExhibits()
        }
    }

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
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AzLoad()
                    }
                } else if (selectedCase == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.please_select_case_for_exhibits).uppercase(Locale.getDefault()))
                    }
                } else {
                    when (tabIndex) {
                        0 -> ViewTab(caseViewModel)
                        1 -> CleanUpTab(caseViewModel)
                        2 -> AssignTab(caseViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ViewTab(caseViewModel: CaseViewModel) {
    val displayExhibits by caseViewModel.displayExhibits.collectAsState()
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val selectedExhibitForDetails by caseViewModel.selectedExhibit.collectAsState()

    val sortedExhibits = remember(displayExhibits) {
        displayExhibits.sortedByDescending { it.caseExhibit != null }
    }

    if (sortedExhibits.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.no_exhibits_for_case).uppercase(Locale.getDefault()))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
            items(sortedExhibits, key = { it.catalogItem.id }) { displayExhibit ->
                ExhibitItem(
                    displayExhibit = displayExhibit,
                    onClick = { caseViewModel.selectExhibit(displayExhibit) },
                    onDeleteClick = {
                        displayExhibit.caseExhibit?.let {
                            caseViewModel.deleteExhibit(it)
                        }
                    }
                )
            }
        }
    }

    selectedExhibitForDetails?.let { exhibit ->
        val exhibitEvidence = remember(evidenceList, exhibit) {
            evidenceList.filter { ev ->
                exhibit.caseExhibit?.evidenceIds?.contains(ev.id) == true
            }
        }
        ExhibitDetailsDialog(
            displayExhibit = exhibit,
            evidenceList = exhibitEvidence,
            onDismiss = { caseViewModel.selectExhibit(null) },
            onRemoveEvidence = { evidenceId ->
                exhibit.caseExhibit?.let {
                    caseViewModel.removeEvidenceFromExhibit(it.id, evidenceId)
                }
            }
        )
    }
}

@Composable
fun ExhibitItem(
    displayExhibit: DisplayExhibit,
    onClick: (DisplayExhibit) -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier =
        Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = { onClick(displayExhibit) }
    ) {
        Row(
            modifier =
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayExhibit.catalogItem.type,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
                Text(
                    text = displayExhibit.catalogItem.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
            if (displayExhibit.caseExhibit != null) {
                AzButton(onClick = onDeleteClick, text = "Del")
            }
        }
    }
}

@Composable
fun ExhibitDetailsDialog(
    displayExhibit: DisplayExhibit,
    evidenceList: List<Evidence>,
    onDismiss: () -> Unit,
    onRemoveEvidence: (Int) -> Unit
) {
    com.hereliesaz.lexorcist.ui.components.AzAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                displayExhibit.catalogItem.type,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    displayExhibit.catalogItem.description,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Contained Evidence:",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium
                )
                if (evidenceList.isEmpty()) {
                    Text(
                        "This exhibit is empty.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn {
                        items(evidenceList, key = { it.id }) { evidence ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = evidence.content.take(80),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(onClick = { onRemoveEvidence(evidence.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Evidence")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AzButton(onClick = onDismiss, text = "Close")
        },
        dismissButton = {}
    )
}

@Composable
fun CleanUpTab(caseViewModel: CaseViewModel) {
    val cleanupSuggestions by caseViewModel.cleanupSuggestions.collectAsState()
    val isScanning by caseViewModel.isScanningForCleanup.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isScanning) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                AzLoad()
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else {
            AzButton(
                onClick = { caseViewModel.generateCleanupSuggestions() },
                text = "Scan"
            )
        }

        if (cleanupSuggestions.isNotEmpty() && !isScanning) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(cleanupSuggestions) { suggestion ->
                    when (suggestion) {
                        is CleanupSuggestion.DuplicateGroup -> {
                            DuplicateGroupItem(
                                group = suggestion,
                                onMerge = { caseViewModel.deleteDuplicates(suggestion) }
                            )
                        }
                        is CleanupSuggestion.ImageSeriesGroup -> {
                            ImageSeriesGroupItem(
                                group = suggestion,
                                onMerge = { caseViewModel.mergeImageSeries(suggestion, "Merged Series") }
                            )
                        }
                    }
                }
            }
        } else if (!isScanning) {
            Text(
                "No cleanup suggestions. Run a scan to find duplicates or image series.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}

@Composable
fun DuplicateGroupItem(group: CleanupSuggestion.DuplicateGroup, onMerge: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Duplicate Group Found (${group.evidence.size} items)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
            group.evidence.forEach {
                Text(
                    " - Evidence ID: ${it.id}, Content: ${it.content.take(50)}...",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AzButton(onClick = onMerge, text = "Merge (Keep First, Delete Others)")
        }
    }
}

@Composable
fun ImageSeriesGroupItem(group: CleanupSuggestion.ImageSeriesGroup, onMerge: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Image Series Found (${group.evidence.size} items)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
            group.evidence.forEach {
                Text(
                    " - Evidence ID: ${it.id}, Source: ${it.sourceDocument}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AzButton(onClick = onMerge, text = "Combine into PDF")
        }
    }
}

@Composable
fun AssignTab(
    caseViewModel: CaseViewModel
) {
    val displayExhibits by caseViewModel.displayExhibits.collectAsState()
    val pertinentExhibits = remember(displayExhibits) { displayExhibits.map { it.catalogItem } }
    val allEvidence by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val exhibits by caseViewModel.exhibits.collectAsState()

    val assignedEvidenceIds = remember(exhibits) { exhibits.flatMap { it.evidenceIds }.toSet() }
    val unassignedEvidence = remember(allEvidence, assignedEvidenceIds) {
        allEvidence.filter { it.id !in assignedEvidenceIds }
    }

    DragAndDropContainer<Evidence> {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "Pertinent Exhibits",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.End
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    items(pertinentExhibits, key = { it.id }) { exhibit ->
                        DropTarget<Evidence>(
                            key = exhibit.id,
                            onDropped = { evidence ->
                                caseViewModel.assignEvidenceToDynamicExhibit(evidence.id, exhibit.type)
                            }
                        ) { isHovered ->
                           ExhibitTypeDisplayItem(exhibit.type, isHovered)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "Unassigned Evidence",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.End
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    items(unassignedEvidence, key = { it.id }) { evidence ->
                        DraggableItem(dataToDrop = evidence) {
                            EvidenceDisplayItem(evidence)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExhibitTypeDisplayItem(exhibitType: String, isHovered: Boolean) {
    val color = if (isHovered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = exhibitType,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun EvidenceDisplayItem(evidence: Evidence) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Type: ${evidence.type}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End
            )
            Text(
                text = evidence.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}