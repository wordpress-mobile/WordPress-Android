package org.wordpress.android.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import org.wordpress.android.BuildConfig

/**
 * Project's base theme.
 */
@Composable
fun AppTheme(
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
