package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType // Added import
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow // Changed from TabRow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Allegation
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.AllegationsViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    caseViewModel: CaseViewModel = hiltViewModel(),
    allegationsViewModel: AllegationsViewModel = hiltViewModel(),
) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val isLoading by caseViewModel.isLoading.collectAsState()
    var showReadinessReportDialog by remember { mutableStateOf(false) }
    var readinessReport by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (
                            if (selectedCase != null) {
                                stringResource(R.string.data_review_title_case, selectedCase!!.name)
                            } else {
                                stringResource(R.string.data_review)
                            }
                            ).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator()
                }
            } else if (selectedCase == null) {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.please_select_case_for_evidence).uppercase(Locale.getDefault()))
                }
            } else if (evidenceList.isEmpty()) {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.no_evidence_for_case).uppercase(Locale.getDefault()))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(evidenceList) { evidence ->
                        EvidenceItem(
                            evidence = evidence,
                            isSelected = false,
                            onClick = {},
                            onEditClick = {},
                            onDeleteClick = {}
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    LexorcistOutlinedButton(
                        onClick = {
                            readinessReport = caseViewModel.generateReadinessReport()
                            showReadinessReportDialog = true
                        },
                        text = "Readiness Report",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

    if (showReadinessReportDialog) {
        ReadinessReportDialog(
            report = readinessReport,
            onDismiss = { showReadinessReportDialog = false }
        )
    }
}

@Composable
fun ReadinessReportDialog(
    report: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Readiness Report") },
        text = {
            LazyColumn {
                item {
                    Text(report)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
