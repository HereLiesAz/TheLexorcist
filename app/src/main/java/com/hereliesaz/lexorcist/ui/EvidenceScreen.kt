package com.hereliesaz.lexorcist.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.LogEntry
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EvidenceScreen(
    navController: NavController,
    caseViewModel: CaseViewModel, 
) {
    var showAddTextEvidence by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val videoProcessingProgress by caseViewModel.videoProcessingProgress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarHostState) {
        caseViewModel.userMessage.collectLatest { message ->
            message?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { caseViewModel.processImageEvidence(it) }
        }

    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { caseViewModel.processAudioEvidence(it) }
        }

    val videoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { caseViewModel.processVideoEvidence(it) }
        }

    LaunchedEffect(Unit) {
        caseViewModel.navigateToTranscriptionScreen.collectLatest { evidenceId ->
            navController.navigate("transcription/$evidenceId")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.add_evidence).uppercase(Locale.getDefault()),
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
            // This specific videoProcessingProgress seems separate from the general ProcessingState below
            // If it's a simple string message, it can remain. If it's more complex, it might need integration.
            videoProcessingProgress?.let { progressMessage ->
                Column(horizontalAlignment = Alignment.End) {
                    // Assuming videoProcessingProgress is a simple string for now.
                    // If it was meant to be a float for LinearProgressIndicator, it needs type change in ViewModel.
                    Text(progressMessage, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            val processingState by caseViewModel.processingState.collectAsState()
            val logMessages by caseViewModel.logMessages.collectAsState()

            // Show ProcessingProgressView if processingState is not null (i.e., not initial/irrelevant state)
            processingState?.let {
                if (it !is ProcessingState.Idle || logMessages.isNotEmpty()) { // Show if not idle or if there are logs
                    ProcessingProgressView(
                        processingState = it,
                        logMessages = logMessages
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (showAddTextEvidence && (processingState == null || processingState is ProcessingState.Idle)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.evidence_text_label)) },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f), // Takes available vertical space
                    placeholder = { Text(stringResource(R.string.enter_evidence_content)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                )
                Spacer(modifier = Modifier.height(16.dp))
                LexorcistOutlinedButton(
                    onClick = {
                        caseViewModel.addTextEvidence(text)
                        text = ""
                        showAddTextEvidence = false
                    },
                    text = stringResource(R.string.save).uppercase(Locale.getDefault())
                )
                Spacer(modifier = Modifier.height(16.dp))
                LexorcistOutlinedButton(
                    onClick = { showAddTextEvidence = false },
                    text = stringResource(R.string.cancel).uppercase(Locale.getDefault())
                )
            } else if (processingState == null || processingState is ProcessingState.Idle) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LexorcistOutlinedButton(onClick = { showAddTextEvidence = true }, text = stringResource(R.string.add_text_evidence).uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }, text = stringResource(R.string.add_image_evidence).uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { audioPickerLauncher.launch("audio/*") }, text = stringResource(R.string.add_audio_evidence).uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { videoPickerLauncher.launch("video/*") }, text = stringResource(R.string.add_video_evidence).uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { navController.navigate("photo_group") }, text = stringResource(R.string.take_photo).uppercase(Locale.getDefault())) 
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(evidenceList) { evidence ->
                    EvidenceListItem(
                        evidence = evidence,
                        onClick = {
                            if (evidence.type == "audio") {
                                navController.navigate("transcription/${evidence.id}")
                            } else {
                                navController.navigate("evidence_details/${evidence.id}")
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

@Composable
fun ProcessingProgressView(
    processingState: ProcessingState,
    logMessages: List<LogEntry>
) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        when (processingState) {
            is ProcessingState.InProgress -> {
                LinearProgressIndicator(
                    progress = { processingState.progress }, // Progress is 0.0f to 1.0f
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Processing: ${"%.0f".format(processingState.progress * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ProcessingState.Completed -> {
                Text(
                    text = "Completed: ${processingState.result}",
                    style = MaterialTheme.typography.bodyMedium, // Changed to bodyMedium for more emphasis
                    color = MaterialTheme.colorScheme.primary, // Using primary color for success
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ProcessingState.Failure -> {
                Text(
                    text = "Failed: ${processingState.error}",
                    style = MaterialTheme.typography.bodyMedium, // Changed to bodyMedium for more emphasis
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ProcessingState.Idle -> {
                if (logMessages.isEmpty()) { // Only show Idle if there are no logs to display from a previous run
                    Text(
                        text = "Idle",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Always show logs if any are present, regardless of Idle state
        if (logMessages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                items(logMessages) {
                    logEntry ->
                    val color = when (logEntry.level) {
                        com.hereliesaz.lexorcist.model.LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                        com.hereliesaz.lexorcist.model.LogLevel.ERROR -> MaterialTheme.colorScheme.error
                        com.hereliesaz.lexorcist.model.LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = "${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(logEntry.timestamp))} - ${logEntry.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
