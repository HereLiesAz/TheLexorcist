package com.hereliesaz.lexorcist.ui

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.lexorcist.ui.components.AzAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Case
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import java.util.Locale

/**
 * Screen for displaying and managing the list of legal cases.
 *
 * Features:
 * - Search bar for filtering cases by name.
 * - LazyColumn for efficient list rendering.
 * - Long-press context menu on case items for Delete/Archive actions.
 * - Floating Action Button (FAB) to create new cases.
 * - Empty state handling with call-to-action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(
    caseViewModel: CaseViewModel,
    navController: NavController,
    mainViewModel: MainViewModel,
) {
    // Collect UI state from ViewModel flows.
    // Sorting and filtering are handled in the ViewModel, so we just observe the results here.
    val casesState by caseViewModel.cases.collectAsState()
    val sortOrder by caseViewModel.sortOrder.collectAsState()
    val searchQuery by caseViewModel.searchQuery.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()

    // Local UI state for dialogs and interaction modes.
    var longPressedCase by remember { mutableStateOf<Case?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCreateCaseDialog by remember { mutableStateOf(false) }
    var currentSortOrderState by remember { mutableStateOf(sortOrder) }

    // Filter unarchived cases. Search query filtering is already handled by the ViewModel.
    // Note: ViewModel filters archived cases already, so this variable name might be redundant but clarifies intent.
    val unarchivedCases = casesState

    // Load cases when the screen enters the composition.
    LaunchedEffect(Unit) {
        caseViewModel.loadCasesFromRepository()
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
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                    )
                },
            )
        },
        floatingActionButton = {
            // FAB for creating a new case.
            AzButton(
                onClick = { showCreateCaseDialog = true },
                text = "New"
            )
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .fillMaxSize(),
            horizontalAlignment = Alignment.End
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { caseViewModel.onSearchQueryChanged(it) },
                label = { Text(stringResource(R.string.search)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { caseViewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
            )

            val isLoading by caseViewModel.isLoading.collectAsState()

            // --- Content Loading / Empty States ---
            if (isLoading && unarchivedCases.isEmpty()) {
                // Show loader only if we have no data yet.
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AzLoad()
                }
            } else if (unarchivedCases.isEmpty()) {
                // Show Empty State with CTA if no cases found.
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
                        textAlign = TextAlign.Center
                    )
                    // Only show "New Case" button in empty state if we are NOT searching.
                    if (searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AzButton(
                            onClick = { showCreateCaseDialog = true },
                            text = stringResource(R.string.new_case_fab_text).uppercase(Locale.getDefault())
                        )
                    }
                }
            } else {
                // --- Case List ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp,
                        ),
                ) {
                    items(unarchivedCases, key = { it.id }) { case ->
                        CaseItem(
                            case = case,
                            isLongPressed = longPressedCase == case,
                            isSelected = selectedCase?.id == case.id,
                            onLongPress = {
                                Log.d("CasesScreen", "onLongPress triggered for case: ${case.name}")
                                longPressedCase = case
                            },
                            onDelete = {
                                longPressedCase = case
                                showDeleteConfirmDialog = true
                            },
                            onArchive = {
                                caseViewModel.archiveCaseWithRepository(case)
                                longPressedCase = null
                            },
                            onCancel = {
                                Log.d("CasesScreen", "onCancel triggered (clearing longPressedCase)")
                                longPressedCase = null
                            },
                            onClick = {
                                Log.d("CasesScreen", "onClick triggered for case: ${case.name}")
                                if (longPressedCase == null) {
                                    caseViewModel.selectCase(case)
                                } else {
                                    // If a context menu is open, tapping elsewhere closes it.
                                    longPressedCase = null
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showDeleteConfirmDialog && longPressedCase != null) {
        val deleteText = stringResource(R.string.delete)
        val cancelText = stringResource(R.string.cancel)
        AzAlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                longPressedCase = null
            },
            title = { Text(stringResource(R.string.delete_case) + ": ${longPressedCase?.name ?: ""}") },
            text = { Text(stringResource(R.string.delete_case_confirmation, longPressedCase?.name ?: "")) },
            confirmButton = {
                AzButton(
                    onClick = {
                        longPressedCase?.let { caseViewModel.deleteCaseWithRepository(it) }
                        showDeleteConfirmDialog = false
                        longPressedCase = null
                    },
                    text = deleteText
                )
            },
            dismissButton = {
                AzButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        longPressedCase = null
                    },
                    text = cancelText
                )
            },
        )
    }

    if (showCreateCaseDialog) {
        CreateCaseDialog(
            caseViewModel = caseViewModel,
            navController = navController,
            onDismiss = { showCreateCaseDialog = false }
        )
    }
}

/**
 * Composable representing a single case item in the list.
 * Supports tap selection and long-press context menu.
 */
@Composable
fun CaseItem(
    case: Case,
    isLongPressed: Boolean,
    isSelected: Boolean,
    onLongPress: (Case) -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isLongPressed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isLongPressed -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                // Use pointerInput for robust gesture detection (tap vs long press).
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress(case) },
                        onTap = { onClick() },
                    )
                },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = backgroundColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLongPressed || isSelected) 4.dp else 1.dp),
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
                    textAlign = TextAlign.End,
                )
            }
            // Context menu actions shown only on long press.
            if (isLongPressed) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_case)
                    )
                }
                IconButton(onClick = onArchive) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = stringResource(R.string.archive)
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            }
        }
    }
}
