package org.wordpress.android.posttypes.bridge

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Temporary theme wrapper for the Post Types module.
 *
 * ## Purpose
 * Provides a standalone Material3 theme that doesn't depend on the main app's theming system.
 * This allows the module to be compiled and tested independently.
 *
 * ## Migration Notes
 * When merging this module back into the main app:
 * - Replace [CptTheme] usages with `AppThemeM3`
 * - Remove this file entirely
 *
 * @see org.wordpress.android.posttypes.bridge package documentation
 */
@Composable
fun CptTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

// Basic color schemes - these approximate the WordPress/Jetpack themes
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0675C4),
    secondary = Color(0xFF0675C4),
    background = Color.White,
    surface = Color.White,
    error = Color(0xFFD63638),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6EC2FB),
    secondary = Color(0xFF6EC2FB),
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFFF8085),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)
