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
import com.hereliesaz.lexorcist.MainViewModel
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.TaggedDataAdapter

@Composable
fun AddEvidenceScreen(
    viewModel: MainViewModel,
    onSelectImage: () -> Unit,
    onTakePicture: () -> Unit
) {
    val extractedText by viewModel.extractedText.collectAsState()
    val imageBitmap by viewModel.imageBitmap.collectAsState()
    val taggedData by viewModel.taggedData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Button(onClick = onSelectImage) {
                Text("Select Image")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onTakePicture) {
                Text("Take Picture")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: Add text evidence */ }) {
            Text("Add Text Evidence")
        }
        Spacer(modifier = Modifier.height(16.dp))
        imageBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
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
            }
        )
    }
}
