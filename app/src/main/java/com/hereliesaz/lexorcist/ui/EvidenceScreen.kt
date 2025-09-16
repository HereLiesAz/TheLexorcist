package com.hereliesaz.lexorcist.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Added import
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceScreen(
    caseViewModel: CaseViewModel,
    navController: NavController,
) {
    var showAddTextEvidence by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val evidenceViewModel: EvidenceViewModel = hiltViewModel()
    val userMessage by evidenceViewModel.userMessage.collectAsState()
    val videoProcessingProgress by evidenceViewModel.videoProcessingProgress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { evidenceViewModel.processImageEvidence(it) }
        }

    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { evidenceViewModel.processAudioEvidence(it) }
        }

    val videoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { evidenceViewModel.processVideoEvidence(it) }
        }

    LaunchedEffect(Unit) {
        evidenceViewModel.navigateToTranscriptionScreen.collectLatest { evidenceId ->
            navController.navigate("transcription/$evidenceId")
        }
    }

    LaunchedEffect(selectedCase) {
        selectedCase?.let {
            caseViewModel.loadEvidenceForSelectedCase()
        }
    }

    // This will not work anymore as selectedEvidence is in EvidenceViewModel
    // selectedEvidence?.let {
    //     EvidenceDetailsDialog(evidence = it, onDismiss = { evidenceViewModel.onDialogDismiss() })
    // }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Evidence".uppercase(Locale.getDefault()),
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
                    .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Top,
        ) {
            videoProcessingProgress?.let { progress ->
                Column(horizontalAlignment = Alignment.End) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(progress, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            val isLoading by evidenceViewModel.isLoading.collectAsState()
            val processingStatus by evidenceViewModel.processingStatus.collectAsState()
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    processingStatus?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (showAddTextEvidence) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.evidence_text_label)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    placeholder = { Text(stringResource(R.string.enter_evidence_content)) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                LexorcistOutlinedButton(
                    onClick = {
                        selectedCase?.let {
                            evidenceViewModel.addTextEvidence(
                                text,
                                it.id.toLong(),
                                it.spreadsheetId,
                            )
                        }
                        text = ""
                        showAddTextEvidence = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.save).uppercase(Locale.getDefault())
                )
                Spacer(modifier = Modifier.height(16.dp))
                LexorcistOutlinedButton(
                    onClick = { showAddTextEvidence = false },
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cancel).uppercase(Locale.getDefault())
                )
            } else {
                LexorcistOutlinedButton(onClick = { showAddTextEvidence = true }, text = stringResource(R.string.add_text_evidence).uppercase(Locale.getDefault()))
                Spacer(modifier = Modifier.height(8.dp))
                LexorcistOutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }, text = stringResource(R.string.add_image_evidence).uppercase(Locale.getDefault()))
                Spacer(modifier = Modifier.height(8.dp))
                LexorcistOutlinedButton(onClick = { audioPickerLauncher.launch("audio/*") }, text = stringResource(R.string.add_audio_evidence).uppercase(Locale.getDefault()))
                Spacer(modifier = Modifier.height(8.dp))
                LexorcistOutlinedButton(onClick = { videoPickerLauncher.launch("video/*") }, text = stringResource(R.string.add_video_evidence).uppercase(Locale.getDefault()))
            }

            Spacer(modifier = Modifier.height(16.dp))

            val logMessages by evidenceViewModel.logMessages.collectAsState()
            if (logMessages.isNotEmpty()) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(logMessages) { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(evidenceList) { evidence ->
                    EvidenceListItem(
                        evidence = evidence,
                        onClick = {
                            if (evidence.type == "audio") {
                                evidenceViewModel.loadEvidenceById(evidence.id)
                                evidenceViewModel.requestNavigationToTranscriptionScreen(evidence.id) // <<< Corrected here
                            } else {
                                evidenceViewModel.onEvidenceSelected(evidence)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EvidenceListItem(
    evidence: com.hereliesaz.lexorcist.data.Evidence,
    onClick: () -> Unit,
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val formattedDate = remember(evidence.timestamp) { sdf.format(Date(evidence.timestamp)) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = "Type: ${evidence.type} | Added: $formattedDate",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = evidence.content,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
            maxLines = 3,
        )
    }
}
