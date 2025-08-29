package com.hereliesaz.lexorcist.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialExpressiveTheme
import kotlin.random.Random
@ExperimentalMaterial3Api
@Composable
fun LexorcistTheme(content: @Composable () -> Unit) {
    val randomColor = Color(
        red = Random.nextInt(256),
        green = Random.nextInt(256),
        blue = Random.nextInt(256)
    )
    DynamicMaterialExpressiveTheme(
        seedColor = randomColor,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
