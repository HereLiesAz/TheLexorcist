package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTextEvidenceScreen(
    evidenceViewModel: EvidenceViewModel, 
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_text_evidence).uppercase(Locale.getDefault())) }, // ALL CAPS
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp) // Apply general padding for the content area
        ) {
            val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End 
            ) {
                Spacer(modifier = Modifier.height(halfScreenHeight))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.evidence_text_label)) }, // Needs R.string.evidence_text_label
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text(stringResource(R.string.enter_evidence_content)) } // Using existing string
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onSave(text) },
                    modifier = Modifier.fillMaxWidth() // Make button full width
                ) {
                    Text(stringResource(R.string.save).uppercase(Locale.getDefault())) // ALL CAPS
                }
            }
        }
    }
}

// Need to add to strings.xml:
// R.string.evidence_text_label = "Evidence Text"
