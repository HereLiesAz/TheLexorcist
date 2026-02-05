package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.ui.components.ExtendedEvent
import com.hereliesaz.lexorcist.ui.components.PlaceholderExtendedEvent
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.pushpal.jetlime.ItemsList
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeDefaults
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.JetLimeExtendedEvent
import com.pushpal.jetlime.EventPointType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying the chronological timeline of evidence.
 *
 * Uses the JetLime library to render a timeline view.
 * Evidence items are sorted by their `documentDate` (occurrence date).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class)
@Composable
fun TimelineScreen(
    caseViewModel: CaseViewModel = hiltViewModel()
) {
    // Collect evidence list from ViewModel.
    // NOTE: sorting is already handled in ViewModel for the main list, but here we enforce chronological sort by date.
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()

    // Optimization: remember the placeholder item to avoid object allocation on every recomposition.
    // This placeholder is shown when the list is empty to provide user feedback.
    val placeholderEvidenceItem = remember {
        Evidence(
            id = 0, // Placeholder ID
            caseId = 0L,
            spreadsheetId = "placeholder_case",
            type = "placeholder",
            content = "No evidence items yet. Add some evidence to see the timeline.",
            formattedContent = null,
            mediaUri = null,
            timestamp = System.currentTimeMillis(),
            sourceDocument = "System",
            documentDate = System.currentTimeMillis(),
            allegationId = null,
            allegationElementName = null,
            category = "Placeholder",
            tags = emptyList(),
            commentary = null,
            linkedEvidenceIds = emptyList(),
            parentVideoId = null,
            entities = emptyMap(),
            transcriptEdits = emptyList(),
            fileSize = 0L,
            fileHash = null,
            isDuplicate = false
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.timeline).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                    )
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
        ) {
            // Optimization: remember the sorted list to avoid O(N log N) sorting on every recomposition.
            // If the input evidenceList reference changes (new data), this block re-runs.
            val itemsToDisplay: List<Evidence> = remember(evidenceList) {
                if (evidenceList.isEmpty()) {
                    listOf(placeholderEvidenceItem)
                } else {
                    evidenceList.sortedBy { it.documentDate }
                }
            }

            // Render the timeline using JetLime.
            JetLimeColumn(
                itemsList = ItemsList(itemsToDisplay),
                key = { _, item -> item.id }, // Stable key based on Evidence ID for efficient updates.
                style = JetLimeDefaults.columnStyle()
            ) { _, item, position ->
                JetLimeExtendedEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        pointType = if (evidenceList.isEmpty() && item.id == 0) EventPointType.Default else EventPointType.filled()
                    ),
                    // Additional content (left/right of the timeline line) showing date and type icon.
                    additionalContent = if (evidenceList.isNotEmpty() || item.id != 0) {
                        @Composable {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                                val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.documentDate))
                                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.documentDate))
                                Text(text = date, style = MaterialTheme.typography.labelMedium)
                                Text(text = time, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = getIconForType(item.type),
                                    contentDescription = item.type,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else { {} }
                ) {
                    if (evidenceList.isEmpty() && item.id == 0) { // Render placeholder card
                        PlaceholderExtendedEvent()
                    } else { // Render actual evidence card
                        ExtendedEvent(evidence = item)
                    }
                }
            }
        }
    }
}

/**
 * Returns the appropriate Material Icon for a given evidence type string.
 */
private fun getIconForType(type: String): ImageVector {
    return when (type.lowercase()) {
        "text" -> Icons.AutoMirrored.Filled.Message
        "image" -> Icons.Default.Image
        "video" -> Icons.Default.Videocam
        "audio" -> Icons.Default.Audiotrack
        else -> Icons.AutoMirrored.Filled.Article // Fallback icon
    }
}
