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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.common.state.SaveState
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.ui.components.RequestDialog
import com.hereliesaz.lexorcist.utils.sendEmail
import com.hereliesaz.lexorcist.viewmodel.ExtrasViewModel
import com.hereliesaz.lexorcist.viewmodel.ScriptBuilderViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScriptBuilderScreen(
    viewModel: ScriptBuilderViewModel = hiltViewModel(),
    navController: NavController
) {
    val activity = LocalContext.current as ComponentActivity
    val extrasViewModel: ExtrasViewModel = hiltViewModel(viewModelStoreOwner = activity)

    val scriptTitle by viewModel.scriptTitle.collectAsState()
    val scriptDescription by viewModel.scriptDescription.collectAsState()
    val scriptText by viewModel.scriptText.collectAsState()
    val caseScripts by viewModel.caseScripts.collectAsState()
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showRequestDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Make a request")
            }
        }
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { viewModel.insertText(snippetTextIncludesStr) }) {
                    Text(stringResource(R.string.script_snippet_text_includes_label))
                }
                OutlinedButton(onClick = { viewModel.insertText(snippetTagsIncludesStr) }) {
                    Text(stringResource(R.string.script_snippet_tags_includes_label))
                }
                OutlinedButton(onClick = { viewModel.insertText(snippetDateGreaterStr) }) {
                    Text(stringResource(R.string.script_snippet_date_greater_label))
                }
                OutlinedButton(onClick = { viewModel.insertText(snippetDateLessStr) }) {
                    Text(stringResource(R.string.script_snippet_date_less_label))
                }
                LexorcistOutlinedButton(
                    onClick = { viewModel.openScriptSelectionDialog() },
                    content = { Text("Import Scripts...") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = scriptTitle,
                onValueChange = { viewModel.onScriptTitleChanged(it) },
                label = { Text(stringResource(R.string.script_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            var tabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf(
                stringResource(R.string.script_tab_description),
                stringResource(R.string.script_tab_editor),
                "Case Scripts"
            )

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
                        Column(
                            modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Top,
                        ) {
                            Text(
                                text = stringResource(R.string.script_editor_explanation),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = scriptDescription,
                                onValueChange = { viewModel.onScriptDescriptionChanged(it) },
                                label = { Text(stringResource(R.string.script_description)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    1 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
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
                    2 -> {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(caseScripts) { script ->
                                ScriptItem(script = script, onClick = {
                                    viewModel.loadScript(script)
                                    tabIndex = 1
                                })
                                Spacer(modifier = Modifier.height(8.dp))
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
                LexorcistOutlinedButton(
                    onClick = { showRequestDialog = true },
                    content = { Text("Request") }
                )
                LexorcistOutlinedButton(
                    onClick = { showShareDialog = true },
                    content = { Text(stringResource(R.string.share)) }
                )
                LexorcistOutlinedButton(
                    onClick = { viewModel.saveScript() },
                    content = {
                        if (saveState is SaveState.Saving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(stringResource(R.string.save_script))
                        }
                    }
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
            Text(text = "by ${script.authorName.ifBlank { script.authorEmail }}", style = MaterialTheme.typography.bodySmall) // Updated to use authorName, with fallback to authorEmail
            Text(text = script.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
