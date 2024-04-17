package org.wordpress.android.ui.compose.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Shimmer.color)
            .shimmerLoadingAnimation()
    )
}
