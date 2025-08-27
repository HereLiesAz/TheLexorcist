package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.MainViewModel
import com.hereliesaz.lexorcist.db.FinancialEntry
import com.hereliesaz.lexorcist.models.TimelineEvent // Added import
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.EmptyCoroutineContext

// Removed TimelineEvent data class from here

@Composable
fun TimelineScreen(viewModel: MainViewModel) {
    val financialEntries by viewModel.financialEntries.collectAsState(
        initial = emptyList(),
        context = EmptyCoroutineContext // Or remove this line to use the default
    )
    val timelineEvents = financialEntries.map {
        TimelineEvent(it.documentDate, "Amount: ${it.amount}, Category: ${it.category}")
    }.sortedBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End, // Align children to the End (right)
        verticalArrangement = Arrangement.Center // Center children vertically as a group
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth() // LazyColumn itself should span width
                .weight(1f)     // Take available space within the centered group
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
