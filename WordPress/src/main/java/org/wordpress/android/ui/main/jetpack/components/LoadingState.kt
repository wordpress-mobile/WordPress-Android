package org.wordpress.android.ui.main.jetpack.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingState() {
    Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
    }
}
