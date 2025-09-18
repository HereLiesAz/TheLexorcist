package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Updated import
import com.hereliesaz.lexorcist.data.SharedItem
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.ExtrasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasScreen(
    extrasViewModel: ExtrasViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onShare: () -> Unit,
) {
    val uiState by extrasViewModel.uiState.collectAsState()
    var showDetailsDialog by remember { mutableStateOf<SharedItem?>(null) }

    LaunchedEffect(key1 = Unit) {
        extrasViewModel.setAuthSource(authViewModel.signInState)
    }

    showDetailsDialog?.let { item ->
        ItemDetailsDialog(
            item = item,
            isAuthor = extrasViewModel.isAuthor(item),
            onDismiss = { showDetailsDialog = null },
            onDelete = {
                extrasViewModel.deleteItem(item)
                showDetailsDialog = null
            },
            onEdit = {
                println("Edit action for ${item.name}")
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extras", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShare) {
                Icon(Icons.Default.Add, contentDescription = "Share")
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = extrasViewModel::onSearchQueryChanged,
                label = { Text("Search Extras") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.items, key = { it.id }) { item ->
                        AddonItem(
                            name = item.name,
                            description = item.description,
                            author = item.author,
                            rating = item.rating.toFloat(),
                            onRate = { rating ->
                                val type = when (item) {
                                    is com.hereliesaz.lexorcist.data.ScriptItem -> "Script"
                                    is com.hereliesaz.lexorcist.data.TemplateItem -> "Template"
                                }
                                extrasViewModel.rateAddon(item.id, rating, type)
                            },
                            onClick = { showDetailsDialog = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDetailsDialog(
    item: SharedItem,
    isAuthor: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(text = item.name, style = MaterialTheme.typography.headlineSmall)
                Text(text = "by ${item.author}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    if (isAuthor) {
                        Button(onClick = onEdit, modifier = Modifier.padding(end = 8.dp)) {
                            Text("Edit")
                        }
                        Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Delete")
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onDismiss) {
                        Text("Close")
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
    onRate: (Int) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(text = "by $author", style = MaterialTheme.typography.bodySmall)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            RatingBar(rating = rating, onRate = onRate)
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRate: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..5).forEach { index ->
            IconButton(onClick = { onRate(index) }) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Rate $index",
                    tint = if (index <= rating) Color.Yellow else Color.Gray,
                )
            }
        }
    }
}
