package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.data.SortOrder
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(caseViewModel: CaseViewModel) {
    val cases by caseViewModel.cases.collectAsState()
    val sortOrder by caseViewModel.sortOrder.collectAsState()
    val searchQuery by caseViewModel.searchQuery.collectAsState() // Assuming this is provided by ViewModel
    var longPressedCase by remember { mutableStateOf<Case?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var sortOrderState by remember { mutableStateOf(sortOrder) }
    var lastSortTap by remember { mutableStateOf(0L) }

    LaunchedEffect(sortOrderState) {
        if (System.currentTimeMillis() - lastSortTap > 1000) {
            caseViewModel.onSortOrderChange(sortOrderState)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateCaseDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Case")
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.End
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(), // LazyColumn takes all space in the Column
                    contentPadding = PaddingValues(top = halfScreenHeight, start = 16.dp, end = 16.dp) // Apply general padding here
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), // Padding for spacing below search bar
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { caseViewModel.onSearchQueryChanged(it) },
                                label = { Text("Search Cases") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") }
                            )
                            IconButton(onClick = {
                                lastSortTap = System.currentTimeMillis()
                                sortOrderState = when (sortOrderState) {
                                    SortOrder.DATE_DESC -> SortOrder.DATE_ASC
                                    SortOrder.DATE_ASC -> SortOrder.NAME_DESC
                                    SortOrder.NAME_DESC -> SortOrder.NAME_ASC
                                    SortOrder.NAME_ASC -> SortOrder.DATE_DESC
                                }
                            }) {
                                Icon(Icons.Filled.Sort, contentDescription = "Sort")
                            }
                        }
                    }

                    items(cases.filter { !it.isArchived }) { case ->
                        CaseItem(
                            case = case,
                            isLongPressed = longPressedCase == case,
                            onLongPress = { longPressedCase = it },
                            onDelete = {
                                longPressedCase = case
                                showDeleteConfirmDialog = true
                            },
                            onArchive = {
                                longPressedCase?.let { caseViewModel.archiveCaseWithRepository(it) }
                                longPressedCase = null
                            },
                            onCancel = { longPressedCase = null },
                            onClick = {
                                if (longPressedCase == null) {
                                    // TODO: Navigate to case details
                                } else {
                                    longPressedCase = null // Cancel long press state
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Case") },
                text = { Text("Are you sure you want to delete this case?") },
                confirmButton = {
                    TextButton(onClick = {
                        longPressedCase?.let { caseViewModel.deleteCaseWithRepository(it) }
                        showDeleteConfirmDialog = false
                        longPressedCase = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCreateCaseDialog) {
            CreateCaseDialog(
                caseViewModel = caseViewModel,
                onDismiss = { showCreateCaseDialog = false }
            )
        }
    }
}

@Composable
fun CaseItem(
    case: Case,
    isLongPressed: Boolean,
    onLongPress: (Case) -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit
) {
    val backgroundColor = if (isLongPressed) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor) // Apply background before pointerInput for better visual feedback
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress(case) },
                    onTap = { onClick() }
                )
            }
            .padding(vertical = 8.dp) // Vertical padding for each item, horizontal is handled by LazyColumn's contentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = case.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 8.dp) // Give weight to text and padding
            )
            if (isLongPressed) {
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = onArchive) {
                        Icon(Icons.Filled.Archive, contentDescription = "Archive")
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Cancel")
                    }
                }
            }
        }
    }
}
