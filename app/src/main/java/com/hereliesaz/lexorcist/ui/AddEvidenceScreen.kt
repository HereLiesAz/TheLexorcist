package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.viewmodel.EvidenceViewModel
import com.hereliesaz.lexorcist.viewmodel.OcrViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AddEvidenceScreen(
    evidenceViewModel: EvidenceViewModel,
    ocrViewModel: OcrViewModel,
    onSelectImage: () -> Unit,
    onTakePicture: () -> Unit,
    onAddTextEvidence: () -> Unit,
    onAddDocument: () -> Unit,
    onAddSpreadsheet: () -> Unit,
    onRecordAudio: () -> Unit,
    onImportAudio: () -> Unit
) {
    val imageBitmapForReview by ocrViewModel.imageBitmapForReview.collectAsState()
    val isOcrInProgress by ocrViewModel.isOcrInProgress.collectAsState()
    val evidenceList by evidenceViewModel.evidenceList.collectAsState()
    val context = LocalContext.current

    if (isOcrInProgress) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Processing Image...")
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            if (imageBitmapForReview == null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onSelectImage) {
                        Text("Select Image")
                    }
                    OutlinedButton(onClick = onTakePicture) {
                        Text("Take Picture")
                    }
                    OutlinedButton(onClick = onAddTextEvidence) {
                        Text("Add Text Evidence")
                    }
                    OutlinedButton(onClick = onAddDocument) {
                        Text("Add Document")
                    }
                    OutlinedButton(onClick = onAddSpreadsheet) {
                        Text("Add Spreadsheet")
                    }
                    OutlinedButton(onClick = onRecordAudio) {
                        Text("Record Audio")
                    }
                    OutlinedButton(onClick = onImportAudio) {
                        Text("Import Audio File")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(evidenceList) { evidence ->
                        val displayContent = if (evidence.content.length > 100) evidence.content.substring(0, 100) + "..." else evidence.content
                        val detailsList = listOfNotNull(
                            displayContent,
                            if (evidence.category.isNotBlank()) "Category: ${evidence.category}" else null,
                            evidence.tags.takeIf { it.isNotEmpty() }?.let { "Tags: ${it.joinToString(", ")}" },
                            "Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(evidence.documentDate)}"
                        )
                        TaggedDataItem(item = Pair(evidence.sourceDocument, detailsList))
                    }
                }

            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = imageBitmapForReview!!.asImageBitmap(),
                        contentDescription = "Image for review",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { ocrViewModel.rotateImageBeingReviewed(-90f) }) {
                            Icon(Icons.Filled.RotateLeft, contentDescription = "Rotate Left")
                        }
                        IconButton(onClick = { ocrViewModel.rotateImageBeingReviewed(90f) }) {
                            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate Right")
                        }
                        IconButton(onClick = { ocrViewModel.confirmImageReview(context) }) {
                            Icon(Icons.Filled.Done, contentDescription = "Approve Image")
                        }
                        IconButton(onClick = { ocrViewModel.cancelImageReview() }) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Cancel Review")
                        }
                    }
                }
            }
        }
    }
}
