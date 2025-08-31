package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel

@Composable
fun AddonsBrowserScreen(
    viewModel: AddonsBrowserViewModel = viewModel(),
    onShare: () -> Unit
) {
    val scripts by viewModel.scripts.collectAsState()
    val templates by viewModel.templates.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onShare) {
                Icon(Icons.Default.Add, contentDescription = "Share")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Text("Scripts")
            LazyColumn {
                items(scripts) { script ->
                    AddonItem(
                        name = script.name,
                        description = script.description,
                        author = script.author,
                        rating = script.rating.toFloat(), // Converted Double to Float
                        onRate = { rating ->
                            viewModel.rateAddon(script.id, rating, "Script")
                        }
                    )
                }
            }
            Text("Templates")
            LazyColumn {
                items(templates) { template ->
                    AddonItem(
                        name = template.name,
                        description = template.description,
                        author = template.author,
                        rating = template.rating.toFloat(), // Converted Double to Float
                        onRate = { rating ->
                            viewModel.rateAddon(template.id, rating, "Template")
                        }
                    )
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
    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name)
            Text(text = "by $author")
            Text(text = description)
            RatingBar(rating = rating, onRate = onRate)
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRate: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
