package org.wordpress.android.ui.compose.utils

import androidx.compose.material.LocalContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Utility function that returns a Composable function that wraps the [content] inside a [CompositionLocalProvider]
 * setting the [LocalContentAlpha] to 1f. Useful for using with some Material Composables that override that alpha
 * Composition Local in a hard-coded fashion (e.g.: TopAppBar). This should not need to be used very often.
 */
fun withFullContentAlpha(content: @Composable () -> Unit): @Composable () -> Unit = {
    CompositionLocalProvider(
        LocalContentAlpha provides 1f,
        content = content
    )
}
