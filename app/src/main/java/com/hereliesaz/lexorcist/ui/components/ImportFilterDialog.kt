package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@Composable
fun ImportFilterDialog(
    onDismiss: () -> Unit,
    onImport: (contact: String?, startDate: Long?, endDate: Long?, importSms: Boolean, importCalls: Boolean) -> Unit
) {
    var contact by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var importSms by remember { mutableStateOf(true) }
    var importCalls by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_device_record_import)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = importSms, onCheckedChange = { importSms = it })
                    Text(stringResource(R.string.import_sms))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = importCalls, onCheckedChange = { importCalls = it })
                    Text(stringResource(R.string.import_call_logs))
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text(stringResource(R.string.contact_number_optional)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text(stringResource(R.string.start_date_yyyy_mm_dd)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text(stringResource(R.string.end_date_yyyy_mm_dd)) }
                )
            }
        },
        confirmButton = {
            AzButton(
                onClick = {
                    val startMillis = startDate.toEpochMillis()
                    val endMillis = endDate.toEpochMillis()
                    onImport(contact.ifBlank { null }, startMillis, endMillis, importSms, importCalls)
                    onDismiss()
                },
                text = stringResource(R.string.import_action)
            )
        },
        dismissButton = {
            AzButton(
                onClick = onDismiss,
                text = stringResource(R.string.cancel)
            )
        }
    )
}

private fun String.toEpochMillis(): Long? {
    if (this.isBlank()) return null
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.parse(this)?.time
    } catch (e: Exception) {
        null
    }
}