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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.MainViewModel
import com.hereliesaz.lexorcist.db.FinancialEntry
import java.text.SimpleDateFormat
import java.util.*

data class TimelineEvent(val date: Long, val description: String)

@Composable
fun TimelineScreen(viewModel: MainViewModel) {
    val financialEntries by viewModel.financialEntries.collectAsState()
    val timelineEvents = financialEntries.map {
        TimelineEvent(it.documentDate, "Amount: ${it.amount}, Category: ${it.category}")
    }.sortedBy { it.date }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(timelineEvents) { event ->
            TimelineItem(event = event)
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
