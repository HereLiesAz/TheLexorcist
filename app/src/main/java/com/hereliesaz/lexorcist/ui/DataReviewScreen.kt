package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.model.TaggedEvidence
import com.hereliesaz.lexorcist.viewmodel.DataReviewViewModel

@Composable
fun DataReviewScreen(
    viewModel: DataReviewViewModel
) {
    val taggedEvidenceList by viewModel.taggedEvidenceList.collectAsState()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(taggedEvidenceList) { evidence ->
            TaggedEvidenceItem(
                evidence = evidence,
                onRelevanceChange = { newRelevance ->
                    viewModel.updateRelevance(evidence, newRelevance.toInt())
                },
                onNotesChange = { newNotes ->
                    viewModel.updateNotes(evidence, newNotes)
                }
            )
        }
    }
}

@Composable
fun TaggedEvidenceItem(
    evidence: TaggedEvidence,
    onRelevanceChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit
) {
    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Evidence: ${evidence.id.content}")
            Text(text = "Tags: ${evidence.tags.joinToString()}")
            Text(text = "Relevance: ${evidence.relevance}")
            Slider(
                value = evidence.relevance.toFloat(),
                onValueChange = onRelevanceChange,
                valueRange = 0f..10f
            )
            TextField(
                value = evidence.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") }
            )
        }
    }
}