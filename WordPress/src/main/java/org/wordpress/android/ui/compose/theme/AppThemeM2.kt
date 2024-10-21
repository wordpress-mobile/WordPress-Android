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
fun AppThemeM2(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppThemeM2WithoutBackground(isDarkTheme) {
        ContentInSurface(content)
    }
}

/**
 * Use this theme only when the root composable supports
 * [contentColor](https://developer.android.com/jetpack/compose/themes/material#content-color).
 * Otherwise use [AppThemeM2].
 */
@Composable
fun AppThemeM2WithoutBackground(
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

/**
 * This theme should *only* be used in the context of the Editor (e.g. Post Settings).
 * More info: https://github.com/wordpress-mobile/gutenberg-mobile/issues/4889
 */
@Composable
fun AppThemeM2Editor(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WpColorPalette(isDarkTheme),
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
