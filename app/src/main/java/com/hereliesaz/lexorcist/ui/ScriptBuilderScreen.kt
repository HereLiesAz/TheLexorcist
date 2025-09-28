package com.hereliesaz.lexorcist.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
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
    val activeScriptIds by viewModel.activeScripts.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val context = LocalContext.current

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

    LaunchedEffect(Unit) {
        viewModel.shareScriptEvent.collect { event ->
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(event.recipientEmail))
                putExtra(android.content.Intent.EXTRA_SUBJECT, event.subject)
                putExtra(android.content.Intent.EXTRA_TEXT, event.body)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Script"))
        }
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
            var tabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf(
                "Editor",
                "Description",
                "Active Scripts"
            )
            var showLoadDialog by remember { mutableStateOf(false) }
            var showSnippetsDialog by remember { mutableStateOf(false) }
            var showShareDialog by remember { mutableStateOf(false) }

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

            if (showSnippetsDialog) {
                val snippets = listOf(
                    "lex.text.contains(\"example\")",
                    "lex.tags.contains(\"example\")",
                    "lex.date.isAfter(\"YYYY-MM-DD\")",
                    "lex.date.isBefore(\"YYYY-MM-DD\")"
                )
                AlertDialog(
                    onDismissRequest = { showSnippetsDialog = false },
                    title = { Text("Snippets") },
                    text = {
                        LazyColumn {
                            items(snippets) { snippet ->
                                Text(
                                    text = snippet,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.insertText(snippet)
                                            showSnippetsDialog = false
                                        }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        AzButton(
                            onClick = { showSnippetsDialog = false },
                            text = "Cancel"
                        )
                    }
                )
            }

            if (showShareDialog) {
                var authorName by remember { mutableStateOf("") }
                var authorEmail by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showShareDialog = false },
                    title = { Text(stringResource(R.string.share_script)) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = authorName,
                                onValueChange = { authorName = it },
                                label = { Text(stringResource(R.string.your_name)) },
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = authorEmail,
                                onValueChange = { authorEmail = it },
                                label = { Text(stringResource(R.string.your_email)) },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        AzButton(
                            onClick = {
                                viewModel.shareScript(authorName, authorEmail)
                                showShareDialog = false
                            },
                            text = stringResource(R.string.submit)
                        )
                    },
                    dismissButton = {
                        AzButton(
                            onClick = { showShareDialog = false },
                            text = stringResource(R.string.cancel)
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
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
                        OutlinedTextField(
                            value = scriptDescription,
                            onValueChange = { viewModel.onScriptDescriptionChanged(it) },
                            label = { Text(stringResource(R.string.script_description)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    2 -> {
                        val activeScriptObjects = remember(activeScriptIds, allScripts) {
                            activeScriptIds.mapNotNull { scriptId -> allScripts.find { it.id == scriptId } }
                        }
                        val reorderableState = rememberReorderableLazyListState(onMove = { from, to ->
                            viewModel.reorderActiveScripts(from.index, to.index)
                        })
                        LazyColumn(
                            state = reorderableState.listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .reorderable(reorderableState)
                                .detectReorderAfterLongPress(reorderableState)
                        ) {
                            items(activeScriptObjects, key = { it.id }) { script ->
                                ReorderableItem(reorderableState, key = script.id) { isDragging ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleActiveScript(script.id) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Checkbox(
                                            checked = activeScriptIds.contains(script.id),
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
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                    onClick = { showSnippetsDialog = true },
                    text = "Snippets"
                )
                AzButton(
                    onClick = { viewModel.saveScript() },
                    text = "Save"
                )
                AzButton(
                    onClick = { caseViewModel.rerunAllScriptsOnAllEvidence() },
                    text = "Run"
                )
                AzButton(
                    onClick = { showShareDialog = true },
                    text = "Share"
                )
            }
        }
    }
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
            Text(text = "by ${(script.authorName ?: "").ifBlank { script.authorEmail ?: "Unknown Author" }}", style = MaterialTheme.typography.bodySmall)
            Text(text = script.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}