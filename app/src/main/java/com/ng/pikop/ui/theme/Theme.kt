package com.ng.pikop.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand Identity: Light Scheme
private val PikopLightColorScheme = lightColorScheme(
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

// Premium Dark Mode (v4.7 Hardening)
private val PikopDarkColorScheme = darkColorScheme(
    primary = PikopGreen,
    onPrimary = PikopWhite,
    secondary = PikopGold,
    onSecondary = PikopNearBlack,
    tertiary = PikopOrange,
    onTertiary = PikopWhite,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE1E1E1),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = PikopLemonGreen,
    error = AlertOrange,
    onError = PikopWhite,
    outline = Color(0xFF373737),
    outlineVariant = Color(0xFF484848)
)

@Composable
fun PikopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to maintain brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PikopDarkColorScheme else PikopLightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // Invert icon colors based on theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
