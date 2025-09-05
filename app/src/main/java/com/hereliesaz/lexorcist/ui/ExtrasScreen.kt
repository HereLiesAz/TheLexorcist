package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasScreen(
    viewModel: AddonsBrowserViewModel = hiltViewModel(),
    onShare: () -> Unit
) {
    val scripts by viewModel.scripts.collectAsState()
    val templates by viewModel.templates.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.extras).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShare) {
                Icon(Icons.Default.Add, contentDescription = "Share")
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2 // REMOVED

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Make the whole content area scrollable
                    .padding(horizontal = 16.dp), // Add horizontal padding for content within the Column
                horizontalAlignment = Alignment.End // Right-align children of this Column
            ) {
                // Spacer(modifier = Modifier.height(halfScreenHeight)) // REMOVED

                Text("Scripts", style = MaterialTheme.typography.headlineSmall)
                // Note: LazyColumn inside a verticalScroll can have performance issues if lists are very long.
                // For now, assuming lists are of manageable size or this is the desired scroll behavior.
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = 400.dp) // Example: Constrain height if needed, or let it wrap
                ) {
                    items(scripts) { script ->
                        AddonItem(
                            name = script.name,
                            description = script.description,
                            author = script.author,
                            rating = script.rating.toFloat(),
                            onRate = {
                                viewModel.rateAddon(script.id, it, "Script")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // Space between sections

                Text("Templates", style = MaterialTheme.typography.headlineSmall)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = 400.dp) // Example: Constrain height if needed
                ) {
                    items(templates) { template ->
                        AddonItem(
                            name = template.name,
                            description = template.description,
                            author = template.author,
                            rating = template.rating.toFloat(),
                            onRate = {
                                viewModel.rateAddon(template.id, it, "Template")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddonItem(
    name: String,
    description: String,
    author: String,
    rating: Float,
    onRate: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Card takes full width available from LazyColumn item
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(), // Fill width for internal alignment
            horizontalAlignment = Alignment.End // Right-align content within the Card
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(text = "by $author", style = MaterialTheme.typography.bodySmall)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            RatingBar(rating = rating, onRate = onRate)
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRate: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // The Row itself will be right-aligned by its parent Column in AddonItem.
        // Internal arrangement of stars is sequential.
    ) {
        (1..5).forEach { index ->
            IconButton(onClick = { onRate(index) }) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Rate $index",
                    tint = if (index <= rating) Color.Yellow else Color.Gray
                )
            }
        }
    }
}
