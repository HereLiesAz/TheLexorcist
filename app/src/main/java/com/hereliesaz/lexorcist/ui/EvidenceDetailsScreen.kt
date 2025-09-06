package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState // Added import
import androidx.compose.foundation.verticalScroll // Added import
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel

@Composable
fun EvidenceDetailsScreen(
    evidence: Evidence,
    viewModel: EvidenceViewModel,
) {
    var commentary by remember { mutableStateOf(evidence.commentary ?: "") }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp), // Apply padding to the outer Box
    ) {
        val halfScreenHeight = this@BoxWithConstraints.maxHeight / 2

        Column(
            modifier =
                Modifier
                    .fillMaxSize() // Column fills the BoxWithConstraints
                    .verticalScroll(rememberScrollState()),
            // Make content scrollable
            horizontalAlignment = Alignment.End, // Right-align children of this Column
        ) {
            Spacer(modifier = Modifier.height(halfScreenHeight)) // Push content to start halfway down

            if (evidence.category in listOf("Image", "OCR Image") && evidence.sourceDocument.startsWith("content://")) {
                Image(
                    painter = rememberAsyncImagePainter(model = evidence.sourceDocument.toUri()),
                    contentDescription = "Evidence Image",
                    modifier = Modifier.fillMaxWidth(), // Image takes full width
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text("Source: ${evidence.sourceDocument}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Category: ${evidence.category}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tags: ${evidence.tags.joinToString()}")
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = commentary ?: "",
                onValueChange = { commentary = it },
                label = { Text("Commentary") },
                modifier = Modifier.fillMaxWidth(), // TextField takes full width
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.updateCommentary(evidence.id, commentary ?: "") }) {
                Text("Save Commentary")
            }
        }
    }
}
