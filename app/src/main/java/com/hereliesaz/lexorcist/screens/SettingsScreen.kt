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
import com.hereliesaz.lexorcist.db.Filter

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val filters by viewModel.filters.collectAsState()
    var showAddFilterDialog by remember { mutableStateOf(false) }
    var filterName by remember { mutableStateOf("") }
    var filterValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
            .padding(16.dp)
    ) {
        Text("Settings Screen")
        // Text("Filters", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        // Spacer(modifier = Modifier.height(16.dp))
        // LazyColumn(modifier = Modifier.weight(1f)) {
        //     items(filters) { filter ->
        //         FilterItem(filter = filter)
        //     }
        // }
        // Spacer(modifier = Modifier.height(16.dp))
        // Button(onClick = { showAddFilterDialog = true }) {
        //     Text("Add New Filter")
        // }
        Text("Filters", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filters) { filter ->
                FilterItem(filter = filter)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showAddFilterDialog = true }) {
            Text("Add New Filter")
        }
    }

    // if (showAddFilterDialog) {
    //     androidx.compose.material3.AlertDialog(
    //         onDismissRequest = { showAddFilterDialog = false },
    //         title = { Text("New Filter") },
    //         text = {
    //             Column {
    //                 OutlinedTextField(
    //                     value = filterName,
    //                     onValueChange = { filterName = it },
    //                     label = { Text("Filter Name") },
    //                     singleLine = true
    //                 )
    //                 Spacer(modifier = Modifier.height(8.dp))
    //                 OutlinedTextField(
    //                     value = filterValue,
    //                     onValueChange = { filterValue = it },
    //                     label = { Text("Filter Value") },
    //                     singleLine = true
    //                 )
    //             }
    //         },
    //         confirmButton = {
    //             Button(
    //                 onClick = {
    //                     if (filterName.isNotBlank() && filterValue.isNotBlank()) {
    //                         viewModel.addFilter(filterName, filterValue)
    //                         showAddFilterDialog = false
    //                         filterName = ""
    //                         filterValue = ""
    //                     }
    //                 }
    //             ) {
    //                 Text("Add")
    //             }
    //         },
    //         dismissButton = {
    //             Button(onClick = { showAddFilterDialog = false }) {
    //                 Text("Cancel")
    //             }
    //         }
    //     )
    // }
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
                        if (filterName.isNotBlank() && filterValue.isNotBlank()) {
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
fun FilterItem(filter: Filter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = filter.name,
            modifier = Modifier.weight(1f)
        )
        Text(text = filter.value)
    }
}
