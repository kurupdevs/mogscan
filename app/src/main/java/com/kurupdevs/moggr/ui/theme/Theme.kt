package com.kurupdevs.moggr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MogPurple = Color(0xFF7C5CFF)
private val MogPurpleDark = Color(0xFF5A3FD4)

private val DarkColors = darkColorScheme(
    primary = MogPurple,
    onPrimary = Color.White,
    background = Color(0xFF0E0E12),
    surface = Color(0xFF15151C),
    surfaceVariant = Color(0xFF1E1E28),
    onBackground = Color(0xFFF2F0FA),
    onSurface = Color(0xFFF2F0FA),
    onSurfaceVariant = Color(0xFFB9B4CC)
)

private val LightColors = lightColorScheme(
    primary = MogPurpleDark,
    onPrimary = Color.White
)

@Composable
fun MoggrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
