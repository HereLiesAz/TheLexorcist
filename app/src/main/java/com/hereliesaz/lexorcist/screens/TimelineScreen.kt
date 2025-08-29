package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Corrected import for the MainViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.model.Evidence
import com.hereliesaz.lexorcist.model.TimelineEvent // Added import
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.EmptyCoroutineContext

// Removed TimelineEvent data class from here

@Composable
fun TimelineScreen(viewModel: MainViewModel) { // ViewModel type now correctly refers to the one in .viewmodel package
    // Corrected to use selectedCaseEvidenceList from the merged ViewModel
    val evidence by viewModel.selectedCaseEvidenceList.collectAsState(
        initial = emptyList(),
        context = EmptyCoroutineContext // Or remove this line to use the default
    )
    var searchQuery by remember { mutableStateOf("") }
    val timelineEvents = evidence.filter { evidenceItem ->
        when {
            searchQuery.startsWith("tag:") -> {
                val tagQuery = searchQuery.substringAfter("tag:").trim()
                evidenceItem.tags.any { tag -> tag.contains(tagQuery, ignoreCase = true) }
            }
            searchQuery.startsWith("content:") -> {
                val contentQuery = searchQuery.substringAfter("content:").trim()
                evidenceItem.content.contains(contentQuery, ignoreCase = true)
            }
            else -> {
                evidenceItem.content.contains(searchQuery, ignoreCase = true) ||
                        evidenceItem.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        }
    }.map {
        TimelineEvent(it.documentDate, "Content: ${it.content}, Tags: ${it.tags.joinToString(", ")}")
    }.sortedBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(timelineEvents) { event ->
                TimelineItem(event = event)
            }
        }
    }
}

@Composable
fun TimelineItem(event: TimelineEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Card still fills width to provide a background for the right-aligned text
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End // Align text within the card to the End (right)
        ) {
            Text(
                text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(event.date)),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.description)
        }
    }
}
