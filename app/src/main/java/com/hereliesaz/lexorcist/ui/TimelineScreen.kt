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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeApi::class)
@Composable
fun TimelineScreen(
    caseViewModel: CaseViewModel = hiltViewModel()
) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()

    val placeholderEvidenceItem = Evidence(
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
            val itemsToDisplay: List<Evidence> = if (evidenceList.isEmpty()) {
                listOf(placeholderEvidenceItem)
            } else {
                evidenceList.sortedBy { it.documentDate }
            }

            JetLimeColumn(
                itemsList = ItemsList(itemsToDisplay),
                key = { _, item -> item.id }, // item is Evidence
                style = JetLimeDefaults.columnStyle()
            ) { _, item, position -> // item is Evidence, position is Int
                JetLimeExtendedEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        pointType = if (evidenceList.isEmpty() && item.id == 0) EventPointType.Default else EventPointType.filled()
                    ),
                    additionalContent = if (evidenceList.isNotEmpty() || item.id != 0) { // Show for actual evidence
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
                    } else { {}
                        // Provide an empty composable when there's no additional content
                    }
                ) {
                    if (evidenceList.isEmpty() && item.id == 0) { // True placeholder
                        PlaceholderExtendedEvent()
                    } else { // Actual evidence item
                        ExtendedEvent(evidence = item)
                    }
                }
            }
        }
    }
}

private fun getIconForType(type: String): ImageVector {
    return when (type.lowercase()) {
        "text" -> Icons.AutoMirrored.Filled.Message
        "image" -> Icons.Default.Image
        "video" -> Icons.Default.Videocam
        "audio" -> Icons.Default.Audiotrack
        else -> Icons.AutoMirrored.Filled.Article // Default for "placeholder" or other types
    }
}
