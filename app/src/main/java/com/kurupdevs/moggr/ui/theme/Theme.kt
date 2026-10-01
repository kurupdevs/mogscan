package com.kurupdevs.moggr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MogCoral = Color(0xFFE07856)
private val MogCoralDark = Color(0xFFC25E3C)

/** Always-light warm editorial theme: cream background, ink text, coral accent. */
private val LightColors = lightColorScheme(
    primary = MogCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBEFE3),
    onPrimaryContainer = MogCoralDark,
    background = Color(0xFFFAF7F1),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFF4EFE7),
    onSurfaceVariant = Color(0xFF78716C),
    outline = Color(0xFFD8D0C2)
)

@Composable
fun MoggrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
