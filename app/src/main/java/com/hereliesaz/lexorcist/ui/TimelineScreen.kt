package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.pushpal.jetlime.JetLimeEvent
import com.pushpal.jetlime.JetLimeColumn
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import io.github.pushpalroy.jetlime.Event
import io.github.pushpalroy.jetlime.JetLime
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(evidenceViewModel: EvidenceViewModel) {
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()
fun TimelineScreen(viewModel: MainViewModel) {
    val evidenceList by viewModel.selectedCaseEvidenceList.collectAsState()
    var selectedEvidence by remember { mutableStateOf<Evidence?>(null) }

    val events = evidenceList.map { evidence ->
        JetLimeEvent {
            onClick = { selectedEvidence = evidence },
            title = {
                Text(
                    text = evidence.sourceDocument,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            },
            subtitle = {
                Text(
                    text = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(evidence.documentDate),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    JetLimeColumn {
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        events = events
    }

    selectedEvidence?.let {
        EvidenceDetailsDialog(evidence = it, onDismiss = { selectedEvidence = null })
    }
}

@Composable
fun EvidenceDetailsDialog(evidence: Evidence, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Evidence Details") },
        text = {
            Column {
                Text("Content: ${evidence.content}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Category: ${evidence.category ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tags: ${evidence.tags?.joinToString() ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
