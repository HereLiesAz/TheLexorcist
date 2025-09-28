package com.hereliesaz.lexorcist.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.hereliesaz.lexorcist.ui.components.ChatHistoryImportDialog
import com.hereliesaz.lexorcist.ui.components.EmailImportDialog
import com.hereliesaz.lexorcist.ui.components.ImapImportDialog
import com.hereliesaz.lexorcist.ui.components.ImportFilterDialog
import androidx.compose.ui.Alignment
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hereliesaz.lexorcist.model.OutlookSignInState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.LogEntry
import com.hereliesaz.lexorcist.model.LogLevel // Added import for clarity
import com.hereliesaz.lexorcist.model.ProcessingState
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hereliesaz.aznavrail.AzLoad

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EvidenceScreen(
    navController: NavController,
    caseViewModel: CaseViewModel,
    mainViewModel: MainViewModel,
    authViewModel: com.hereliesaz.lexorcist.viewmodel.AuthViewModel = hiltViewModel()
) {
    var showAddTextEvidence by remember { mutableStateOf(false) }
    var showChatImportDialog by remember { mutableStateOf(false) }
    var showGmailImportDialog by remember { mutableStateOf(false) }
    var showOutlookImportDialog by remember { mutableStateOf(false) }
    var showImapImportDialog by remember { mutableStateOf(false) }
    var showImportFilterDialog by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val videoProcessingProgress by caseViewModel.videoProcessingProgress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarHostState) {
        caseViewModel.userMessage.collectLatest { message ->
            message?.let {
                snackbarHostState.showSnackbar(it)
                caseViewModel.clearUserMessage() // Clear message after showing
            }
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
            uri?.let { caseViewModel.processAudioEvidence(it, "") }
        }

    val videoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { caseViewModel.processVideoEvidence(it) }
        }

    val requestSmsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val contact = mainViewModel.importContact.value
            val startDate = mainViewModel.importStartDate.value
            val endDate = mainViewModel.importEndDate.value
            caseViewModel.importSmsEvidence(contact, startDate, endDate)
        } else {
            // Handle permission denial
        }
    }

    val requestCallLogPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val contact = mainViewModel.importContact.value
            val startDate = mainViewModel.importStartDate.value
            val endDate = mainViewModel.importEndDate.value
            caseViewModel.importCallLogEvidence(contact, startDate, endDate)
        } else {
            // Handle permission denial
        }
    }

    val requestLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            caseViewModel.importLocationHistoryEvidence()
        } else {
            // Handle permission denial
        }
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
            videoProcessingProgress?.let { progressMessage ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(progressMessage, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            val processingState by caseViewModel.processingState.collectAsState()
            val logMessages by caseViewModel.logMessages.collectAsState()

            processingState?.let {
                if (it !is ProcessingState.Idle || logMessages.isNotEmpty()) {
                    ProcessingProgressView(
                        processingState = it,
                        logMessages = logMessages
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Show text input field if showAddTextEvidence is true AND processing is not in progress
            if (showAddTextEvidence && (processingState == null || processingState !is ProcessingState.InProgress)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.evidence_text_label)) },
                    modifier =
                    Modifier
                        .fillMaxWidth(), // Removed .weight(1f)
                    placeholder = { Text(stringResource(R.string.enter_evidence_content)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                )
                Spacer(modifier = Modifier.height(16.dp))
                LexorcistOutlinedButton(
                    onClick = {
                        caseViewModel.addTextEvidence(text, "")
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
            // Show action buttons if text input is not shown AND processing is not in progress
            } else if (processingState == null || processingState !is ProcessingState.InProgress) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LexorcistOutlinedButton(onClick = { showAddTextEvidence = true }, text = "Text".uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }, text = "Image".uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { audioPickerLauncher.launch("audio/*") }, text = "Audio".uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { videoPickerLauncher.launch("video/*") }, text = "Video".uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { navController.navigate("photo_group") }, text = "Photo".uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { showImportFilterDialog = true }, text = "Device Records".uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }, text = "Location".uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { showChatImportDialog = true }, text = "Messages".uppercase(Locale.getDefault()))
                    LexorcistOutlinedButton(onClick = { showGmailImportDialog = true }, text = "Gmail".uppercase(Locale.getDefault()))
                    val outlookSignInState by authViewModel.outlookSignInState.collectAsState()
                    LexorcistOutlinedButton(
                        onClick = {
                            if (outlookSignInState is OutlookSignInState.Success) {
                                showOutlookImportDialog = true
                            } else {
                                // Optionally, show a snackbar or message to the user
                            }
                        },
                        text = "Outlook".uppercase(Locale.getDefault()),
                        enabled = outlookSignInState is OutlookSignInState.Success
                    )
                    LexorcistOutlinedButton(
                        onClick = { showImapImportDialog = true },
                        text = "Email".uppercase(Locale.getDefault())
                    )
                }
            }

            if (showImportFilterDialog) {
                ImportFilterDialog(
                    onDismiss = { showImportFilterDialog = false },
                    onImport = { contact, startDate, endDate, importSms, importCalls ->
                        showImportFilterDialog = false
                        if (importSms) {
                            requestSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                        if (importCalls) {
                            requestCallLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                        }
                        // The actual import will be triggered by the permission result callbacks,
                        // we need to update the viewmodel to hold these filter values.
                        mainViewModel.setImportFilters(contact, startDate, endDate, importSms, importCalls)
                    }
                )
            }

            if (showChatImportDialog) {
                ChatHistoryImportDialog(
                    onDismiss = { showChatImportDialog = false },
                    onImport = { uri ->
                        showChatImportDialog = false
                        caseViewModel.importChatHistory(uri)
                    }
                )
            }

            if (showGmailImportDialog) {
                EmailImportDialog(
                    title = "Import from Gmail",
                    onDismiss = { showGmailImportDialog = false },
                    onImport = { from, subject, before, after ->
                        showGmailImportDialog = false
                        caseViewModel.importEmails(
                            from = from,
                            subject = subject,
                            before = before,
                            after = after
                        )
                    }
                )
            }

            if (showOutlookImportDialog) {
                EmailImportDialog(
                    title = "Import from Outlook",
                    onDismiss = { showOutlookImportDialog = false },
                    onImport = { from, subject, before, after ->
                        showOutlookImportDialog = false
                        // val outlookState = authViewModel.outlookSignInState.value // No longer needed here
                        // if (outlookState is com.hereliesaz.lexorcist.model.OutlookSignInState.Success) { // Check is inside ViewModel
                            caseViewModel.importOutlookEmails(
                                // accessToken = outlookState.accessToken, // REMOVED - ViewModel handles token access
                                from = from,
                                subject = subject,
                                before = before,
                                after = after
                            )
                        // }
                    }
                )
            }

            if (showImapImportDialog) {
                ImapImportDialog(
                    onDismiss = { showImapImportDialog = false },
                    onImport = { host, user, pass, from, subject ->
                        showImapImportDialog = false
                        caseViewModel.importImapEmails(host, user, pass, from, subject)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(evidenceList) { evidence ->
                    EvidenceListItem(
                        evidence = evidence,
                        onClick = {
                                    when (evidence.type) {
                                        "audio" -> navController.navigate("transcription/${evidence.id}")
                                        "video" -> navController.navigate("video_evidence/${evidence.id}")
                                        else -> navController.navigate("evidence_details/${evidence.id}")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Processing: ${"%.0f".format(processingState.progress * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AzLoad(modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { processingState.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ProcessingState.Completed -> {
                Text(
                    text = "Completed: ${processingState.result}",
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.primary, 
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ProcessingState.Failure -> {
                Text(
                    text = "Failed: ${processingState.error}",
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ProcessingState.Idle -> {
                if (logMessages.isEmpty()) { 
                    Text(
                        text = "Idle",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        if (logMessages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                items(logMessages) { logEntry ->
                    val color = when (logEntry.level) {
                        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                        LogLevel.ERROR -> MaterialTheme.colorScheme.error
                        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                        LogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                        LogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurfaceVariant
                        LogLevel.WTF -> MaterialTheme.colorScheme.error
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
