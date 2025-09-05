package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.AllegationsViewModel
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllegationsScreen(
    allegationsViewModel: AllegationsViewModel = hiltViewModel(),
    caseViewModel: CaseViewModel = hiltViewModel()
) {
    val allegations by allegationsViewModel.allegations.collectAsState()
    val searchQuery by allegationsViewModel.searchQuery.collectAsState()
    val isDialogShown by allegationsViewModel.isDialogShown.collectAsState()
    val selectedAllegation by allegationsViewModel.selectedAllegation.collectAsState()
    val selectedCase by caseViewModel.selectedCase.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(selectedCase) {
        selectedCase?.spreadsheetId?.let {
            allegationsViewModel.loadAllegations(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.allegations).uppercase(Locale.getDefault()),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { allegationsViewModel.onSearchQueryChanged(it) },
                label = { Text("Search Allegations") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                )
            )
            LazyColumn {
                items(allegations) { allegation ->
                    Text(
                        text = allegation.text,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { allegationsViewModel.onAllegationSelected(allegation) }
                    )
                }
            }
        }
    }

    if (isDialogShown) {
        selectedAllegation?.let {
            AlertDialog(
                onDismissRequest = { allegationsViewModel.onDialogDismiss() },
                title = { Text(it.text) },
                text = { Text(it.text) }, // Using text as description for now
                confirmButton = {
                    Row {
                        Button(onClick = { /* TODO: Implement Similar To */ allegationsViewModel.onDialogDismiss() }) {
                            Text("Similar to")
                        }
                        Button(onClick = { /* TODO: Implement Add */ allegationsViewModel.onDialogDismiss() }) {
                            Text("Add")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { allegationsViewModel.onDialogDismiss() }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
