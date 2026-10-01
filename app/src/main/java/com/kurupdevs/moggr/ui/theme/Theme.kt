package com.kurupdevs.moggr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MogBlue = Color(0xFF2F6BFF)
private val MogBlueDark = Color(0xFF1E4FD1)

/** Always-light fintech theme: white cards on a soft blue-grey background. */
private val LightColors = lightColorScheme(
    primary = MogBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EBFF),
    onPrimaryContainer = MogBlueDark,
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF101828),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEAF0FA),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD0D5DD)
)

@Composable
fun MoggrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
