package com.hereliesaz.lexorcist.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.viewmodel.ScriptEditorViewModel
import com.hereliesaz.lexorcist.viewmodel.SaveState

@Composable
fun ScriptEditorScreen(viewModel: ScriptEditorViewModel) {
    val scriptText by viewModel.scriptText.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                Toast.makeText(context, "Script saved successfully", Toast.LENGTH_SHORT).show()
            }
            is SaveState.Error -> {
                val errorMessage = (saveState as SaveState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = scriptText,
                onValueChange = { viewModel.onScriptTextChanged(it) },
                label = { Text("Enter your script") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveScript() },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState != SaveState.Saving
            ) {
                if (saveState == SaveState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Script")
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Script Builder", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.insertText("evidence.text.includes(\"\")") }) {
                Text("text.includes(...)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.insertText("evidence.tags.includes(\"\")") }) {
                Text("tags.includes(...)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.insertText("evidence.documentDate > new Date(\"YYYY-MM-DD\").getTime()") }) {
                Text("date > ...")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.insertText("evidence.documentDate < new Date(\"YYYY-MM-DD\").getTime()") }) {
                Text("date < ...")
            }
        }
    }
}
