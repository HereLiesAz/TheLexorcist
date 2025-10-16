package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.lexorcist.model.UiComponentModel
import com.hereliesaz.lexorcist.service.ScriptRunner
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DynamicUiRenderer(
    components: List<UiComponentModel>,
    scriptRunner: ScriptRunner
) {
    val coroutineScope = rememberCoroutineScope()
    Column {
        components.forEach { component ->
            when (component.type) {
                "button" -> {
                    val onClickScript = remember(component.onClick) { component.onClick }
                    AzButton(onClick = {
                        onClickScript?.let {
                            coroutineScope.launch {
                                scriptRunner.runGenericScript(it, emptyMap())
                            }
                        }
                    }) {
                        Text(text = component.properties["text"] ?: "")
                    }
                }
                "text" -> {
                    Text(text = component.properties["text"] ?: "")
                }
            }
        }
    }
}
