package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    evidence: Evidence,
    caseViewModel: CaseViewModel,
    navController: NavController,
) {
    var transcript by remember(evidence) {
        mutableStateOf(evidence.content)
    }
    var reason by remember { mutableStateOf("") }
    var showReasonDialog by remember { mutableStateOf(false) }

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
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
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
            Text("Edit History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(evidence.transcriptEdits) { edit ->
                    Text("${edit.timestamp}: ${edit.reason} - ${edit.content}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LexorcistOutlinedButton(
                onClick = {
                    if (transcript != evidence.content) {
                        showReasonDialog = true
                    } else {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.save).uppercase(Locale.getDefault())
            )
        }
    }

    if (showReasonDialog) {
        ReasonDialog(
            onDismiss = { showReasonDialog = false },
            onConfirm = { reasonText ->
                caseViewModel.updateTranscript(evidence, transcript, reasonText)
                showReasonDialog = false
                navController.popBackStack()
            },
        )
    }
}

@Composable
fun ReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reason_for_edit)) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(stringResource(R.string.reason)) },
                textStyle = TextStyle(textAlign = TextAlign.End)
            )
        },
        confirmButton = {
            LexorcistOutlinedButton(onClick = { onConfirm(reason) }, text = stringResource(R.string.confirm))
        },
        dismissButton = {
            LexorcistOutlinedButton(onClick = onDismiss, text = stringResource(R.string.cancel))
        },
    )
}
