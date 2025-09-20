package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.ui.components.LexorcistOutlinedButton

// Data Models for JSON Schema
data class DynamicScreenModel(
    val title: String = "Scripted Screen",
    val elements: List<UIElement> = emptyList()
)

data class UIElement(
    val type: String,
    val properties: Map<String, Any> = emptyMap()
)

// Main Composable for the Scripted Screen
@Composable
fun ScriptedScreen(
    schemaJson: String?,
    onAction: (String) -> Unit
) {
    var screenModel by remember { mutableStateOf<DynamicScreenModel?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(schemaJson) {
        if (schemaJson.isNullOrBlank()) {
            error = "No UI schema provided."
            return@LaunchedEffect
        }
        try {
            val gson = Gson()
            val model = gson.fromJson(schemaJson, DynamicScreenModel::class.java)
            screenModel = model
            error = null
        } catch (e: Exception) {
            e.printStackTrace()
            error = "Failed to parse UI schema: ${e.message}"
            screenModel = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // This screen will be placed inside a Scaffold from MainScreen,
        // so we don't need to define our own Scaffold here.
        // The title will be handled by the TopAppBar in MainScreen.

        if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
        } else if (screenModel != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(screenModel!!.elements) { element ->
                    RenderUIElement(element, onAction)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

// Composable to render a single UI element based on its type
@Composable
private fun RenderUIElement(element: UIElement, onAction: (String) -> Unit) {
    when (element.type.lowercase()) {
        "text" -> {
            val text = element.properties["text"] as? String ?: ""
            val size = (element.properties["size"] as? Double)?.toFloat() ?: 16f
            Text(text = text, fontSize = size.sp)
        }
        "button" -> {
            val label = element.properties["label"] as? String ?: "Button"
            val action = element.properties["onClickAction"] as? String
            LexorcistOutlinedButton(
                onClick = {
                    if (action != null) {
                        onAction(action)
                    }
                },
                text = label,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "spacer" -> {
            val height = (element.properties["height"] as? Double)?.toFloat() ?: 8f
            Spacer(modifier = Modifier.height(height.dp))
        }
        "textfield" -> {
             // Basic implementation, would need state handling for a real app
            val label = element.properties["label"] as? String ?: ""
            var text by remember { mutableStateOf("") }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        else -> {
            Text(
                text = "Unsupported element type: ${element.type}",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
    }
}
