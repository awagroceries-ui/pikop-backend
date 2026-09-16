package com.ng.pikop.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Forced brand identity: Light background even in dark mode for brand consistency
private val BrandColorScheme = lightColorScheme(
    primary = PikopGreen,
    onPrimary = PikopWhite,
    secondary = PikopGold,
    onSecondary = PikopNearBlack,
    tertiary = PikopOrange,
    onTertiary = PikopWhite,
    background = PikopWhite,
    onBackground = PikopNearBlack,
    surface = PikopWhite,
    onSurface = PikopNearBlack,
    surfaceVariant = PikopLemonGreen,
    onSurfaceVariant = PikopGreen,
    error = AlertOrange,
    onError = PikopWhite,
    outline = PikopGrey,
    outlineVariant = PikopDarkGrey
)

@Composable
fun PikopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to maintain brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Rebranding requirement: Strict adherence to white background
    val colorScheme = BrandColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use white status bar for the new white background brand
            window.statusBarColor = colorScheme.background.toArgb()
            // Ensure icons are dark on white background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
