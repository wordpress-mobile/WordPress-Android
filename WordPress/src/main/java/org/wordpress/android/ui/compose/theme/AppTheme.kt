package org.wordpress.android.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import org.wordpress.android.BuildConfig

/**
 * This theme should be used to support light/dark colors if the root composable does not support
 * [contentColor](https://developer.android.com/jetpack/compose/themes/material#content-color).
 */
@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppThemeWithoutBackground(isDarkTheme) {
        ContentInSurface(content)
    }
}

/**
 * Use this theme only when the root composable supports
 * [contentColor](https://developer.android.com/jetpack/compose/themes/material#content-color).
 * Otherwise use [AppTheme].
 */
@Composable
fun AppThemeWithoutBackground(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorPalette = when (BuildConfig.IS_JETPACK_APP) {
        true -> JpColorPalette(isDarkTheme)
        else -> WpColorPalette(isDarkTheme)
    }

    MaterialTheme(
        colors = colorPalette,
        content = content
    )
}

@Composable
private fun ContentInSurface(
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colors.background) {
        content()
    }
}
