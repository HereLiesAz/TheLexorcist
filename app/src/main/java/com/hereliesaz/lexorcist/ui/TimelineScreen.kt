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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel

@OptIn(ExperimentalComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(evidenceViewModel: EvidenceViewModel) {
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()
    var selectedEvidence by remember { mutableStateOf<Evidence?>(null) }
    var linkingMode by remember { mutableStateOf(false) }
    var linkingFromId by remember { mutableStateOf<Int?>(null) }
    val evidenceCardPositions = remember { mutableStateMapOf<Int, Offset>() }

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Case Timeline", style = MaterialTheme.typography.headlineMedium)
        if (linkingMode) {
            Text("Linking mode enabled. Tap another evidence to link.", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (evidenceList.isEmpty()) {
            Text("No evidence found for this case.")
        } else {
            Box {
                JetLimeColumn(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    itemsList = ItemsList(evidenceList),
                    key = { _, item -> item.id },
                ) { _, item, position ->
                    JetLimeExtendedEvent(
                        style = JetLimeEventDefaults.eventStyle(
                            position = position,
                            pointColor = MaterialTheme.colorScheme.primary,
                            pointAnimation = JetLimeEventDefaults.pointAnimation()
                        ),
                        additionalContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(item.documentDate)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(item.documentDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (linkingMode) {
                                            linkingFromId?.let { fromId ->
                                                evidenceViewModel.linkEvidence(fromId, item.id)
                                                linkingMode = false
                                                linkingFromId = null
                                            }
                                        } else {
                                            selectedEvidence = item
                                        }
                                    },
                                    onLongClick = {
                                        linkingMode = true
                                        linkingFromId = item.id
                                    }
                                )
                                .onGloballyPositioned { coordinates ->
                                    val size = coordinates.size
                                    evidenceCardPositions[item.id] = coordinates.positionInParent() + Offset(size.width / 2f, size.height / 2f)
                                }
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
                Canvas(modifier = Modifier.fillMaxSize()) {
                    evidenceList.forEach { evidence ->
                        evidence.linkedEvidenceIds.forEach { linkedId ->
                            val from = evidenceCardPositions[evidence.id]
                            val to = evidenceCardPositions[linkedId]
                            if (from != null && to != null) {
                                drawLine(
                                    color = Color.Gray,
                                    start = from,
                                    end = to,
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
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
        title = { Text(evidence.sourceDocument) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Content: ${evidence.content}", style = MaterialTheme.typography.bodyMedium)
                Text("Category: ${evidence.category}", style = MaterialTheme.typography.bodyMedium)
                Text("Tags: ${evidence.tags.joinToString().ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodyMedium) // Corrected typo
                Text(
                    text = "Date: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.getDefault()
                        ).format(Date(evidence.documentDate))
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
