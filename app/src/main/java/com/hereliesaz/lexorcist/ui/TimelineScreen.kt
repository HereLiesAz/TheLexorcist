package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence // Changed import to data.Evidence
import com.hereliesaz.lexorcist.ui.components.ExtendedEvent
import com.hereliesaz.lexorcist.ui.components.PlaceholderExtendedEvent
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import io.github.pushpalroy.jetlime.ItemsList
import io.github.pushpalroy.jetlime.JetLimeColumn
import io.github.pushpalroy.jetlime.JetLimeDefaults
import io.github.pushpalroy.jetlime.JetLimeEventDefaults
import io.github.pushpalroy.jetlime.JetLimeExtendedEvent
import io.github.pushpalroy.jetlime.models.EventPointType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    caseViewModel: CaseViewModel = hiltViewModel()
) {
    val evidenceList by caseViewModel.selectedCaseEvidenceList.collectAsState()

    // Define a placeholder Evidence object for when the list is empty
    val placeholderEvidenceItem = Evidence(
        id = 0, // Unique placeholder ID
        caseId = 0L,
        spreadsheetId = "placeholder_case",
        type = "placeholder", // Handled by getIconForType default
        content = "No evidence items yet. Add some evidence to see the timeline.",
        formattedContent = null,
        mediaUri = null,
        timestamp = System.currentTimeMillis(),
        sourceDocument = "System",
        documentDate = System.currentTimeMillis(), // Needs a value for the timeline item
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
            val itemsToDisplay = if (evidenceList.isEmpty()) {
                listOf(placeholderEvidenceItem)
            } else {
                evidenceList.sortedBy { it.documentDate }
            }

            JetLimeColumn(
                itemsList = ItemsList(itemsToDisplay),
                key = { _, item -> item.id },
                style = JetLimeDefaults.columnStyle()
            ) { _, item, position -> // item is of type Evidence (from data package)
                JetLimeExtendedEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        pointType = if (evidenceList.isEmpty()) EventPointType.EMPTY else EventPointType.FILLED
                    ),
                    additionalContent = if (evidenceList.isNotEmpty()) {
                        @Composable {
                            // Date and Icon on the left
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
                    } else null
                ) {
                    // The main content card on the right
                    if (evidenceList.isEmpty()) {
                        PlaceholderExtendedEvent() // This Composable displays placeholder information
                    } else {
                        ExtendedEvent(evidence = item) // item is Evidence
                    }
            if (evidenceList.isEmpty()) {
                JetLimeColumn(
                    itemsList = ItemsList(listOf("placeholder")),
                    style = JetLimeDefaults.columnStyle(
                    )
                ) { _, _, _ ->
                    JetLimeExtendedEvent(
                        style = JetLimeEventDefaults.eventStyle(
                            pointType = EventPointType.EMPTY
                        )
                    ) {
                        PlaceholderExtendedEvent()
                    }
                }
            } else {
                JetLimeColumn(
                    itemsList = ItemsList(evidenceList.sortedBy { it.documentDate }),
                    key = { _, item -> item.id },
                    style = JetLimeDefaults.columnStyle(
                    )
                ) { _, evidence, position ->
                    JetLimeExtendedEvent(
                        style = JetLimeEventDefaults.eventStyle(position = position),
                        additionalContent = {
                            // Date and Icon on the left
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                                val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(evidence.documentDate))
                                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(evidence.documentDate))
                                Text(text = date, style = MaterialTheme.typography.labelMedium)
                                Text(text = time, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = getIconForType(evidence.type),
                                    contentDescription = evidence.type,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    ) {
                        // The main content card on the right
                        ExtendedEvent(evidence = evidence)
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
        // "placeholder" type will fall into else
        else -> Icons.AutoMirrored.Filled.Article
    }
}}}

