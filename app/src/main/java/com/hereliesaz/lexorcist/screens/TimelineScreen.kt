package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.models.TimelineEvent
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(viewModel: MainViewModel) {
    val evidenceList by viewModel.selectedCaseEvidenceList.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val timelineEvents = evidenceList.filter {
        it.content.contains(searchQuery, ignoreCase = true) ||
        it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
    }.map {
        TimelineEvent(it.documentDate, "Content: ${it.content}, Tags: ${it.tags.joinToString(", ")}")
    }.sortedBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(event.date)),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.description)
        }
    }
}
