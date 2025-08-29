package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import io.github.pushpalroy.jetlime.Event
import io.github.pushpalroy.jetlime.JetLime
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(viewModel: MainViewModel) {
    val evidenceList by viewModel.selectedCaseEvidenceList.collectAsState()

    val events = evidenceList.map {
        Event(
            title = {
                Text(
                    text = it.sourceDocument,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            },
            subtitle = {
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.documentDate),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
    }

    JetLime(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        events = events
    )
}
