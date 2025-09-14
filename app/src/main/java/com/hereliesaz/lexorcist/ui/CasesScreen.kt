package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
// import androidx.compose.material3.CircularProgressIndicator // Duplicate import removed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(
    caseViewModel: CaseViewModel,
    navController: NavController,
) {
    val casesState by caseViewModel.cases.collectAsState()
    val sortOrder by caseViewModel.sortOrder.collectAsState() // Retain for logic if sort is re-added
    val searchQuery by caseViewModel.searchQuery.collectAsState()
    var longPressedCase by remember { mutableStateOf<Case?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var currentSortOrderState by remember { mutableStateOf(sortOrder) }

    val unarchivedCases =
        remember(casesState, searchQuery) {
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
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.cases).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateCaseDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_new_case))
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding) // Apply padding from Scaffold (e.g., for FAB)
                    .statusBarsPadding() // Add status bar padding directly to the content
                    .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { caseViewModel.onSearchQueryChanged(it) },
                label = { Text(stringResource(R.string.search)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                // Padding for the search field
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search)) },
                singleLine = true,
            )

            // val isLoading by caseViewModel.isLoading.collectAsState()
            // if (isLoading) {
            // Column(
            // modifier = Modifier.fillMaxSize(),
            // horizontalAlignment = Alignment.CenterHorizontally,
            // verticalArrangement = Arrangement.Center
            // ) {
            // CircularProgressIndicator()
            // }
            // } else
            if (unarchivedCases.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val noCasesText =
                        if (searchQuery.isNotBlank()) {
                            stringResource(R.string.no_cases_match_search)
                        } else {
                            stringResource(R.string.no_cases_found_line1) + "\n" +
                                stringResource(R.string.no_cases_found_line2)
                        }
                    Text(
                        text = noCasesText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues( // Adjust padding as needed now that topBar is gone
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp, // Adjusted top padding
                            bottom = 16.dp,
                        ),
                ) {
                    items(unarchivedCases, key = { it.id }) { case ->
                        CaseItem(
                            case = case,
                            isLongPressed = longPressedCase == case,
                            onLongPress = { longPressedCase = case },
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
                            },
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
            title = { Text(stringResource(R.string.delete_case) + ": ${longPressedCase?.name ?: ""}") },
            text = { Text(stringResource(R.string.delete_case_confirmation, longPressedCase?.name ?: "")) },
            confirmButton = {
                Button(onClick = {
                    longPressedCase?.let { caseViewModel.deleteCaseWithRepository(it) }
                    showDeleteConfirmDialog = false
                    longPressedCase = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteConfirmDialog = false
                    longPressedCase = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showCreateCaseDialog) {
        CreateCaseDialog(
            caseViewModel = caseViewModel,
            navController = navController,
            onDismiss = { showCreateCaseDialog = false },
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
    onClick: () -> Unit,
) {
    val backgroundColor = if (isLongPressed) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val contentColor = if (isLongPressed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress(case) },
                        onTap = { onClick() },
                    )
                },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = backgroundColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLongPressed) 4.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = case.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (isLongPressed) {
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
