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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.AllegationsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllegationsScreen(
    viewModel: AllegationsViewModel = hiltViewModel()
) {
    val allegations by viewModel.allegations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDialogShown by viewModel.isDialogShown.collectAsState()
    val selectedAllegation by viewModel.selectedAllegation.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.loadAllegations("some-case-id")
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
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
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
                        .clickable { viewModel.onAllegationSelected(allegation) }
                )
            }
        }
    }

    if (isDialogShown) {
        selectedAllegation?.let {
            AlertDialog(
                onDismissRequest = { viewModel.onDialogDismiss() },
                title = { Text(it.text) },
                text = { Text(it.text) }, // Using text as description for now
                confirmButton = {
                    Row {
                        Button(onClick = { /* TODO: Implement Similar To */ viewModel.onDialogDismiss() }) {
                            Text("Similar to")
                        }
                        Button(onClick = { /* TODO: Implement Add */ viewModel.onDialogDismiss() }) {
                            Text("Add")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { viewModel.onDialogDismiss() }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}}
