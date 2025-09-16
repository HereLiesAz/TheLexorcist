package com.hereliesaz.lexorcist.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EvidenceScreen(
    navController: NavController,
    caseId: Long,
    spreadsheetId: String,
    evidenceViewModel: EvidenceViewModel = hiltViewModel(),
) {
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()
    val isLoading by evidenceViewModel.isLoading.collectAsState()
    val processingStatus by evidenceViewModel.processingStatus.collectAsState()
    val videoProcessingProgress by evidenceViewModel.videoProcessingProgress.collectAsState()
    val logMessages by evidenceViewModel.logMessages.collectAsState()
    val selectedCase by evidenceViewModel.selectedCase.collectAsState()
    var showAddTextDialog by remember { mutableStateOf(false) }

    LaunchedEffect(caseId, spreadsheetId) {
        evidenceViewModel.loadCaseAndEvidence(caseId, spreadsheetId)
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { evidenceViewModel.processImageEvidence(it) }
        }

    val audioPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { evidenceViewModel.processAudioEvidence(it) }
        }

    val videoPickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { evidenceViewModel.processVideoEvidence(it) }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedCase?.name ?: stringResource(R.string.evidence),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Buttons Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                LexorcistOutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    text = stringResource(R.string.add_image)
                )
                LexorcistOutlinedButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    text = stringResource(R.string.add_audio)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                LexorcistOutlinedButton(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    text = stringResource(R.string.add_video)
                )
                LexorcistOutlinedButton(
                    onClick = { showAddTextDialog = true },
                    text = stringResource(R.string.add_text)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Evidence List
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(evidenceList, key = { it.id }) { evidence ->
                    EvidenceListItem(evidence = evidence, onClick = {
                        evidenceViewModel.onEvidenceSelected(evidence)
                        // TODO: Navigate to evidence detail screen
                    })
                    HorizontalDivider()
                }
            }

            // Progress Section
            if (isLoading || processingStatus != null || videoProcessingProgress != null) {
                ProgressSection(
                    processingStatus = processingStatus ?: videoProcessingProgress,
                    logMessages = logMessages,
                    onClearLogs = { evidenceViewModel.clearLogs() }
                )
            }
        }
    }

    if (showAddTextDialog) {
        AddTextEvidenceDialog(
            onDismiss = { showAddTextDialog = false },
            onConfirm = { text ->
                selectedCase?.let {
                    evidenceViewModel.addTextEvidence(text, it.id, it.spreadsheetId)
                }
                showAddTextDialog = false
            }
        )
    }
}

@Composable
fun EvidenceListItem(evidence: Evidence, onClick: () -> Unit) {
    val timestamp = remember(evidence.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(evidence.timestamp))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = evidence.content.take(100) + if (evidence.content.length > 100) "..." else "",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Added: $timestamp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProgressSection(
    processingStatus: String?,
    logMessages: List<String>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (processingStatus != null) {
            Text(text = processingStatus, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(8.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logMessages) { message ->
                    Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        LexorcistOutlinedButton(onClick = onClearLogs, text = "Clear Logs")
    }
}

@Composable
fun AddTextEvidenceDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("Add Text Evidence", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Evidence Text") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LexorcistOutlinedButton(onClick = onDismiss, text = "Cancel")
                    LexorcistOutlinedButton(onClick = { onConfirm(text) }, text = "Add")
                }
            }
        }
    }
}
