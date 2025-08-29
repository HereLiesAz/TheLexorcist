package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
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
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
// import com.pushpal.jetlime.EventPointType // Removed import as it's causing unresolved references
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.JetLimeExtendedEvent
import com.pushpal.jetlime.ItemsList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalComposeApi::class)
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
                JetLimeExtendedEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        // pointType = EventPointType.POINT, // Removed pointType
                        pointColor = MaterialTheme.colorScheme.primary, 
                        pointAnimation = JetLimeEventDefaults.pointAnimation()
                        // pointIcon removed
                    ),
                    additionalContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(item.documentDate ?: System.currentTimeMillis())),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(item.documentDate ?: System.currentTimeMillis())),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End
                            )
                        }
                    }
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
                                text = item.sourceDocument ?: "Unknown Source",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3
                            )
                            if (item.category?.isNotBlank() == true) { 
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

    selectedEvidence?.let { evidence ->
        EvidenceDetailsDialog(evidence = evidence, onDismiss = { selectedEvidence = null })
    }
}

@Composable
fun getIconForCategory(category: String?): ImageVector {
    return when (category?.lowercase(Locale.getDefault())) {
        "email" -> Icons.Filled.Mail
        "message" -> Icons.AutoMirrored.Filled.Message
        "image" -> Icons.Filled.Image
        "video" -> Icons.Filled.Videocam
        "audio" -> Icons.Filled.Audiotrack
        "document" -> Icons.AutoMirrored.Filled.Article
        "location" -> Icons.Filled.LocationOn
        "call" -> Icons.Filled.Phone
        "ocr image" -> Icons.Filled.Image
        "text file" -> Icons.AutoMirrored.Filled.Article
        "pdf file" -> Icons.AutoMirrored.Filled.Article
        "spreadsheet file" -> Icons.AutoMirrored.Filled.Article
        "document file" -> Icons.AutoMirrored.Filled.Article
        "sms" -> Icons.AutoMirrored.Filled.Message
        "text upload" -> Icons.AutoMirrored.Filled.Article
        "local text" -> Icons.AutoMirrored.Filled.Article
        "drive_upload" -> Icons.Filled.Work
        "file" -> Icons.AutoMirrored.Filled.Article
        else -> Icons.Filled.Work
    }
}

@Composable
fun EvidenceDetailsDialog(evidence: Evidence, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(evidence.sourceDocument ?: "Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Content: ${evidence.content}", style = MaterialTheme.typography.bodyMedium)
                Text("Category: ${evidence.category ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text("Tags: ${evidence.tags.joinToString().ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodyMedium) // Corrected typo
                Text(
                    text = "Date: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.getDefault()
                        ).format(Date(evidence.documentDate ?: System.currentTimeMillis()))
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
