package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hereliesaz.lexorcist.R

@Composable
fun RequestDialog(
    onDismissRequest: () -> Unit,
    onSendRequest: (name: String, email: String, request: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var request by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.make_a_request)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.share_addon_name_label)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = request,
                    onValueChange = { request = it },
                    label = { Text(stringResource(R.string.request)) },
                    placeholder = { Text(stringResource(R.string.i_take_requests_tell_me_what_you_need_be_specific)) }
                )
            }
        },
        confirmButton = {
            LexorcistOutlinedButton(
                onClick = {
                    onSendRequest(name, email, request)
                },
                text = stringResource(R.string.send)
            )
        },
        dismissButton = {
            LexorcistOutlinedButton(
                onClick = onDismissRequest,
                text = stringResource(R.string.cancel)
            )
        }
    )
}
