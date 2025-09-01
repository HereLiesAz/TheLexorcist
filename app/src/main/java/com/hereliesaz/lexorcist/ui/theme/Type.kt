package com.hereliesaz.lexorcist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hereliesaz.lexorcist.R

// Define the custom font family
val LexorcistFontFamily = FontFamily(
    Font(R.font.itheabook, FontWeight.Normal),
    Font(R.font.itheamedium, FontWeight.Medium),
    Font(R.font.itheabold, FontWeight.Bold)
)

// Set of Material typography styles to start with, overriding with the custom font
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Normal, // itheabook
        fontSize = 55.sp, // Was 57.sp
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Normal, // itheabook
        fontSize = 43.sp, // Was 45.sp
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Normal, // itheabook
        fontSize = 34.sp, // Was 36.sp
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Bold, // itheabold
        fontSize = 30.sp, // Was 32.sp
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Medium, // itheamedium
        fontSize = 26.sp, // Was 28.sp
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Medium, // itheamedium
        fontSize = 22.sp, // Was 24.sp
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Bold, // itheabold
        fontSize = 20.sp, // Was 22.sp
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Medium, // itheamedium
        fontSize = 14.sp, // Was 16.sp
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Medium, // itheamedium
        fontSize = 12.sp, // Was 14.sp
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Normal, // itheabook
        fontSize = 14.sp, // Was 16.sp
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Normal, // itheabook
        fontSize = 12.sp, // Was 14.sp
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Normal, // itheabook
        fontSize = 10.sp, // Was 12.sp
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Medium, // itheamedium
        fontSize = 12.sp, // Was 14.sp
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Medium, // itheamedium
        fontSize = 10.sp, // Was 12.sp
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = LexorcistFontFamily,
        fontWeight = FontWeight.Medium, // itheamedium
        fontSize = 9.sp,  // Was 11.sp
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
