package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort // Changed import
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
// import androidx.compose.material.icons.filled.Sort // Original import removed or commented
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.SortOrder
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(caseViewModel: CaseViewModel) {
    val casesState by caseViewModel.cases.collectAsState()
    val sortOrder by caseViewModel.sortOrder.collectAsState()
    val searchQuery by caseViewModel.searchQuery.collectAsState()
    var longPressedCase by remember { mutableStateOf<Case?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var currentSortOrderState by remember { mutableStateOf(sortOrder) }

    val unarchivedCases = remember(casesState, searchQuery) {
        casesState.filter { case ->
            !case.isArchived &&
            (searchQuery.isBlank() || case.name.contains(searchQuery, ignoreCase = true))
        }
    }

    LaunchedEffect(currentSortOrderState) {
        caseViewModel.onSortOrderChange(currentSortOrderState)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End 
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { caseViewModel.onSearchQueryChanged(it) },
                    label = { Text(stringResource(R.string.search).uppercase(Locale.getDefault())) }, // ALL CAPS
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    currentSortOrderState = when (currentSortOrderState) {
                        SortOrder.DATE_DESC -> SortOrder.DATE_ASC
                        SortOrder.DATE_ASC -> SortOrder.NAME_DESC
                        SortOrder.NAME_DESC -> SortOrder.NAME_ASC
                        SortOrder.NAME_ASC -> SortOrder.DATE_DESC
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort).uppercase(Locale.getDefault())) // ALL CAPS for accessibility if desired, though usually not for CD
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateCaseDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_new_case).uppercase(Locale.getDefault())) // ALL CAPS
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding -> 
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding) 
                .fillMaxSize()         
        ) {
            val contentAreaHeight = this.maxHeight 
            val scrollOffset = contentAreaHeight / 2

            if (unarchivedCases.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()) 
                        .padding(horizontal = 16.dp),         
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Top 
                ) {
                    Spacer(Modifier.height(scrollOffset)) 
                    val noCasesText = if (searchQuery.isNotBlank()) {
                        stringResource(R.string.no_cases_match_search).uppercase(Locale.getDefault())
                    } else {
                        stringResource(R.string.no_cases_found_line1).uppercase(Locale.getDefault()) + "\n" +
                        stringResource(R.string.no_cases_found_line2).uppercase(Locale.getDefault())
                    }
                    Text(
                        text = noCasesText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = scrollOffset, 
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp 
                    )
                ) {
                    items(unarchivedCases, key = { it.id }) { case ->
                        CaseItem(
                            case = case,
                            isLongPressed = longPressedCase == case,
                            onLongPress = { longPressedCase = it },
                            onDelete = {
                                longPressedCase = case 
                                showDeleteConfirmDialog = true
                            },
                            onArchive = {
                                caseViewModel.archiveCaseWithRepository(case) 
                                longPressedCase = null 
                            },
                            onCancel = { longPressedCase = null },
                            onClick = {
                                if (longPressedCase == null) {
                                    caseViewModel.selectCase(case)
                                } else {
                                    longPressedCase = null 
                                }
                            }
                        )
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
            title = { Text((stringResource(R.string.delete_case) + ": ${longPressedCase?.name ?: ""}").uppercase(Locale.getDefault())) }, // ALL CAPS
            text = { Text(stringResource(R.string.delete_case_confirmation, longPressedCase?.name ?: "")) }, // Confirmation text usually not all caps
            confirmButton = {
                Button(onClick = {
                    longPressedCase?.let { caseViewModel.deleteCaseWithRepository(it) }
                    showDeleteConfirmDialog = false
                    longPressedCase = null
                }) {
                    Text(stringResource(R.string.delete).uppercase(Locale.getDefault())) // ALL CAPS
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    showDeleteConfirmDialog = false
                    longPressedCase = null
                }) {
                    Text(stringResource(R.string.cancel).uppercase(Locale.getDefault())) // ALL CAPS
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
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) { 
                Text(
                    text = case.name, // Case name is data, not a title, so not uppercased
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isLongPressed) {
                // Content descriptions for IconButtons are for accessibility, not typically uppercased.
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_case))
                }
                IconButton(onClick = onArchive) {
                    Icon(Icons.Filled.Archive, contentDescription = stringResource(R.string.archive))
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cancel))
                }
            }
        }
    }
}

// Placeholder for new string resources that would be added to strings.xml
// R.string.no_cases_match_search = "No cases match your search."
// R.string.no_cases_found_line1 = "No cases found."
// R.string.no_cases_found_line2 = "Create a new case or open an existing one."
