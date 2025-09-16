package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.TimelineSortType
import com.jet.jetlime.ItemsList
import com.jet.jetlime.JetLimeColumn
import com.jet.jetlime.JetLimeDefaults
import com.jet.jetlime.JetLimeExtendedEvent
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(caseViewModel: CaseViewModel, navController: NavController) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()
    var showEvidenceDetailsDialog by remember { mutableStateOf<Evidence?>(null) }
    val timelineSortType by caseViewModel.timelineSortType.collectAsState()

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
            TimelineSortDropdown(
                sortType = timelineSortType,
                onSortChange = { caseViewModel.onTimelineSortOrderChanged(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (evidenceList.isNotEmpty()) {
                JetLimeColumn(
                    modifier = Modifier.padding(16.dp),
                    itemsList = ItemsList(evidenceList),
                    key = { _, item -> item.id },
                    style = JetLimeDefaults.columnStyle(),
                ) { _, item, _ ->
                    JetLimeExtendedEvent(
                        content = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showEvidenceDetailsDialog = item },
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(item.type, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.content)
                                }
                            }
                        }
                    )
                }
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

@Composable
fun TimelineSortDropdown(
    sortType: TimelineSortType,
    onSortChange: (TimelineSortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Sort by: $sortType")
        Spacer(modifier = Modifier.height(8.dp))
        LexorcistOutlinedButton(onClick = { expanded = true }, text = "Change")
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TimelineSortType.values().forEach { sortType ->
                DropdownMenuItem(
                    text = { Text(sortType.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() }) },
                    onClick = {
                        onSortChange(sortType)
                        expanded = false
                    },
                )
            }
        }
    }
}
