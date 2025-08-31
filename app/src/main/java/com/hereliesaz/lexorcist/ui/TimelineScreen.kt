package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.JetLimeExtendedEvent
import com.pushpal.jetlime.ItemsList
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    navController: NavController,
    evidenceViewModel: EvidenceViewModel
) {
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()
    var selectedEvidence by remember { mutableStateOf<Evidence?>(null) }
    var showEnlargedImageDialog by remember { mutableStateOf(false) }
    var evidenceToEnlarge by remember { mutableStateOf<Evidence?>(null) }
    var linkingMode by remember { mutableStateOf(false) }
    var linkingFromId by remember { mutableStateOf<Int?>(null) }
    val evidenceCardPositions = remember { mutableStateMapOf<Int, Offset>() }

    // State for filters
    var searchQuery by remember { mutableStateOf("") }
    val allCategories = remember(evidenceList) { evidenceList.map { it.category }.distinct() }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filteredEvidenceList by remember(evidenceList, searchQuery, selectedCategories) {
        derivedStateOf {
            evidenceList.filter { evidence ->
                val matchesSearch = if (searchQuery.isNotBlank()) {
                    evidence.content.contains(searchQuery, ignoreCase = true) ||
                            (evidence.sourceDocument.contains(searchQuery, ignoreCase = true)) ||
                            evidence.tags.any { it.contains(searchQuery, ignoreCase = true) }
                } else {
                    true
                }

                val matchesCategory = if (selectedCategories.isNotEmpty()) {
                    selectedCategories.contains(evidence.category)
                } else {
                    true
                }

                matchesSearch && matchesCategory
            }
        }
    }

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Case Timeline", style = MaterialTheme.typography.headlineMedium)
        if (linkingMode) {
            Text("Linking mode enabled. Tap another evidence to link.", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Filter Controls
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Timeline...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Filter by Category:", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategories) { category ->
                    FilterChip(
                        selected = selectedCategories.contains(category),
                        onClick = {
                            selectedCategories = if (selectedCategories.contains(category)) {
                                selectedCategories - category
                            } else {
                                selectedCategories + category
                            }
                        },
                        label = { Text(category.ifBlank { "Uncategorized" }) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredEvidenceList.isEmpty()) {
            Text("No evidence found matching your criteria.")
        } else {
            Box {
                JetLimeColumn(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    itemsList = ItemsList(filteredEvidenceList),
                    key = { _, item -> item.id },
                ) { _, item, position ->
                    JetLimeExtendedEvent(
                        style = JetLimeEventDefaults.eventStyle(
                            position = position,
                            pointColor = getColorForCategory(item.category),
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
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getIconForCategory(item.category),
                                    contentDescription = "Category Icon",
                                    tint = getColorForCategory(item.category),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(
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
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
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
                                     if (item.category in listOf("Image", "OCR Image") && item.sourceDocument.startsWith("content://") == true) {
                                        androidx.compose.foundation.Image(
                                            painter = rememberAsyncImagePainter(model = item.sourceDocument.toUri()),
                                            contentDescription = "Evidence Thumbnail",
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(MaterialTheme.shapes.small)
                                                .clickable {
                                                    evidenceToEnlarge = item
                                                    showEnlargedImageDialog = true
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
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

    if (showEnlargedImageDialog && evidenceToEnlarge != null) {
        EnlargedImageDialog(
            evidence = evidenceToEnlarge!!,
            onDismiss = { showEnlargedImageDialog = false },
            onNavigate = {
                showEnlargedImageDialog = false
                navController.navigate("evidence_details/${evidenceToEnlarge!!.id}")
            }
        )
    }
}

@Composable
fun EnlargedImageDialog(
    evidence: Evidence,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(evidence.sourceDocument) },
        text = {
            Column {
                androidx.compose.foundation.Image(
                    painter = rememberAsyncImagePainter(model = evidence.sourceDocument.toUri()),
                    contentDescription = "Enlarged Evidence Thumbnail",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigate) {
                    Text("View Details")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun EvidenceDetailsDialog(evidence: Evidence, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Evidence Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Source: ${evidence.sourceDocument}", fontWeight = FontWeight.Bold)
                Text(evidence.content)
                Text("Category: ${evidence.category}", style = MaterialTheme.typography.bodySmall)
                Text("Tags: ${evidence.tags.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun getColorForCategory(category: String?): Color {
    return when (category?.lowercase(Locale.getDefault())) {
        "email", "sms", "message" -> Color(0xFF4285F4) // Blue
        "image", "video", "ocr image" -> Color(0xFF34A853) // Green
        "audio" -> Color(0xFFFABB05) // Yellow
        "document", "text file", "pdf file", "spreadsheet file" -> Color(0xFFEA4335) // Red
        "location" -> Color(0xFFFF6D00) // Orange
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun getIconForCategory(category: String?): ImageVector {
    return when (category?.lowercase(Locale.getDefault())) {
        "email" -> Icons.Default.Mail
        "sms", "message" -> Icons.AutoMirrored.Filled.Message
        "image", "ocr image" -> Icons.Default.Image
        "video" -> Icons.Default.Videocam
        "audio" -> Icons.Default.Audiotrack
        "document", "text file", "pdf file", "spreadsheet file" -> Icons.AutoMirrored.Filled.Article
        "location" -> Icons.Default.LocationOn
        else -> Icons.Default.Work
    }
}
