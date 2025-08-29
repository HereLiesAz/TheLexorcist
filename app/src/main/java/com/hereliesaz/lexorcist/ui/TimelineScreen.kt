package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.pushpal.jetlime.EventPointType
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeEvent
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.ItemsList
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A screen that displays a timeline of evidence for a selected case.
 *
 * @param viewModel The [MainViewModel] that holds the state for this screen.
 */
@Composable
fun TimelineScreen(viewModel: MainViewModel) {
    val evidenceList by viewModel.selectedCaseEvidenceList.collectAsState()
    var selectedEvidence by remember { mutableStateOf<Evidence?>(null) }

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Case Timeline", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (evidenceList.isEmpty()) {
            Text("No evidence found for this case.")
        } else {
            JetLimeColumn(
                modifier = Modifier.padding(horizontal = 12.dp),
                itemsList = ItemsList(evidenceList),
                key = { _, item -> item.id },
            ) { _, item, position ->
                JetLimeEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        pointType = EventPointType.Filled,
                        pointColor = MaterialTheme.colorScheme.primary,
                        pointAnimation = JetLimeEventDefaults.pointAnimation(),
                        pointIcon = getIconForCategory(item.category)
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEvidence = item }
                            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.sourceDocument,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm",
                                    Locale.getDefault()
                                ).format(item.documentDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3
                            )
                            if (item.category.isNotBlank()) {
                                Text(
                                    text = "Category: ${item.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (item.tags.isNotEmpty()) {
                                Text(
                                    text = "Tags: ${item.tags.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedEvidence?.let {
        EvidenceDetailsDialog(evidence = it, onDismiss = { selectedEvidence = null })
    }
}

/**
 * Returns an [ImageVector] based on the evidence category.
 *
 * @param category The category of the evidence.
 * @return An [ImageVector] that represents the category.
 */
@Composable
fun getIconForCategory(category: String?): ImageVector {
    return when (category?.lowercase()) {
        "email" -> Icons.Default.Mail
        "message" -> Icons.Default.Message
        "image" -> Icons.Default.Image
        "video" -> Icons.Default.Videocam
        "audio" -> Icons.Default.Audiotrack
        "document" -> Icons.Default.Article
        "location" -> Icons.Default.LocationOn
        "call" -> Icons.Default.Phone
        else -> Icons.Default.Work
    }
}

/**
 * A dialog that shows the details of a selected piece of evidence.
 *
 * @param evidence The evidence to display.
 * @param onDismiss The action to perform when the dialog is dismissed.
 */
@Composable
fun EvidenceDetailsDialog(evidence: Evidence, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(evidence.sourceDocument) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Content: ${evidence.content}", style = MaterialTheme.typography.bodyMedium)
                Text("Category: ${evidence.category ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text("Tags: ${evidence.tags?.joinToString() ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Date: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.getDefault()
                        ).format(evidence.documentDate)
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
