package com.musictube.player.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Music app colors
val MusicPrimary = Color(0xFF7C3AED) // Deep purple
val MusicPrimaryVariant = Color(0xFF9D5CF6) // Light purple
val MusicSecondary = Color(0xFF191414) // Dark background
val MusicBackground = Color(0xFF121212)
val MusicSurface = Color(0xFF282828)
val MusicOnPrimary = Color.White
val MusicOnSecondary = Color.White
val MusicOnBackground = Color.White
val MusicOnSurface = Color.White

private val DarkColorScheme = darkColorScheme(
    primary = MusicPrimary,
    secondary = MusicSecondary,
    background = MusicBackground,
    surface = MusicSurface,
    onPrimary = MusicOnPrimary,
    onSecondary = MusicOnSecondary,
    onBackground = MusicOnBackground,
    onSurface = MusicOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@androidx.compose.runtime.Composable
fun MusicTubeTheme(
    darkTheme: Boolean = true, // Always use dark theme for music app
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}