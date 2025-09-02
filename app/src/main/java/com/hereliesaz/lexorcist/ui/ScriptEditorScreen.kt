package com.hereliesaz.lexorcist.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.ScriptEditorViewModel
import com.hereliesaz.lexorcist.common.state.SaveState

@OptIn(ExperimentalMaterial3Api::class) // Removed ExperimentalMaterial3ExpressiveApi
@Composable
fun ScriptEditorScreen(viewModel: ScriptEditorViewModel) {
    val scriptText by viewModel.scriptText.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf(false) } // Restored

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Script Builder Section
        Text(stringResource(R.string.script_builder), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editor Section with Tabs
        var tabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf(stringResource(R.string.script_tab_description), stringResource(R.string.script_tab_editor))

        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SecondaryTabRow(
                tabIndex,
                Modifier,
                TabRowDefaults.primaryContainerColor,
                TabRowDefaults.primaryContentColor,
                @Composable {
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabIndex))
                },
                @Composable { HorizontalDivider() },
                {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    text = { Text(title) },
                                    selected = tabIndex == index,
                                    onClick = { tabIndex = index }
                                )
                            }
                        })
            when (tabIndex) {
                0 -> { // Description Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.script_description_placeholder),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                1 -> { // Editor Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = stringResource(R.string.script_editor_instructions),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                        )
                        OutlinedTextField(
                            value = scriptText,
                            onValueChange = { viewModel.onScriptTextChanged(it) },
                            label = { Text(stringResource(R.string.enter_your_script)) },
                            placeholder = { Text(stringResource(R.string.script_editor_placeholder)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            OutlinedButton(
                onClick = { showShareDialog = true }
            ) {
                Text(stringResource(R.string.share))
            }
            OutlinedButton(
                onClick = { viewModel.saveScript() },
                enabled = saveState !is SaveState.Saving
            ) {
                if (saveState is SaveState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp)) // Changed back
                } else {
                    Text(stringResource(R.string.save_script))
                }
            }
        }
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(stringResource(R.string.share_script_title)) },
            text = { Text(stringResource(R.string.share_script_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Implement sharing logic with AddonsBrowserViewModel
                        showShareDialog = false
                    }
                ) {
                    Text(stringResource(R.string.share))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showShareDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
