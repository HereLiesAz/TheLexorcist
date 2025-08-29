package com.hereliesaz.lexorcist.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereliesaz.lexorcist.viewmodel.DataReviewViewModel
import com.hereliesaz.lexorcist.viewmodel.MainViewModel

@Composable
fun DataReviewScreen(
    mainViewModel: MainViewModel,
    dataReviewViewModel: DataReviewViewModel = viewModel(),
) {
    val extractedText by mainViewModel.extractedText.collectAsState()
    val reviewedText by dataReviewViewModel.reviewedText.collectAsState()

    LaunchedEffect(extractedText) {
        dataReviewViewModel.onTextChange(extractedText)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = reviewedText,
            onValueChange = { dataReviewViewModel.onTextChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Extracted Text") }
        )
        Button(
            onClick = { mainViewModel.updateExtractedText(reviewedText) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}