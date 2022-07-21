package org.wordpress.android.ui.qrcodeauth.compose.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.wordpress.android.ui.compose.components.IndeterminateCircularProgress

@Composable
fun LoadingState() {
    Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
    ) {
        IndeterminateCircularProgress()
    }
}
