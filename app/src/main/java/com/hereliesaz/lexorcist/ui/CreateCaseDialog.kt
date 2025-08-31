package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.viewmodel.CaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCaseDialog(
    caseViewModel: CaseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var caseName by remember { mutableStateOf("") }
    val defaultExhibitSheetNameStr = stringResource(R.string.default_exhibit_sheet_name)
    var exhibitSheetName by remember { mutableStateOf(defaultExhibitSheetNameStr) }
    var caseNumber by remember { mutableStateOf("") }
    var caseSection by remember { mutableStateOf("") }
    var caseJudge by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_new_case)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = caseName,
                    onValueChange = { caseName = it },
                    label = { Text(stringResource(R.string.case_name_required)) },
                    isError = caseName.isBlank()
                )
                TextField(
                    value = exhibitSheetName,
                    onValueChange = { exhibitSheetName = it },
                    label = { Text(stringResource(R.string.exhibit_sheet_name)) }
                )
                TextField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = { Text(stringResource(R.string.case_number)) }
                )
                TextField(
                    value = caseSection,
                    onValueChange = { caseSection = it },
                    label = { Text(stringResource(R.string.case_section)) }
                )
                TextField(
                    value = caseJudge,
                    onValueChange = { caseJudge = it },
                    label = { Text(stringResource(R.string.judge)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (caseName.isNotBlank()) {
                        caseViewModel.createNewCaseWithRepository(
                            caseName = caseName,
                            exhibitSheetName = exhibitSheetName.ifBlank { context.getString(R.string.default_exhibit_sheet_name) },
                            caseNumber = caseNumber,
                            caseSection = caseSection,
                            caseJudge = caseJudge
                        )
                        onDismiss()
                    }
                },
                enabled = caseName.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
