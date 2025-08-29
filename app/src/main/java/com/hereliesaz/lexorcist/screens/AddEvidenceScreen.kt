package com.hereliesaz.lexorcist.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Corrected import for the MainViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.TaggedDataAdapter

@Composable
fun AddEvidenceScreen(
    // ViewModel type now correctly refers to the one in .viewmodel package
    viewModel: MainViewModel,
    onSelectImage: () -> Unit,
    onTakePicture: () -> Unit,
    onAddTextEvidence: () -> Unit,
    onAddDocument: () -> Unit,
    onAddSpreadsheet: () -> Unit
) {
    val extractedText by viewModel.extractedText.collectAsState()
    val imageBitmap by viewModel.imageBitmap.collectAsState()
    val taggedData by viewModel.taggedData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End, // Align children to the end (right)
        verticalArrangement = Arrangement.Center // Center children vertically as a group
    ) {
        // Button column is now a direct child, will be right-aligned and vertically centered with other siblings.
        Column(
            horizontalAlignment = Alignment.End // Ensure buttons within this column align to its end
        ) {
            Button(onClick = onSelectImage) {
                Text("Select Image")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onTakePicture) {
                Text("Take Picture")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddTextEvidence) {
                Text("Add Text Evidence")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddDocument) {
                Text("Add Document")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddSpreadsheet) {
                Text("Add Spreadsheet")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        imageBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth() // Image takes full width
                    .height(200.dp)
                // Removed .align(Alignment.CenterHorizontally) - will be right-aligned by parent Column
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AndroidView(
            factory = { context ->
                RecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                }
            },
            update = { recyclerView ->
                recyclerView.adapter = TaggedDataAdapter(taggedData)
            },
            modifier = Modifier.fillMaxWidth() // RecyclerView takes full width, will be right-aligned by parent Column
        )
    }
}
