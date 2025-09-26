package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                com.hereliesaz.lexorcist.ui.components.NewLexorcistLoadingIndicator()
            } else if (selectedCase == null) {
                Text(stringResource(R.string.please_select_case_for_exhibits).uppercase(Locale.getDefault()))
            } else if (exhibits.isEmpty()) {
                Text(stringResource(R.string.no_exhibits_for_case).uppercase(Locale.getDefault()))
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
