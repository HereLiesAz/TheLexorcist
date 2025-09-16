package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Corrected import
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.viewmodel.AllegationSortType
import com.hereliesaz.lexorcist.viewmodel.MasterAllegationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllegationsScreen(viewModel: MasterAllegationsViewModel = hiltViewModel()) {
    val allegations by viewModel.allegations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedAllegations by viewModel.selectedAllegations.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDetailsDialog by remember { mutableStateOf<MasterAllegation?>(null) }

    val groupedAllegations = if (sortType == AllegationSortType.TYPE) allegations.groupBy { it.type } else null
    val groupedByCategory = if (sortType == AllegationSortType.CATEGORY) allegations.groupBy { it.category } else null

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.allegations)) })
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            horizontalAlignment = Alignment.End
        ) {
            if (selectedAllegations.isNotEmpty()) {
                Text(
                    text = "Selected Allegations:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 16.dp),
                ) {
                    items(selectedAllegations.toList()) { allegation ->
                        Text(text = allegation.name)
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text(stringResource(R.string.search)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(onSearch = {
                        keyboardController?.hide()
                    }),
            )

            SortDropdown(
                sortType = sortType,
                onSortChange = { viewModel.onSortTypeChanged(it) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (groupedAllegations != null) {
                    groupedAllegations.forEach { (type, allegationsForType) ->
                        item {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        val allegationsByCategory = allegationsForType.groupBy { it.category }
                        allegationsByCategory.forEach { (category, allegationList) ->
                            item {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp),
                                )
                            }
                            items(allegationList) { allegation ->
                                AllegationItem(
                                    allegation = allegation,
                                    isSelected = selectedAllegations.contains(allegation),
                                    onToggleSelection = { viewModel.toggleAllegationSelection(allegation) },
                                    onLongPress = { showDetailsDialog = it },
                                )
                            }
                        }
                    }
                } else if (groupedByCategory != null) {
                    groupedByCategory.forEach { (category, allegationList) ->
                        item {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        items(allegationList) { allegation ->
                            AllegationItem(
                                allegation = allegation,
                                isSelected = selectedAllegations.contains(allegation),
                                onToggleSelection = { viewModel.toggleAllegationSelection(allegation) },
                                onLongPress = { showDetailsDialog = it },
                            )
                        }
                    }
                } else {
                    items(allegations) { allegation ->
                        AllegationItem(
                            allegation = allegation,
                            isSelected = selectedAllegations.contains(allegation),
                            onToggleSelection = { viewModel.toggleAllegationSelection(allegation) },
                            onLongPress = { showDetailsDialog = it },
                        )
                    }
                }
            }

            if (showDetailsDialog != null) {
                AlertDialog(
                    onDismissRequest = { showDetailsDialog = null },
                    title = { Text(showDetailsDialog!!.name) },
                    text = { Text(showDetailsDialog!!.description) },
                    confirmButton = {
                        LexorcistOutlinedButton(onClick = { showDetailsDialog = null }, text = "OK")
                    },
                )
            }
        }
    }
}

@Composable
fun SortDropdown(
    sortType: AllegationSortType,
    onSortChange: (AllegationSortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Sort by: $sortType")
        Spacer(modifier = Modifier.height(8.dp))
        LexorcistOutlinedButton(onClick = { expanded = true }, text = "Change")
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Type") },
                onClick = {
                    onSortChange(AllegationSortType.TYPE)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Category") },
                onClick = {
                    onSortChange(AllegationSortType.CATEGORY)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Name") },
                onClick = {
                    onSortChange(AllegationSortType.NAME)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Court Level") },
                onClick = {
                    onSortChange(AllegationSortType.COURT_LEVEL)
                    expanded = false
                },
            )
        }
    }
}

@Composable
fun AllegationItem(
    allegation: MasterAllegation,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: (MasterAllegation) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onToggleSelection() },
                        onLongPress = { onLongPress(allegation) },
                    )
                },
        colors =
            CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = allegation.name, fontWeight = FontWeight.Bold)
            Text(text = allegation.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
