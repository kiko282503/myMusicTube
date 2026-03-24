package com.musictube.player.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary                = VibrantPurple,
    onPrimary              = Color.White,
    primaryContainer       = PurpleContainer,
    onPrimaryContainer     = LightLavender,
    secondary              = MutedViolet,
    onSecondary            = Color.White,
    secondaryContainer     = SecondaryContainer,
    onSecondaryContainer   = OnSecondaryContain,
    tertiary               = PinkAccent,
    onTertiary             = Color.White,
    tertiaryContainer      = PinkContainer,
    onTertiaryContainer    = OnPinkContainer,
    error                  = ErrorRed,
    onError                = OnError,
    errorContainer         = ErrorContainer,
    onErrorContainer       = OnErrorContainer,
    background             = DeepPurpleBlack,
    onBackground           = WarmWhite,
    surface                = DarkPurpleSurface,
    onSurface              = SoftLavenderText,
    surfaceVariant         = SurfaceVariantColor,
    onSurfaceVariant       = DimSubtleText,
    outline                = OutlineColor,
    outlineVariant         = OutlineVariant,
    inverseSurface         = SoftLavenderText,
    inverseOnSurface       = DeepPurpleBlack,
    inversePrimary         = NeonPurpleGlow,
    scrim                  = Color(0x99000000),
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun MusicTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepPurpleBlack.toArgb()
            window.navigationBarColor = DeepPurpleBlack.toArgb()
            // Light status bar icons (dark icons on light background)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}