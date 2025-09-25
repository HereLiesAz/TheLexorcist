package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceDetailsViewModel
import com.hereliesaz.lexorcist.viewmodel.TimelineSortType
import com.pushpal.jetlime.ItemsList
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeDefaults
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.JetLimeExtendedEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hereliesaz.lexorcist.data.Exhibit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    caseViewModel: CaseViewModel,
    navController: NavController,
    evidenceDetailsViewModel: EvidenceDetailsViewModel = hiltViewModel()
) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    var showEvidenceDetailsDialog by remember { mutableStateOf<Evidence?>(null) }
    val timelineSortType by caseViewModel.timelineSortType.collectAsState()
    var showAssignExhibitDialog by remember { mutableStateOf<Evidence?>(null) }
    val exhibits by caseViewModel.exhibits.collectAsState()

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
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.End
        ) {
            TimelineSortDropdown(
                sortType = timelineSortType,
                onSortChange = { caseViewModel.onTimelineSortOrderChanged(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (evidenceList.isNotEmpty()) {
                JetLimeColumn(
                    modifier = Modifier.padding(16.dp),
                    itemsList = ItemsList(evidenceList),
                    key = { _, item -> item.id },
                    style = JetLimeDefaults.columnStyle(),
                ) { _, item, position ->
                    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                    val formattedDate = remember(item.timestamp) { sdf.format(Date(item.timestamp)) }

                    JetLimeExtendedEvent(
                        modifier = Modifier,
                        style = JetLimeEventDefaults.eventStyle(position = position),
                        additionalContent = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(formattedDate, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Type: ${item.type}", style = MaterialTheme.typography.bodySmall)
                                Text("Tags: ${item.tags.joinToString()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEvidenceDetailsDialog = item },
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(item.sourceDocument, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.content)
                                Button(onClick = { showAssignExhibitDialog = item }) {
                                    Text("Assign to Exhibit")
                                }
                            }
                        }
                    }
                }
            } else {
                // Show a placeholder extended event when there is no evidence
                JetLimeColumn(
                    modifier = Modifier.padding(16.dp),
                    itemsList = ItemsList(listOf(Unit)), // A single item list for the placeholder
                    style = JetLimeDefaults.columnStyle(),
                ) { _, _, position ->
                    JetLimeExtendedEvent(
                        style = JetLimeEventDefaults.eventStyle(position = position),
                        additionalContent = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("Example Event", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("This is where the event details like type and tags would appear.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text("Placeholder Evidence", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.no_evidence_placeholder))
                            }
                        }
                    }
                }
            }
        }
    }

    showEvidenceDetailsDialog?.let { evidence ->
        TimelineEvidenceDetailsDialog(
            evidence = evidence,
            onDismiss = { showEvidenceDetailsDialog = null },
            onRemove = {
                evidenceDetailsViewModel.removeEvidence(evidence)
                showEvidenceDetailsDialog = null
            },
            onNavigateToEvidenceDetails = { navController.navigate("evidence_details/${evidence.id}") }
        )
    }

    if (showAssignExhibitDialog != null) {
        AssignExhibitDialog(
            evidence = showAssignExhibitDialog!!,
            exhibits = exhibits,
            onDismiss = { showAssignExhibitDialog = null },
            onAssign = { evidence, exhibit ->
                caseViewModel.addEvidenceToExhibit(exhibit.id, listOf(evidence.id))
                showAssignExhibitDialog = null
            }
        )
    }
}

@Composable
fun AssignExhibitDialog(
    evidence: Evidence,
    exhibits: List<Exhibit>,
    onDismiss: () -> Unit,
    onAssign: (Evidence, Exhibit) -> Unit
) {
    var selectedExhibit by remember { mutableStateOf<Exhibit?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to Exhibit") },
        text = {
            LazyColumn {
                items(exhibits) { exhibit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedExhibit = exhibit }
                    ) {
                        RadioButton(
                            selected = selectedExhibit?.id == exhibit.id,
                            onClick = { selectedExhibit = exhibit }
                        )
                        Text(exhibit.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedExhibit?.let {
                        onAssign(evidence, it)
                    }
                },
                enabled = selectedExhibit != null
            ) {
                Text("Assign")
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
fun TimelineSortDropdown(
    sortType: TimelineSortType,
    onSortChange: (TimelineSortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().clickable { expanded = true },
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Sort by: ")
        Text(sortType.toString(), color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort by")
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            enumValues<TimelineSortType>().forEach { sortTypeEntry ->
                DropdownMenuItem(
                    text = { Text(sortTypeEntry.name.replace("_", " ").lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                    onClick = {
                        onSortChange(sortTypeEntry)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun TimelineEvidenceDetailsDialog(
    evidence: Evidence,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onNavigateToEvidenceDetails: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(evidence.sourceDocument) },
        text = { Text(evidence.content) },
        confirmButton = {
            Button(onClick = onNavigateToEvidenceDetails) {
                Text("View Details")
            }
        },
        dismissButton = {
            Button(onClick = onRemove) {
                Text("Remove")
            }
        }
    )
}