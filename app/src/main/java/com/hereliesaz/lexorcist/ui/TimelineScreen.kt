package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import java.util.Locale
import com.jet.jetlime.JetLimeExtended
import com.jet.jetlime.JetLimeDefaults
import com.jet.jetlime.JetLimeEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(caseViewModel: com.hereliesaz.lexorcist.viewmodel.CaseViewModel, navController: NavController) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    var showEvidenceDetailsDialog by remember { mutableStateOf<Evidence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Timeline".uppercase(java.util.Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.End
        ) {
            if (evidenceList.isNotEmpty()) {
                val items =
                    evidenceList.map {
                        JetLimeEvent(
                            title = { Text(it.type) },
                            description = {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showEvidenceDetailsDialog = it },
                                ) {
                                    Text(it.content, modifier = Modifier.padding(8.dp))
                                }
                            },
                        )
                    }
                JetLimeExtended(
                    modifier = Modifier.padding(16.dp),
                    items = items,
                    jetLimeStyle = JetLimeDefaults.jetLimeStyle(contentDistance = 20.dp),
                )
            } else {
                Text(
                    text = "No evidence to display on the timeline.",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }

    showEvidenceDetailsDialog?.let { evidence ->
        EvidenceDetailsDialog(
            evidence = evidence,
            onDismiss = { showEvidenceDetailsDialog = null },
            onNavigateToEvidenceDetails = {
                navController.navigate("evidence_details/${evidence.id}")
                showEvidenceDetailsDialog = null
            },
        )
    }
}
