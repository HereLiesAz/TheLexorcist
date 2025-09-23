package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.viewmodel.AllegationSortType
import com.hereliesaz.lexorcist.viewmodel.MasterAllegationsViewModel
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.lexorcist.ui.components.AzAlertDialog

@OptIn(ExperimentalFoundationApi::class)
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

    // According to AGENTS.md, this screen should be laid out as:
    // 1. "Allegations" Title (Handled by TopAppBar)
    // 2. List of selected allegations applied to the case
    // 3. Search box
    // 4. Request button next to sort-by option on the same row
    // 5. Complete list of available allegations to select from

    Scaffold(

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // 2. List of selected allegations
            if (selectedAllegations.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.selected_allegations),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp) // Fixed height for selected allegations list
                ) {
                    itemsIndexed(selectedAllegations) { _, allegation ->
                        Text(
                            text = allegation.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { viewModel.toggleAllegationSelection(allegation) },
                                    onLongClick = { showDetailsDialog = allegation }
                                )
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.End
                        )
                        if (selectedAllegations.last() != allegation) {
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Search Box
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text(stringResource(id = R.string.search_allegations)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 4. Request button next to sort-by option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                AzCycler(
                    options = AllegationSortType.entries.map { it.name },
                    selectedOption = sortOption.name,
                    onCycle = {
                        val nextIndex = (sortOption.ordinal + 1) % AllegationSortType.entries.size
                        viewModel.onSortTypeChanged(AllegationSortType.entries[nextIndex])
                    },
                    modifier = Modifier.weight(1f)
                )

                AzButton(
                    onClick = { showRequestDialog = true },
                    text = stringResource(id = R.string.request),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 5. Complete list of available allegations
            if (allegations.isEmpty() && searchTerm.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.no_allegations_found_for_search, searchTerm),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else if (allegations.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_allegations_available),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(allegations) { _, allegation ->
                    Text(
                        text = allegation.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.toggleAllegationSelection(allegation) },
                                onLongClick = { showDetailsDialog = allegation }
                            )
                            .padding(vertical = 8.dp),
                        color = if (allegation.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                    if (allegations.last() != allegation) {
                        HorizontalDivider()
                    }
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
            title = { Text(stringResource(id = R.string.request_new_allegation)) },
            text = {
                Column {
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
//                        viewModel.requestNewAllegation(requestedName, requestedDescription, requestedCategory)
                        showRequestDialog = false
                    },
                    text = stringResource(id = R.string.submit)
                )
            },
            dismissButton = {
                AzButton (
                    onClick = { showRequestDialog = false },
                    text = stringResource(id = R.string.cancel)
                )
            }
        )
    }

    showDetailsDialog?.let { allegation ->
        AzAlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = { Text(allegation.name) },
            text = { Text("${stringResource(id = R.string.category)}: ${allegation.category}\n\n${allegation.description}") },
            confirmButton = {
                AzButton(
                    onClick = { showDetailsDialog = null },
                    text = stringResource(id = android.R.string.ok)
                )
            }
        )
    }
}
