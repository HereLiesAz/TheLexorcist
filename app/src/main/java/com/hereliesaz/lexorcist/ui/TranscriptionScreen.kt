package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.model.TranscriptEdit
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.hereliesaz.lexorcist.model.ProcessingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    evidence: Evidence,
    caseViewModel: CaseViewModel,
    navController: NavController,
) {
    var transcript by remember(evidence) { mutableStateOf(evidence.content) }
    var reason by remember { mutableStateOf("") }
    val isTranscriptChanged = transcript != evidence.content
    val processingState by caseViewModel.processingState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.transcription).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = processingState) {
                is ProcessingState.InProgress -> {
                    com.hereliesaz.lexorcist.ui.components.LexorcistLoadingIndicator()
                }
                is ProcessingState.Failure -> {
                    Text(
                        text = "Transcription failed: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                is ProcessingState.Completed -> {
                    transcript = state.result
                    Column(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        OutlinedTextField(
                            value = transcript,
                            onValueChange = { transcript = it },
                            label = { Text(stringResource(R.string.transcript_label)) },
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = TextStyle(textAlign = TextAlign.End)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isTranscriptChanged) {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = { Text(stringResource(R.string.reason_for_edit)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(textAlign = TextAlign.End)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        LexorcistOutlinedButton(
                            onClick = {
                                if (isTranscriptChanged) {
                                    caseViewModel.updateTranscript(evidence, transcript, reason)
                                }
                                navController.popBackStack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTranscriptChanged || reason.isNotBlank(),
                            text = stringResource(if (isTranscriptChanged) R.string.save_edit else R.string.done).uppercase(Locale.getDefault())
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            stringResource(R.string.edit_history),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(evidence.transcriptEdits.sortedByDescending { it.timestamp }) { edit ->
                                EditHistoryItem(edit = edit)
                            }
                        }
                    }
                }
                else -> {
                    // Idle state, show the editor
                    Column(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        OutlinedTextField(
                            value = transcript,
                            onValueChange = { transcript = it },
                            label = { Text(stringResource(R.string.transcript_label)) },
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = TextStyle(textAlign = TextAlign.End)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isTranscriptChanged) {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = { Text(stringResource(R.string.reason_for_edit)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(textAlign = TextAlign.End)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        LexorcistOutlinedButton(
                            onClick = {
                                if (isTranscriptChanged) {
                                    caseViewModel.updateTranscript(evidence, transcript, reason)
                                }
                                navController.popBackStack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isTranscriptChanged || reason.isNotBlank(),
                            text = stringResource(if (isTranscriptChanged) R.string.save_edit else R.string.done).uppercase(Locale.getDefault())
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            stringResource(R.string.edit_history),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(evidence.transcriptEdits.sortedByDescending { it.timestamp }) { edit ->
                                EditHistoryItem(edit = edit)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditHistoryItem(edit: TranscriptEdit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(edit.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reason: ${edit.reason}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = edit.content,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
