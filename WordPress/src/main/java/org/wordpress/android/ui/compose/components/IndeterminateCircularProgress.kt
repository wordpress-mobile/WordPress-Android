package org.wordpress.android.ui.compose.components

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Indeterminate circular progress UI component. Used for operations that we don't how long are going to take.
 */
@Composable
fun IndeterminateCircularProgress(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
        modifier = modifier
    )
}
