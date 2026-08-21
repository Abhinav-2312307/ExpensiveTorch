package com.example.expensiveflashlight.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkGoldColorScheme = darkColorScheme(
    // Primary: Main gold accent
    primary = GoldPrimary,
    onPrimary = OnGoldText,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldBright,

    // Secondary: Bright gold
    secondary = GoldBright,
    onSecondary = OnGoldText,
    secondaryContainer = SurfaceMedium,
    onSecondaryContainer = GoldBright,

    // Tertiary: Muted gold
    tertiary = GoldMuted,
    onTertiary = OnGoldText,
    tertiaryContainer = SurfaceLight,
    onTertiaryContainer = GoldMuted,

    // Background
    background = BackgroundDeepBlack,
    onBackground = OnDarkText,

    // Surface
    surface = BackgroundDark,
    onSurface = OnDarkText,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnDarkTextSecondary,

    // Error
    error = ErrorRed,
    onError = OnDarkText,
    errorContainer = ErrorRedDark,
    onErrorContainer = ErrorRed,

    // Outline
    outline = OutlineGold,
    outlineVariant = OutlineSubtle,

    // Inverse
    inverseSurface = OnDarkText,
    inverseOnSurface = BackgroundDeepBlack,
    inversePrimary = GoldDark,

    // Scrim
    scrim = Color.Black
)

@Composable
fun ExpensiveFlashlightTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkGoldColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent status bar for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            // Transparent navigation bar for edge-to-edge
            window.navigationBarColor = Color.Transparent.toArgb()
            // Light status bar icons = false → white icons on dark background
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
