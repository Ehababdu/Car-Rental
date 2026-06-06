package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Global theme state for compose-level reactivity
val isAppInDarkMode = mutableStateOf(false)

// Bold Typography Brand Colors with high-contrast accessibility support for light and dark modes
val BrandPrimary: Color
    get() = if (isAppInDarkMode.value) Color(0xFF90CAF9) else Color(0xFF0061A4)

val BrandSecondary: Color
    get() = if (isAppInDarkMode.value) Color(0xFFE2E8F0) else Color(0xFF001D36)

val BrandBackground: Color
    get() = if (isAppInDarkMode.value) Color(0xFF0F172A) else Color(0xFFFDFBFF)

val BrandSurface: Color
    get() = if (isAppInDarkMode.value) Color(0xFF1E293B) else Color(0xFFE1E2EC)

val BrandCard: Color
    get() = if (isAppInDarkMode.value) Color(0xFF1E293B) else Color(0xFFFFFFFF)

val BrandText: Color
    get() = if (isAppInDarkMode.value) Color(0xFFF8FAFC) else Color(0xFF1A1C1E)

val BrandAccent: Color
    get() = if (isAppInDarkMode.value) Color(0xFF94A3B8) else Color(0xFF43474E)

val BrandGreen: Color
    get() = if (isAppInDarkMode.value) Color(0xFF4ADE80) else Color(0xFF00875A)

val BrandOrange: Color
    get() = if (isAppInDarkMode.value) Color(0xFFFBBF24) else Color(0xFFD97706)

val BrandIndigo: Color
    get() = if (isAppInDarkMode.value) Color(0xFF818CF8) else Color(0xFF4F46E5)

val BrandTeal: Color
    get() = if (isAppInDarkMode.value) Color(0xFF2DD4BF) else Color(0xFF0D9488)

val BrandRed: Color
    get() = if (isAppInDarkMode.value) Color(0xFFF87171) else Color(0xFFDC2626)


