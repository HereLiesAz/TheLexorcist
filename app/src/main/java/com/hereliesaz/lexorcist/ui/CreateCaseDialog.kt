package com.hereliesaz.lexorcist.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth // Added import
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField // Changed from TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment // Added import
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCaseDialog(
    caseViewModel: CaseViewModel,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var caseName by remember { mutableStateOf("") }
    val defaultExhibitSheetNameStr = stringResource(R.string.default_exhibit_sheet_name)
    var exhibitSheetName by remember { mutableStateOf(defaultExhibitSheetNameStr) }
    var caseNumber by remember { mutableStateOf("") }
    var caseSection by remember { mutableStateOf("") }
    var caseJudge by remember { mutableStateOf("") }

    val userRecoverableAuthIntent by caseViewModel.userRecoverableAuthIntent.collectAsState()

    val authLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Retry the operation or inform the user of success
                // For now, we assume the operation might need to be retried by the user if it didn't auto-proceed.
                // Optionally, call createCase again or a specific retry logic in ViewModel.
            } else {
                // Handle the case where the user did not complete the auth flow
                caseViewModel.showError("Authorization was not completed.")
            }
            caseViewModel.clearUserRecoverableAuthIntent() // Clear the intent after handling
        }

    LaunchedEffect(userRecoverableAuthIntent) {
        userRecoverableAuthIntent?.let {
            authLauncher.launch(it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_new_case)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                // Ensure Column itself takes full width for alignment reference
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End, // Right-align children (TextFields)
            ) {
                OutlinedTextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    label = { Text(stringResource(R.string.case_name_required)) },
                    isError = caseName.isBlank(),
                    modifier = Modifier.fillMaxWidth(), // TextField takes full width
                    textStyle = TextStyle(textAlign = TextAlign.End)
                )
                OutlinedTextField(
                    value = exhibitSheetName,
                    onValueChange = { exhibitSheetName = it },
                    label = { Text(stringResource(R.string.exhibit_sheet_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(textAlign = TextAlign.End)
                )
                OutlinedTextField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = { Text(stringResource(R.string.case_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(textAlign = TextAlign.End)
                )
                OutlinedTextField(
                    value = caseSection,
                    onValueChange = { caseSection = it },
                    label = { Text(stringResource(R.string.case_section)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(textAlign = TextAlign.End)
                )
                OutlinedTextField(
                    value = caseJudge,
                    onValueChange = { caseJudge = it },
                    label = { Text(stringResource(R.string.judge)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(textAlign = TextAlign.End)
                )
            }
        },
        confirmButton = {
            LexorcistOutlinedButton(
                onClick = {
                    if (caseName.isNotBlank()) {
                        caseViewModel.createCase(
                            caseName = caseName,
                            exhibitSheetName = exhibitSheetName.ifBlank { context.getString(R.string.default_exhibit_sheet_name) },
                            caseNumber = caseNumber,
                            caseSection = caseSection,
                            caseJudge = caseJudge,
                        )
                        // Dismissal and navigation should ideally happen based on successful case creation
                        // For now, keeping original logic. Consider observing a success event from ViewModel.
                        onDismiss()
                        navController.navigate("cases")
                    }
                },
                content = { Text(stringResource(R.string.create)) } // Explicitly use content
            )
        },
        dismissButton = {
            LexorcistOutlinedButton(
                onClick = onDismiss, 
                content = { Text(stringResource(R.string.cancel)) } // Explicitly use content
            )
        },
    )
}
