package org.wordpress.android.ui.compose.utils

import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import org.wordpress.android.ui.compose.components.ContentAlphaProvider

/**
 * Utility function that returns a Composable function that wraps the [content] inside a [ContentAlphaProvider]
 * setting the [LocalContentAlpha] to 1f. Useful for using with some Material Composables that override that alpha
 * Composition Local in a hard-coded fashion (e.g.: TopAppBar). This should not need to be used very often.
 */
fun withFullContentAlpha(content: @Composable () -> Unit): @Composable () -> Unit = {
    ContentAlphaProvider(
        1f,
        content = content
    )
}

/**
 * Indicates whether the currently selected theme is light.
 * @return true if the current theme is light
 */
@Composable
fun isLightTheme(): Boolean = MaterialTheme.colors.isLight
