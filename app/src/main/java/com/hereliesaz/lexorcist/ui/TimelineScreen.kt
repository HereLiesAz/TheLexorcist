package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    caseViewModel: CaseViewModel = hiltViewModel()
) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val exhibits by caseViewModel.exhibits.collectAsState()
    var showAssignExhibitDialog by remember { mutableStateOf<Evidence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timeline)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(evidenceList.sortedBy { it.documentDate }) { evidence ->
                TimelineItem(
                    evidence = evidence,
                    onAssignExhibitClick = { showAssignExhibitDialog = evidence }
                )
            }
        }

        if (showAssignExhibitDialog != null) {
            AssignExhibitDialog(
                evidence = showAssignExhibitDialog!!,
                exhibits = exhibits,
                onDismiss = { showAssignExhibitDialog = null },
                onAssign = { evidence, exhibit ->
                    // This is a simplified assignment. In a real app, you'd update the exhibit with the evidence.
                    // For now, we'll just log it.
                    println("Assigning ${evidence.id} to ${exhibit.name}")
                    showAssignExhibitDialog = null
                }
            )
        }
    }
}

@Composable
fun TimelineItem(
    evidence: Evidence,
    onAssignExhibitClick: (Evidence) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            Canvas(modifier = Modifier.size(24.dp), onDraw = {
                drawCircle(color = Color.Gray, radius = 12.dp.toPx())
            })
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(modifier = Modifier.weight(1f).width(2.dp), onDraw = {
                drawLine(
                    color = Color.Gray,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            })
        }
        Column {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(evidence.documentDate)
            Text(date, style = MaterialTheme.typography.titleMedium)
            Text(evidence.content, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onAssignExhibitClick(evidence) }) {
                Text("Assign to Exhibit")
            }
        }
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