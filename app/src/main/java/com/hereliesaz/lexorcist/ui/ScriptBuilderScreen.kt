package com.hereliesaz.lexorcist.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel // Updated import
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.common.state.SaveState
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptBuilderViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState
import sh.calvin.reorderable.reorderable
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScriptBuilderScreen(
    viewModel: ScriptBuilderViewModel = hiltViewModel(),
    navController: NavController,
    caseViewModel: CaseViewModel
) {
    val scriptTitle by viewModel.scriptTitle.collectAsState()
    val scriptDescription by viewModel.scriptDescription.collectAsState()
    val scriptText by viewModel.scriptText.collectAsState()
    val allScripts by viewModel.allScripts.collectAsState()
    val activeScripts by viewModel.activeScripts.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val extrasUiState by extrasViewModel.uiState.collectAsState()
    var previousExtrasIsLoading by remember { mutableStateOf(extrasUiState.isLoading) }

    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }

    if (showRequestDialog) {
        RequestDialog(
            onDismissRequest = { showRequestDialog = false },
            onSendRequest = { name, email, request ->
                val body = "Name: $name\nEmail: $email\nRequest: $request"
                sendEmail(context, "hereliesaz@gmail.com", "Scripts Request", body)
                showRequestDialog = false
            }
        )
    }

    val snippetTextIncludesStr = stringResource(R.string.script_snippet_text_includes)
    val snippetTagsIncludesStr = stringResource(R.string.script_snippet_tags_includes)
    val snippetDateGreaterStr = stringResource(R.string.script_snippet_date_greater)
    val snippetDateLessStr = stringResource(R.string.script_snippet_date_less)

    LaunchedEffect(saveState) {
        when (val currentState = saveState) {
            is SaveState.Success -> {
                Toast.makeText(context, R.string.script_saved_successfully, Toast.LENGTH_SHORT).show()
            }
            is SaveState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    LaunchedEffect(extrasUiState) {
        if (previousExtrasIsLoading && !extrasUiState.isLoading) {
            if (extrasUiState.error != null) {
                Toast.makeText(context, "Failed to share script: ${extrasUiState.error}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Script shared successfully!", Toast.LENGTH_SHORT).show()
            }
        }
        previousExtrasIsLoading = extrasUiState.isLoading
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.script_builder).uppercase(Locale.getDefault()),
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
                .padding(8.dp),
            horizontalAlignment = Alignment.End,
        ) {

            var tabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf(
                "Editor",
                "Description",
                "Active Scripts"
            )
            var showLoadDialog by remember { mutableStateOf(false) }

            if (showLoadDialog) {
                AlertDialog(
                    onDismissRequest = { showLoadDialog = false },
                    title = { Text("Load Script") },
                    text = {
                        LazyColumn {
                            items(allScripts) { script ->
                                ScriptItem(script = script) {
                                    viewModel.loadScript(script)
                                    showLoadDialog = false
                                }
                            }
                        }
                    },
                    confirmButton = {
                        AzButton(
                            onClick = { showLoadDialog = false },
                            text = "Cancel"
                        )
                    }
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                SecondaryTabRow(
                    selectedTabIndex = tabIndex,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabIndex)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                        )
                    }
                }
                when (tabIndex) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                             OutlinedTextField(
                                value = scriptTitle,
                                onValueChange = { viewModel.onScriptTitleChanged(it) },
                                label = { Text(stringResource(R.string.script_title)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = scriptText,
                                onValueChange = { viewModel.onScriptTextChanged(it) },
                                label = { Text(stringResource(R.string.enter_your_script)) },
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                            )
                        }
                    }
                    1 -> {
                        Column(
                            modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Top,
                        ) {
                            OutlinedTextField(
                                value = scriptDescription,
                                onValueChange = { viewModel.onScriptDescriptionChanged(it) },
                                label = { Text(stringResource(R.string.script_description)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    2 -> {
                        val activeScriptObjects = activeScripts.mapNotNull { scriptId -> allScripts.find { it.id == scriptId } }
                        val reorderableState = rememberReorderableLazyColumnState(onMove = { from, to ->
                            viewModel.reorderActiveScripts(from.index, to.index)
                        })
                        LazyColumn(
                            state = reorderableState.listState,
                            modifier = Modifier.fillMaxSize().padding(16.dp).reorderable(reorderableState)
                        ) {
                            items(activeScriptObjects, key = { it.id }) { script ->
                                ReorderableItem(reorderableState, key = script.id) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleActiveScript(script.id) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Checkbox(
                                            checked = activeScripts.contains(script.id),
                                            onCheckedChange = { viewModel.toggleActiveScript(script.id) }
                                        )
                                        Spacer(modifier = Modifier.size(16.dp))
                                        Text(text = script.name)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                AzButton(
                    onClick = { viewModel.newScript() },
                    text = "New"
                )
                AzButton(
                    onClick = { showLoadDialog = true },
                    text = "Load"
                )
                AzButton(
                    onClick = {
                        val allEvidence = caseViewModel.selectedCaseEvidenceList.value
                        viewModel.runScripts(allEvidence, caseViewModel)
                    },
                    text = "Run"
                )
                AzButton(
                    onClick = { viewModel.saveScript() },
                    text = stringResource(R.string.save_script)
                )
            }
        }
    }

    if (showShareDialog) {
        var dialogAuthorName by remember { mutableStateOf("") }
        var dialogAuthorEmail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(scriptTitle.ifBlank { "Untitled Script" }) },
            text = {
                Column {
                    Text(scriptDescription.ifBlank { "No description." })
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dialogAuthorName,
                        onValueChange = { dialogAuthorName = it },
                        label = { Text("Your Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dialogAuthorEmail,
                        onValueChange = { dialogAuthorEmail = it },
                        label = { Text("Your Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                LexorcistOutlinedButton(
                    onClick = {
                        if (scriptTitle.isNotBlank() &&
                            scriptText.isNotBlank() &&
                            dialogAuthorName.isNotBlank() &&
                            dialogAuthorEmail.isNotBlank()) {
                            extrasViewModel.shareItem(
                                name = scriptTitle,
                                description = scriptDescription,
                                content = scriptText,
                                type = "Script",
                                authorName = dialogAuthorName,
                                authorEmail = dialogAuthorEmail
                            )
                            showShareDialog = false
                        } else {
                            Toast.makeText(context, "All fields including name and email are required for sharing.", Toast.LENGTH_LONG).show()
                        }
                    },
                    content = { Text(stringResource(R.string.share)) }
                )
            },
            dismissButton = {
                LexorcistOutlinedButton(
                    onClick = { showShareDialog = false },
                    content = { Text(stringResource(R.string.cancel)) }
                )
            }
        )
    }

    val showDialog by viewModel.showScriptSelectionDialog.collectAsState()
    if (showDialog) {
        ScriptSelectionDialog(
            scripts = caseScripts,
            onDismiss = { viewModel.closeScriptSelectionDialog() },
            onConfirm = { selectedScripts ->
                viewModel.onScriptsSelected(selectedScripts)
            }
        )
    }
}

@Composable
fun ScriptSelectionDialog(
    scripts: List<Script>,
    onDismiss: () -> Unit,
    onConfirm: (List<Script>) -> Unit
) {
    var selectedScripts by remember { mutableStateOf<Set<Script>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Scripts to Import") },
        text = {
            LazyColumn {
                items(scripts) { script ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedScripts = if (selectedScripts.contains(script)) {
                                    selectedScripts - script
                                } else {
                                    selectedScripts + script
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = selectedScripts.contains(script),
                            onCheckedChange = { isChecked ->
                                selectedScripts = if (isChecked) {
                                    selectedScripts + script
                                } else {
                                    selectedScripts - script
                                }
                            }
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(text = script.name)
                    }
                }
            }
        },
        confirmButton = {
            LexorcistOutlinedButton(
                onClick = { onConfirm(selectedScripts.toList()) },
                content = { Text("Import") }
            )
        },
        dismissButton = {
            LexorcistOutlinedButton(
                onClick = onDismiss,
                content = { Text("Cancel") }
            )
        }
    )
}

@Composable
fun ScriptItem(script: Script, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = script.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "by ${(script.authorName ?: "").ifBlank { script.authorEmail ?: "Unknown Author" }}", style = MaterialTheme.typography.bodySmall) // Updated to use authorName, with fallback to authorEmail
            Text(text = script.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
