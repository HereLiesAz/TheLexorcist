package com.hereliesaz.lexorcist.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceScreen(
    evidenceViewModel: EvidenceViewModel = hiltViewModel(),
    caseViewModel: CaseViewModel = hiltViewModel(),
    navController: NavController,
) {
    var showAddTextEvidence by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val selectedCase by caseViewModel.selectedCase.collectAsState()

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { evidenceViewModel::processImageEvidence }
        }

    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { evidenceViewModel::processAudioEvidence }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.evidence).uppercase(Locale.getDefault()),
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
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Top,
        ) {
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
                Button(
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
                    enabled = selectedCase != null,
                ) {
                    Text(stringResource(R.string.save).uppercase(Locale.getDefault()))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAddTextEvidence = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cancel).uppercase(Locale.getDefault()))
                }
            } else {
                Button(onClick = { showAddTextEvidence = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription =
                            stringResource(R.string.add_text_evidence).uppercase(
                                Locale.getDefault(),
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_text_evidence).uppercase(Locale.getDefault()))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription =
                            stringResource(R.string.add_image_evidence).uppercase(
                                Locale.getDefault(),
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_image_evidence).uppercase(Locale.getDefault()))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { audioPickerLauncher.launch("audio/*") }) {
                    Icon(
                        Icons.Default.Audiotrack,
                        contentDescription =
                            stringResource(R.string.add_audio_evidence).uppercase(
                                Locale.getDefault(),
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_audio_evidence).uppercase(Locale.getDefault()))
                }
            }
        }
    }
}
