package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hereliesaz.lexorcist.data.Evidence
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton

@Composable
fun EvidenceDetailsDialog(
    evidence: Evidence,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onNavigateToEvidenceDetails: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Evidence Details")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (evidence.type == "image" || evidence.type == "ocr_image_from_video") {
                    AsyncImage(
                        model = evidence.sourceDocument,
                        contentDescription = "Evidence Preview",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = evidence.content,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                LexorcistOutlinedButton(onClick = onRemove) {
                    Text("Remove")
                }
                Spacer(modifier = Modifier.weight(1f))
                LexorcistOutlinedButton(onClick = onNavigateToEvidenceDetails) {
                    Text("View Full Details")
                }
                Spacer(modifier = Modifier.height(8.dp))
                LexorcistOutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        modifier = Modifier.padding(16.dp),
    )
}
