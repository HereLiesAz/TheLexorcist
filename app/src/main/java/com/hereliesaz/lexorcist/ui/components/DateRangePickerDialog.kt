package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startDate: Long, endDate: Long) -> Unit
) {
    var startDateString by remember { mutableStateOf("") }
    var endDateString by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isPickingStartDate by remember { mutableStateOf(true) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it
                        if (isPickingStartDate) {
                            startDateString = sdf.format(cal.time)
                        } else {
                            endDateString = sdf.format(cal.time)
                        }
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_date_range)) },
        text = {
            Column {
                OutlinedTextField(
                    value = startDateString,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.start_date_yyyy_mm_dd)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clickable {
                            isPickingStartDate = true
                            showDatePicker = true
                        }
                )
                OutlinedTextField(
                    value = endDateString,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.end_date_yyyy_mm_dd)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            isPickingStartDate = false
                            showDatePicker = true
                        }
                )
            }
        },
        confirmButton = {
            AzButton(
                onClick = {
                    try {
                        val startMillis = sdf.parse(startDateString)?.time ?: 0L
                        val endMillis = sdf.parse(endDateString)?.let {
                            val cal = Calendar.getInstance()
                            cal.time = it
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            cal.timeInMillis
                        } ?: 0L

                        if (startMillis > 0 && endMillis > 0 && startMillis <= endMillis) {
                            onConfirm(startMillis, endMillis)
                        } else {
                            // Handle invalid date range
                        }
                    } catch (e: Exception) {
                        // Handle parsing error
                    }
                },
                text = stringResource(R.string.ok)
            )
        },
        dismissButton = {
            AzButton(onClick = onDismiss, text = stringResource(R.string.cancel))
        }
    )
}