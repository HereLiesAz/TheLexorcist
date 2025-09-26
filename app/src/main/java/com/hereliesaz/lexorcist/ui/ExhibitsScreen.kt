package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Exhibit
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExhibitsScreen(caseViewModel: CaseViewModel = hiltViewModel()) {
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val exhibits by caseViewModel.exhibits.collectAsState()
    val isLoading by caseViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.exhibits).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator()
                }
            } else if (selectedCase == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.please_select_case_for_exhibits).uppercase(Locale.getDefault()))
                }
            } else if (exhibits.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.no_exhibits_for_case).uppercase(Locale.getDefault()))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(exhibits) { exhibit ->
                        ExhibitItem(
                            exhibit = exhibit,
                            isSelected = false, // Not selectable on this screen
                            onClick = { /* No action */ },
                            onEditClick = { /* No action */ },
                            onDeleteClick = { /* No action */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExhibitItem(
    exhibit: Exhibit,
    isSelected: Boolean,
    onClick: (String) -> Unit,
    onEditClick: (Exhibit) -> Unit,
    onDeleteClick: (Exhibit) -> Unit,
) {
    Card(
        modifier =
        Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .pointerInput(exhibit) {
                detectTapGestures(onTap = { onClick(exhibit.id.toString()) })
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier =
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = exhibit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = exhibit.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { onEditClick(exhibit) }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit).uppercase(Locale.getDefault()))
                }
                IconButton(onClick = { onDeleteClick(exhibit) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete).uppercase(Locale.getDefault()))
                }
            }
        }
    }
}
