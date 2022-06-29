package org.wordpress.android.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

/**
 * Project's base theme.
 */
@Composable
fun WordPressTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
            colors = if (isDarkTheme) getDarkColors() else getLightColors(),
            content = content
    )
}

private fun getLightColors() = lightColors(
        primary = blue50,
        primaryVariant = blue70,
        secondary = blue50,
        secondaryVariant = blue70,
        background = white,
        surface = white,
        error = red50,
        onPrimary = white,
        onSecondary = white,
        onBackground = black,
        onSurface = black,
        onError = white
)

private fun getDarkColors() = darkColors(
        primary = blue30,
        primaryVariant = blue50,
        secondary = blue30,
        secondaryVariant = blue50,
        background = white,
        surface = white,
        error = red50,
        onPrimary = white,
        onSecondary = white,
        onBackground = black,
        onSurface = black,
        onError = white
)
