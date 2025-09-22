package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.model.CleanupSuggestion
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel // Added import

@Composable
fun CleanupDialog(
    caseViewModel: CaseViewModel,
    mainViewModel: MainViewModel, // Added mainViewModel
    onDismiss: () -> Unit
) {
    val suggestions by caseViewModel.cleanupSuggestions.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cleanup Suggestions") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(suggestions) { suggestion ->
                    when (suggestion) {
                        is CleanupSuggestion.DuplicateGroup -> {
                            Column {
                                Text("Duplicate Group:", style = MaterialTheme.typography.titleMedium)
                                suggestion.evidence.forEach { evidence ->
                                    Text("- ${evidence.sourceDocument}")
                                }
                                Button(onClick = { caseViewModel.deleteDuplicates(suggestion, mainViewModel) }) { // Added mainViewModel
                                    Text("Delete Duplicates")
                                }
                            }
                        }
                        is CleanupSuggestion.ImageSeriesGroup -> {
                            Column {
                                Text("Image Series Group:", style = MaterialTheme.typography.titleMedium)
                                suggestion.evidence.forEach { evidence ->
                                    Text("- ${evidence.sourceDocument}")
                                }
                                Button(onClick = { caseViewModel.mergeImageSeries(suggestion, mainViewModel) }) { // Added mainViewModel
                                    Text("Merge into PDF")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
