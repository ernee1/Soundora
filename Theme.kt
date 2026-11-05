package com.soundora.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF8B5CF6)
val Blue = Color(0xFF3B82F6)
val Navy = Color(0xFF0B1020)
val Card = Color(0xFF141B33)

@Composable
fun SoundoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple, secondary = Blue,
            background = Navy, surface = Card,
            onBackground = Color.White, onSurface = Color.White
        ),
        content = content
    )
}
