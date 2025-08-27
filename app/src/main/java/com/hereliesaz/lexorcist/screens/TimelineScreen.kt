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
import com.hereliesaz.lexorcist.MainViewModel
import com.hereliesaz.lexorcist.models.TimelineEvent
import com.hereliesaz.lexorcist.model.FinancialEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(viewModel: MainViewModel) {
    val financialEntries by viewModel.financialEntries.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val timelineEvents = financialEntries.filter { entry ->
        entry.sourceDocument.contains(searchQuery, ignoreCase = true) ||
                entry.allegationId.contains(searchQuery, ignoreCase = true)
    }.map {
        TimelineEvent(it.documentDate.time, "Amount: ${it.amount}, Source: ${it.sourceDocument}")
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
