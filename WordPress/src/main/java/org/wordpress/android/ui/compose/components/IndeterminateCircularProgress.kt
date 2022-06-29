package org.wordpress.android.ui.compose.components

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun IndeterminateCircularProgress(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
            modifier = modifier
    )
}
