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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val casesState by caseViewModel.cases.collectAsState()
    val sortOrder by caseViewModel.sortOrder.collectAsState()
    val searchQuery by caseViewModel.searchQuery.collectAsState()
    var longPressedCase by remember { mutableStateOf<Case?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var currentSortOrderState by remember { mutableStateOf(sortOrder) } // Renamed to avoid conflict
    var lastSortTap by remember { mutableStateOf(0L) }

    // Filtered list of unarchived cases
    val unarchivedCases = remember(casesState, searchQuery) {
        casesState.filter { case ->
            !case.isArchived &&
            (searchQuery.isBlank() || case.name.contains(searchQuery, ignoreCase = true))
            // Add other search criteria if needed (e.g., case number, plaintiffs)
        }
    }

    LaunchedEffect(currentSortOrderState) {
        // Debounce or prevent rapid changes if necessary, though current logic might be fine
        caseViewModel.onSortOrderChange(currentSortOrderState)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding(), // Add padding for status bar if TopAppBar is not used
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End // Align content to the end
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { caseViewModel.onSearchQueryChanged(it) },
                    label = { Text("Search Cases") },
                    modifier = Modifier.weight(1f), // TextField takes available space
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    lastSortTap = System.currentTimeMillis() // For potential future debouncing
                    currentSortOrderState = when (currentSortOrderState) {
                        SortOrder.DATE_DESC -> SortOrder.DATE_ASC
                        SortOrder.DATE_ASC -> SortOrder.NAME_DESC
                        SortOrder.NAME_DESC -> SortOrder.NAME_ASC
                        SortOrder.NAME_ASC -> SortOrder.DATE_DESC
                    }
                }) {
                    Icon(Icons.Filled.Sort, contentDescription = "Sort Cases")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateCaseDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create New Case")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

            Column(
                modifier = Modifier.fillMaxSize(), // This Column fills the content area of Scaffold
                horizontalAlignment = Alignment.End // Right-align direct children
            ) {
                if (unarchivedCases.isEmpty()) {
                    // This inner Column is to group the Spacer and Text for vertical scrolling and right alignment
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Top
                    ){
                        Spacer(modifier = Modifier.height(halfScreenHeight))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No cases match your search." 
                                   else "No cases found. Create a new case or open an existing one.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(), // LazyColumn fills the space in the outer Column
                        contentPadding = PaddingValues(
                            top = halfScreenHeight, 
                            start = 16.dp, 
                            end = 16.dp, 
                            bottom = 16.dp // Add some bottom padding as well
                        )
                    ) {
                        items(unarchivedCases, key = { it.id }) { case ->
                            CaseItem(
                                case = case,
                                isLongPressed = longPressedCase == case,
                                onLongPress = { longPressedCase = it },
                                onDelete = {
                                    longPressedCase = case // Set for dialog context
                                    showDeleteConfirmDialog = true
                                },
                                onArchive = {
                                    caseViewModel.archiveCaseWithRepository(case) // Use current case from item
                                    longPressedCase = null // Reset long press state
                                },
                                onCancel = { longPressedCase = null },
                                onClick = {
                                    if (longPressedCase == null) {
                                        caseViewModel.selectCase(case)
                                        // TODO: Navigate to a specific case detail screen if needed,
                                        // or simply selecting is enough for other parts of the app to react.
                                    } else {
                                        longPressedCase = null // Cancel long press state if already active
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog && longPressedCase != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmDialog = false 
                longPressedCase = null
            },
            title = { Text("Delete Case: ${longPressedCase?.name ?: ""}") },
            text = { Text("Are you sure you want to permanently delete this case and all its associated data in Google Drive? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    longPressedCase?.let { caseViewModel.deleteCaseWithRepository(it) }
                    showDeleteConfirmDialog = false
                    longPressedCase = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    showDeleteConfirmDialog = false
                    longPressedCase = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // This uses the CreateCaseDialog from MainScreen.kt
    if (showCreateCaseDialog) {
        CreateCaseDialog(
            caseViewModel = caseViewModel,
            onDismiss = { showCreateCaseDialog = false }
            // "Open Existing Case" button will be added to CreateCaseDialog itself
        )
    }
}

@Composable
fun CaseItem(
    case: Case,
    isLongPressed: Boolean,
    onLongPress: (Case) -> Unit,
    onDelete: () -> Unit, // Changed from (Case) -> Unit to () -> Unit as context is set before dialog
    onArchive: () -> Unit, // Changed from (Case) -> Unit to () -> Unit
    onCancel: () -> Unit,
    onClick: () -> Unit
) {
    val backgroundColor = if (isLongPressed) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val contentColor = if (isLongPressed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress(case) },
                    onTap = { onClick() }
                )
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = backgroundColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isLongPressed) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            // horizontalArrangement = Arrangement.SpaceBetween, // Let Column with weight handle spacing
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) { 
                Text(
                    text = case.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth() // Text should fill width to be aligned by Column
                )
                // Add other case details here if needed, like date or plaintiff/defendant
                // Text(text = "Last Modified: ${case.lastModifiedTime}", style = MaterialTheme.typography.bodySmall)
            }
            if (isLongPressed) {
                // These buttons are already at the end due to the Column with weight(1f) for text
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Case")
                }
                IconButton(onClick = onArchive) {
                    Icon(Icons.Filled.Archive, contentDescription = "Archive Case")
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Cancel Actions") // Or use Close icon
                }
            }
        }
    }
}
