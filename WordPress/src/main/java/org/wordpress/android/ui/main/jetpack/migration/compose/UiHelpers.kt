package org.wordpress.android.ui.main.jetpack.migration.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

const val DIM_ALPHA = 0.2f

fun Modifier.dimmed(shouldDim: Boolean) = alpha(if (shouldDim) DIM_ALPHA else 1f)
