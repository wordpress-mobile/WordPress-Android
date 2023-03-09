package org.wordpress.android.ui.compose.components

import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Helper Composable to provide the [LocalContentAlpha] to the content. Prefer using the values below to match Material
 * guidelines regarding content emphasis:
 * - [ContentAlpha.high]
 * - [ContentAlpha.medium]
 * - [ContentAlpha.disabled]
 *
 * More info: https://developer.android.com/jetpack/compose/designsystems/material#emphasis
 */
@Composable
fun ContentAlphaProvider(
    alpha: Float,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentAlpha provides alpha, content = content)
}
