package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.JetLimeExtendedEvent
import com.pushpal.jetlime.ItemsList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(viewModel: MainViewModel) {
    val evidenceList by viewModel.selectedCaseEvidenceList.collectAsState()
    var selectedEvidence by remember { mutableStateOf<Evidence?>(null) }

    // State for filters
    var searchQuery by remember { mutableStateOf("") }
    val allCategories = remember(evidenceList) { evidenceList.map { it.category }.distinct() }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filteredEvidenceList by remember(evidenceList, searchQuery, selectedCategories) {
        derivedStateOf {
            evidenceList.filter { evidence ->
                val matchesSearch = if (searchQuery.isNotBlank()) {
                    evidence.content.contains(searchQuery, ignoreCase = true) ||
                    evidence.sourceDocument.contains(searchQuery, ignoreCase = true) ||
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
            JetLimeColumn(
                modifier = Modifier.padding(horizontal = 12.dp),
                itemsList = ItemsList(filteredEvidenceList),
                key = { _, item -> item.id },
            ) { _, item, position ->
                JetLimeExtendedEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        pointColor = getColorForCategory(item.category), // Color-coded point
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
                            .clickable { selectedEvidence = item }
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
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

// getIconForCategory and EvidenceDetailsDialog functions remain the same as before...
// (Make sure they are still in this file)
