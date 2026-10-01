package com.kurupdevs.moggr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// v2.7 reskin: equilibrium-style warm greige. Coral accent kept for brand continuity.
val MogCoral = Color(0xFFE07856)
val MogCoralDark = Color(0xFFC25E3C)

// Greige surfaces
val EqGreige = Color(0xFFE9E2D6)
val EqGreigeDeep = Color(0xFFDCD3C2)
val EqCard = Color(0xFFF6F2EA)
val EqInk = Color(0xFF1C1917)
val EqInkSoft = Color(0xFF44403C)
val EqMuted = Color(0xFF8A8177)
val EqLine = Color(0xFFD5CBB9)

// Pastel accents (feature tiles)
val EqLavender = Color(0xFFDCD5F3)
val EqLavenderDeep = Color(0xFFB9AEE8)
val EqSage = Color(0xFFD3E2D5)
val EqSageDeep = Color(0xFFA9C8AE)
val EqPeach = Color(0xFFF7DDC6)
val EqPeachDeep = Color(0xFFF0BE93)
val EqRose = Color(0xFFF3D3D3)
val EqSky = Color(0xFFD4E2EE)

// Dark pill
val EqPillDark = Color(0xFF23201B)

// Teal accent (secondary pills, reference style)
val EqTeal = Color(0xFF6FA8A0)

/** Always-light warm greige theme: equilibrium-style calm, Moggr coral accent. */
private val LightColors = lightColorScheme(
    primary = MogCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBEFE3),
    onPrimaryContainer = MogCoralDark,
    background = EqGreige,
    onBackground = EqInk,
    surface = EqCard,
    onSurface = EqInk,
    surfaceVariant = Color(0xFFF1EBDD),
    onSurfaceVariant = EqMuted,
    outline = EqLine,
    secondary = EqInk,
    onSecondary = Color.White,
    tertiary = EqSageDeep,
    onTertiary = EqInk
)

@Composable
fun MoggrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
