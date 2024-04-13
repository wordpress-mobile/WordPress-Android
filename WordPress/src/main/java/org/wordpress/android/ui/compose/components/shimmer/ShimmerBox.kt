package org.wordpress.android.ui.compose.components.shimmer

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    isLoadingCompleted: Boolean = true,
    isLightModeActive: Boolean = true,
    widthOfShadowBrush: Int = 500,
    angleOfAxisY: Float = 270f,
    durationMillis: Int = 1000,
) {
    Box(
        modifier = modifier
            .shimmerLoadingAnimation(
                isLoadingCompleted = isLoadingCompleted,
                isLightModeActive = isLightModeActive,
                widthOfShadowBrush = widthOfShadowBrush,
                angleOfAxisY = angleOfAxisY,
                durationMillis = durationMillis,
            )
    )
}
