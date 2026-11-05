package com.inkr8.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Inkr8Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFFD700), secondary = Color(0xFFE8C65A),
        background = Color(0xFF111318), surface = Color(0xFF1D2027), onPrimary = Color(0xFF171717)), content = content)
}
