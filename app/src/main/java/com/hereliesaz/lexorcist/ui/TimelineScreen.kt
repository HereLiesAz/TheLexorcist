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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi // Added for experimental APIs
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.TimelineSortType
import com.pushpal.jetlime.ItemsList
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeDefaults
import com.pushpal.jetlime.JetLimeEvent
import com.pushpal.jetlime.JetLimeEventDefaults
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class,
    ExperimentalComposeApi::class
) // Added ExperimentalComposeUiApi
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
                        stringResource(R.string.timeline).uppercase(Locale.getDefault()),
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
                    key = { _: Int, item: Evidence -> item.id }, // Explicitly typed item
                    style = JetLimeDefaults.columnStyle(),
                ) { _, item, position ->
                    JetLimeEvent(
                        style = JetLimeEventDefaults.eventStyle(position = position)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEvidenceDetailsDialog = item },
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(item.sourceDocument, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.content)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.no_evidence_placeholder),
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
            onNavigateToEvidenceDetails = { navController.navigate("evidence_details/${evidence.id}") } 
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
        LexorcistOutlinedButton(onClick = { expanded = true }, text = stringResource(id = R.string.sort))
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            enumValues<TimelineSortType>().forEach { sortTypeEntry ->
                DropdownMenuItem(
                    text = { Text(sortTypeEntry.name.replace("_", " ").lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) }, // Corrected to use sortTypeEntry for display
                    onClick = {
                        onSortChange(sortTypeEntry)
                        expanded = false
                    },
                )
            }
        }
    }
}
