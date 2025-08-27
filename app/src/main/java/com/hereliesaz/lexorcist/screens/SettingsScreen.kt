package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.MainViewModel
import com.hereliesaz.lexorcist.model.SheetFilter // Changed import from db.Filter to model.SheetFilter

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    // The type of filters will now be List<SheetFilter> from the ViewModel
    val filters by viewModel.filters.collectAsState()
    var showAddFilterDialog by remember { mutableStateOf(false) }
    var filterName by remember { mutableStateOf("") }
    var filterValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End, // Changed to End
        verticalArrangement = Arrangement.Center
    ) {
        Text("Settings Screen")
        Text("Filters", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth() // Ensure LazyColumn takes full width for item alignment
        ) {
            items(filters) { filter -> // filter is now SheetFilter
                FilterItem(filter = filter)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showAddFilterDialog = true }) {
            Text("Add New Filter")
        }
    }

    if (showAddFilterDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddFilterDialog = false },
            title = { Text("New Filter") },
            text = {
                Column {
                    OutlinedTextField(
                        value = filterName,
                        onValueChange = { filterName = it },
                        label = { Text("Filter Name") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = filterValue,
                        onValueChange = { filterValue = it },
                        label = { Text("Filter Value") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (filterName.isNotBlank() || filterValue.isNotBlank()) { // Allow adding if at least one field is not blank
                            viewModel.addFilter(filterName, filterValue)
                            showAddFilterDialog = false
                            filterName = ""
                            filterValue = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = { showAddFilterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FilterItem(filter: SheetFilter) { // Changed parameter type to SheetFilter
    Row(
        modifier = Modifier
            .fillMaxWidth() // Row fills width to allow content to be pushed to the end
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End // Align content to the End (right)
    ) {
        Text(
            text = filter.name,
            // modifier = Modifier.weight(1f) // Removed weight
        )
        Spacer(modifier = Modifier.width(8.dp)) // Add some space between name and value if desired
        Text(text = filter.value)
    }
}
