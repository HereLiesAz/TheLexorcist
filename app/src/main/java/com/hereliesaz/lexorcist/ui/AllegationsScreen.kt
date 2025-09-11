package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.hereliesaz.lexorcist.viewmodel.MasterAllegationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllegationsScreen(
    viewModel: MasterAllegationsViewModel = hiltViewModel()
) {
    val allegations by viewModel.allegations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val groupedAllegations = allegations.groupBy { it.type }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.allegations)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text(stringResource(R.string.search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                groupedAllegations.forEach { (type, allegationsForType) ->
                    item {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    val allegationsByCategory = allegationsForType.groupBy { it.category }
                    allegationsByCategory.forEach { (category, allegationList) ->
                        item {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                        items(allegationList) { allegation ->
                            AllegationItem(allegation = allegation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllegationItem(allegation: MasterAllegation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = allegation.name, fontWeight = FontWeight.Bold)
            Text(text = allegation.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
