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
// Corrected import to use the combined ViewModel from the .viewmodel package
import com.hereliesaz.lexorcist.viewmodel.MainViewModel 
import com.hereliesaz.lexorcist.model.SheetFilter

@Composable
fun SettingsScreen(viewModel: MainViewModel) { // ViewModel type is now com.hereliesaz.lexorcist.viewmodel.MainViewModel
    // Use the renamed state flow for settings screen filters
    val filters by viewModel.settingScreenFilters.collectAsState()
    var showAddFilterDialog by remember { mutableStateOf(false) }
    var filterName by remember { mutableStateOf("") }
    var filterValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Settings Screen")
        Text("Filters (In-Memory for UI)", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(filters) { filter ->
                FilterItem(filter = filter)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showAddFilterDialog = true }) {
            Text("Add New UI Filter")
        }
    }

    if (showAddFilterDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddFilterDialog = false },
            title = { Text("New UI Filter") },
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
                        if (filterName.isNotBlank() || filterValue.isNotBlank()) {
                            // Use the renamed method for adding settings screen filters
                            viewModel.addSettingScreenFilter(filterName, filterValue)
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
fun FilterItem(filter: SheetFilter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(text = filter.name)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = filter.value)
    }
}
