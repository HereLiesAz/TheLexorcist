package com.hereliesaz.lexorcist.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.ScriptEditorViewModel
import com.hereliesaz.lexorcist.common.state.SaveState

@Composable
fun ScriptEditorScreen(viewModel: ScriptEditorViewModel) {
    val scriptText by viewModel.scriptText.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val context = LocalContext.current

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

    Row(modifier = Modifier.fillMaxSize()) {
        // Editor Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Spacer removed - content starts at the top of this panel
            OutlinedTextField(
                value = scriptText,
                onValueChange = { viewModel.onScriptTextChanged(it) },
                label = { Text(stringResource(R.string.enter_your_script)) },
                placeholder = { Text(stringResource(R.string.script_editor_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // TextField takes available vertical space in this Column
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.saveScript() },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is SaveState.Saving
            ) {
                if (saveState is SaveState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.save_script))
                }
            }
        }

        // Script Builder Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Spacer removed - content starts at the top of this panel
            Text(stringResource(R.string.script_builder), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { viewModel.insertText(snippetTextIncludesStr) }) {
                Text(stringResource(R.string.script_snippet_text_includes_label))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.insertText(snippetTagsIncludesStr) }) {
                Text(stringResource(R.string.script_snippet_tags_includes_label))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.insertText(snippetDateGreaterStr) }) {
                Text(stringResource(R.string.script_snippet_date_greater_label))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.insertText(snippetDateLessStr) }) {
                Text(stringResource(R.string.script_snippet_date_less_label))
            }
        }
    }
}
