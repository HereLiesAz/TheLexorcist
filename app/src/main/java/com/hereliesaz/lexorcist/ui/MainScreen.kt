package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.viewmodel.MainViewModel

@Composable
fun MainScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    onSignInClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val evidenceList by mainViewModel.evidenceList.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSignInClick) {
                Text("Sign In")
            }
            Button(onClick = onExportClick) {
                Text("Export to Sheet")
            }
            Button(onClick = {
                mainViewModel.processEvidenceForReview()
                navController.navigate("data_review")
            }) {
                Text("Review Data")
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(evidenceList) { evidence ->
                Text(text = evidence.text, modifier = Modifier.padding(8.dp))
            }
        }
    }
}
