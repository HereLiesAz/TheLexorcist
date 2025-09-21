package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel
import java.util.Locale

@Composable
fun EvidenceDetailsScreen(
    evidence: Evidence,
    caseViewModel: CaseViewModel,
    navController: NavController,
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
            if (!evidence.formattedContent.isNullOrEmpty()) {
                Text("Formatted Content:")
                Text(evidence.formattedContent)
                Spacer(modifier = Modifier.height(16.dp))
            }
            OutlinedTextField(
                value = commentary,
                onValueChange = { commentary = it },
                label = { Text("Commentary") },
                modifier = Modifier.fillMaxWidth(), // TextField takes full width
            )
            Spacer(modifier = Modifier.height(16.dp))
            LexorcistOutlinedButton(onClick = { caseViewModel.updateCommentary(evidence.id, commentary) }, text = "Save Commentary")
            if (evidence.type == "audio") {
                Spacer(modifier = Modifier.height(16.dp))
                LexorcistOutlinedButton(
                    onClick = { navController.navigate("transcription/${evidence.id}") },
                    text = stringResource(R.string.edit_transcript).uppercase(Locale.getDefault()),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LexorcistOutlinedButton(
                onClick = {
                    caseViewModel.deleteEvidence(evidence)
                    navController.popBackStack()
                },
                text = "Remove",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            LexorcistOutlinedButton(
                onClick = {
                    caseViewModel.deleteEvidence(evidence)
                    navController.popBackStack()
                },
                text = "Remove",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
