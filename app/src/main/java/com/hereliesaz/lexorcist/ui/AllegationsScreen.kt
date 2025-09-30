package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.ui.components.AzAlertDialog
import com.hereliesaz.lexorcist.viewmodel.AllegationSortType
import com.hereliesaz.lexorcist.viewmodel.MasterAllegationsViewModel
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllegationsScreen(
    viewModel: MasterAllegationsViewModel = hiltViewModel()
) {
    val allegations by viewModel.allegations.collectAsState()
    val selectedAllegations by viewModel.selectedAllegations.collectAsState()
    val searchTerm by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortType.collectAsState()
    var showRequestDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf<MasterAllegation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.allegations).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Section for displaying selected allegations as chips
            if (selectedAllegations.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.selected_allegations),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedAllegations.forEach { allegation ->
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { viewModel.toggleAllegationSelection(allegation) },
                                    onLongClick = { showDetailsDialog = allegation }
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = allegation.name,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search Box
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text(stringResource(id = R.string.search_allegations)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Sort and Request buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val sortOptions = AllegationSortType.entries.map { it.name }
                AzCycler(
                    options = sortOptions,
                    selectedOption = sortOption.name,
                    onCycle = {
                        val nextIndex = (sortOption.ordinal + 1) % AllegationSortType.entries.size
                        viewModel.onSortTypeChanged(AllegationSortType.entries[nextIndex])
                    }
                )
                val requestText = stringResource(id = R.string.request)
                AzButton(
                    onClick = { showRequestDialog = true },
                    text = requestText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Main list of all available allegations
            if (allegations.isEmpty() && searchTerm.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.no_allegations_found_for_search, searchTerm),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else if (allegations.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_allegations_available),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(allegations, key = { it.id ?: it.name }) { allegation ->
                    val backgroundColor = if (allegation.isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .combinedClickable(
                                onClick = { viewModel.toggleAllegationSelection(allegation) },
                                onLongClick = { showDetailsDialog = allegation }
                            )
                    ) {
                        Text(
                            text = allegation.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }

    if (showRequestDialog) {
        var requestedName by remember { mutableStateOf("") }
        var requestedDescription by remember { mutableStateOf("") }
        var requestedCategory by remember { mutableStateOf("") }

        AzAlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = {
                Text(
                    stringResource(id = R.string.request_new_allegation),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.End) {
                    TextField(
                        value = requestedName,
                        onValueChange = { requestedName = it },
                        label = { Text(stringResource(id = R.string.allegation_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = requestedDescription,
                        onValueChange = { requestedDescription = it },
                        label = { Text(stringResource(id = R.string.description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = requestedCategory,
                        onValueChange = { requestedCategory = it },
                        label = { Text(stringResource(id = R.string.category)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                AzButton(
                    onClick = {
                        // viewModel.requestNewAllegation(requestedName, requestedDescription, requestedCategory)
                        showRequestDialog = false
                    },
                    text = stringResource(id = R.string.submit)
                )
            },
            dismissButton = {
                AzButton(
                    onClick = { showRequestDialog = false },
                    text = stringResource(id = R.string.cancel)
                )
            },
        )
    }

    showDetailsDialog?.let { allegation ->
        AzAlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = {
                Text(
                    allegation.name,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            text = {
                Text(
                    "${stringResource(id = R.string.category)}: ${allegation.category}\n\n${allegation.description}",
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                AzButton(
                    onClick = { showDetailsDialog = null },
                    text = stringResource(id = android.R.string.ok)
                )
            },
            dismissButton = { }
        )
    }
}