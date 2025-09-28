package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.lexorcist.model.CleanupSuggestion
import com.hereliesaz.lexorcist.ui.components.AzAlertDialog
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel

@Composable
fun CleanupDialog(
    caseViewModel: CaseViewModel,
    onDismiss: () -> Unit,
) {
    val suggestions by caseViewModel.cleanupSuggestions.collectAsState()

    AzAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Cleanup Suggestions",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                items(suggestions) { suggestion ->
                    when (suggestion) {
                        is CleanupSuggestion.DuplicateGroup -> {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Duplicate Group:", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                                suggestion.evidence.forEach { evidence ->
                                    Text("- ${evidence.sourceDocument}", textAlign = TextAlign.End)
                                }
                                AzButton(onClick = { caseViewModel.deleteDuplicates(suggestion) }, text = "Delete Duplicates")
                            }
                        }
                        is CleanupSuggestion.ImageSeriesGroup -> {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Image Series Group:", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                                suggestion.evidence.forEach { evidence ->
                                    Text("- ${evidence.sourceDocument}", textAlign = TextAlign.End)
                                }
                                AzButton(onClick = { caseViewModel.mergeImageSeries(suggestion, "") }, text = "Merge into PDF")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        confirmButton = {
            AzButton(onClick = onDismiss, text = "Close")
        },
        dismissButton = {}
    )
}
