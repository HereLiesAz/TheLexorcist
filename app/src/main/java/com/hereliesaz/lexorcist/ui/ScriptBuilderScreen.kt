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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState
import sh.calvin.reorderable.reorderable // Added import
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
import com.hereliesaz.lexorcist.ui.components.RequestDialog
import com.hereliesaz.lexorcist.utils.sendEmail
import com.hereliesaz.lexorcist.viewmodel.ScriptBuilderViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScriptBuilderScreen(
    viewModel: ScriptBuilderViewModel = hiltViewModel(),
    navController: NavController
) {
    val scriptTitle by viewModel.scriptTitle.collectAsState()
    val scriptDescription by viewModel.scriptDescription.collectAsState()
    val scriptText by viewModel.scriptText.collectAsState()
    val allScripts by viewModel.allScripts.collectAsState()
    val activeScripts by viewModel.activeScripts.collectAsState()
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AzButton(
                    onClick = { viewModel.insertText("some snippet") }, 
                    text = "Snippets" // Corrected AzButton usage
                )
                AzButton(
                    onClick = { viewModel.openScriptSelectionDialog() },
                    text = "Import"
                )
            }
            var tabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf(
                "Editor",
                "Description",
                "Active Scripts"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                AzButton(
                    onClick = { viewModel.openScriptSelectionDialog() },
                    text = "Import"
                )
                AzButton(
                    onClick = { /* TODO: Implement Snippets Dialog */ },
                    text = "Snippets"
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
                    onClick = { viewModel.openScriptSelectionDialog() },
                    text = "Import"
                )
                AzButton(
                    onClick = { /* TODO: Implement Snippets Dialog */ },
                    text = "Snippets"
                )
                AzButton(
                    onClick = { viewModel.runScripts() },
                    text = "Run"
                )
                AzButton(
                    onClick = { viewModel.saveScript() },
                    text = stringResource(R.string.save_script)
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
            Text(text = "by ${(script.authorName ?: "").ifBlank { script.authorEmail ?: "Unknown Author" }}", style = MaterialTheme.typography.bodySmall) // Updated to use authorName, with fallback to authorEmail
            Text(text = script.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
