package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// --- Data classes for JSON parsing ---

data class ScreenModel(
    @SerializedName("title") val title: String = "Scripted Screen",
    @SerializedName("components") val components: List<ComponentModel> = emptyList()
)

data class ComponentModel(
    @SerializedName("type") val type: String,
    // Common properties
    @SerializedName("id") val id: String? = null,
    // Text properties
    @SerializedName("content") val content: String? = null,
    @SerializedName("style") val style: String? = null,
    // Button properties
    @SerializedName("label") val label: String? = null,
    @SerializedName("onClickFunction") val onClickFunction: String? = null
)


/**
 * A Composable that dynamically builds a UI from a JSON schema.
 *
 * @param screenJson The JSON string defining the screen layout and components.
 * @param onJsAction A callback to execute a Javascript function by name, triggered by UI events.
 * @param onTitleResolved A callback to pass the parsed screen title up to the parent Scaffold.
 */
@Composable
fun ScriptedScreen(
    screenJson: String,
    onJsAction: (String) -> Unit,
    onTitleResolved: (String) -> Unit
) {
    val gson = remember { Gson() }
    val screenModel = remember(screenJson) {
        try {
            gson.fromJson(screenJson, ScreenModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace() // Log the error for debugging
            null // Return null on parsing failure
        }
    }

    if (screenModel == null) {
        // Show an error message if JSON is invalid or parsing fails
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Error: The dynamic screen could not be loaded. The format is invalid.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        // Set a default error title
        LaunchedEffect(Unit) {
            onTitleResolved("Error")
        }
        return
    }

    // Pass the parsed title up to the parent Composable (e.g., MainScreen's TopAppBar)
    LaunchedEffect(screenModel.title) {
        onTitleResolved(screenModel.title)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(screenModel.components) { component ->
            when (component.type) {
                "text" -> {
                    val textStyle = when (component.style) {
                        "headline" -> MaterialTheme.typography.headlineMedium
                        "title" -> MaterialTheme.typography.titleLarge
                        "body" -> MaterialTheme.typography.bodyMedium
                        "caption" -> MaterialTheme.typography.bodySmall
                        else -> MaterialTheme.typography.bodyLarge // Default style
                    }
                    Text(
                        text = component.content ?: "",
                        style = textStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "button" -> {
                    Button(
                        onClick = {
                            component.onClickFunction?.let { onJsAction(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(component.label ?: "Button")
                    }
                }
                // A simple spacer component
                "spacer" -> {
                    Box(modifier = Modifier.padding(8.dp))
                }
                else -> {
                    // Render a placeholder for unknown component types
                    Text(
                        text = "Unknown component type: '${component.type}'",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
