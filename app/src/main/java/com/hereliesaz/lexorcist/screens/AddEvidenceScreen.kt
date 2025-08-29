package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft // Updated import
import androidx.compose.material.icons.automirrored.filled.RotateRight // Updated import
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.TaggedDataAdapter

@Composable
fun AddEvidenceScreen(
    viewModel: MainViewModel,
    onSelectImage: () -> Unit,
    onTakePicture: () -> Unit,
    onAddTextEvidence: () -> Unit,
    onAddDocument: () -> Unit,
    onAddSpreadsheet: () -> Unit
) {
    val imageBitmapForReview by viewModel.imageBitmapForReview.collectAsState()
    val taggedData by viewModel.taggedData.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageBitmapForReview == null) {
            // Buttons for adding new evidence
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSelectImage) {
                    Text(stringResource(R.string.select_image))
                }
                Button(onClick = onTakePicture) {
                    Text(stringResource(R.string.take_picture))
                }
                Button(onClick = onAddTextEvidence) {
                    Text(stringResource(R.string.add_text_evidence))
                }
                Button(onClick = onAddDocument) {
                    Text(stringResource(R.string.add_document))
                }
                Button(onClick = onAddSpreadsheet) {
                    Text(stringResource(R.string.add_spreadsheet))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RecyclerView for tagged data
            AndroidView(
                factory = { ctx ->
                    RecyclerView(ctx).apply {
                        layoutManager = LinearLayoutManager(ctx)
                    }
                },
                update = { recyclerView ->
                    recyclerView.adapter = TaggedDataAdapter(taggedData)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) 
            )

        } else {
            // Image Review UI
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    bitmap = imageBitmapForReview!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.image_for_review),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) 
                        .padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.rotateImageBeingReviewed(-90f) }) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = stringResource(R.string.rotate_left)) // Updated Icon
                    }
                    IconButton(onClick = { viewModel.rotateImageBeingReviewed(90f) }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = stringResource(R.string.rotate_right)) // Updated Icon
                    }
                    IconButton(onClick = { viewModel.confirmImageReview(context) }) {
                        Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.approve_image))
                    }
                    IconButton(onClick = { viewModel.cancelImageReview() }) {
                        Icon(Icons.Filled.Cancel, contentDescription = stringResource(R.string.cancel_review))
                    }
                }
            }
        }
    }
}
