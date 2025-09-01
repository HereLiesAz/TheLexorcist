package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.* 
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import java.util.Locale

@Composable
fun AddEvidenceScreen(
    evidenceViewModel: EvidenceViewModel = viewModel(),
    onAddTextEvidence: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize() 
            .verticalScroll(rememberScrollState()) 
            .padding(16.dp), 
        horizontalAlignment = Alignment.End, 
        verticalArrangement = Arrangement.Top 
    ) {
        Button(onClick = onAddTextEvidence) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_text_evidence).uppercase(Locale.getDefault()))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_text_evidence).uppercase(Locale.getDefault())) // ALL CAPS
        }
    }
}
