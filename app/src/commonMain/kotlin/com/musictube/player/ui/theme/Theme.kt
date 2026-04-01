package com.musictube.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary              = VibrantPurple,
    onPrimary            = Color.White,
    primaryContainer     = PurpleContainer,
    onPrimaryContainer   = LightLavender,
    secondary            = MutedViolet,
    onSecondary          = Color.White,
    secondaryContainer   = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContain,
    tertiary             = PinkAccent,
    onTertiary           = Color.White,
    tertiaryContainer    = PinkContainer,
    onTertiaryContainer  = OnPinkContainer,
    error                = ErrorRed,
    onError              = OnError,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    background           = DeepPurpleBlack,
    onBackground         = WarmWhite,
    surface              = DarkPurpleSurface,
    onSurface            = SoftLavenderText,
    surfaceVariant       = SurfaceVariantColor,
    onSurfaceVariant     = DimSubtleText,
    outline              = OutlineColor,
    outlineVariant       = OutlineVariant,
    inverseSurface       = SoftLavenderText,
    inverseOnSurface     = DeepPurpleBlack,
    inversePrimary       = NeonPurpleGlow,
    scrim                = Color(0x99000000)
)

private val LightColorScheme = lightColorScheme(
    primary   = Purple40,
    secondary = PurpleGrey40,
    tertiary  = Pink40
)

@Composable
fun MusicTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // System-bar tinting is platform-specific; handled in androidMain/MainActivity.
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
