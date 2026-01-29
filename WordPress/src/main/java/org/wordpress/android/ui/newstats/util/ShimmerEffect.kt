package org.wordpress.android.ui.newstats.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

private const val SHIMMER_ANIMATION_DURATION_MS = 1200
private const val SHIMMER_TARGET_VALUE = 1000f
private const val SHIMMER_OFFSET = 500f
private const val SHIMMER_ALPHA_LOW = 0.3f
private const val SHIMMER_ALPHA_HIGH = 0.6f

/**
 * A reusable shimmer effect composable that provides a shimmer brush for loading states.
 *
 * The shimmer animation is automatically paused when this composable leaves the composition,
 * making it lifecycle-aware and efficient.
 *
 * @param modifier The modifier to apply to the shimmer box
 * @param content Optional content to display inside the shimmer box. If null, displays a simple
 *                shimmer placeholder.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    val shimmerBrush = rememberShimmerBrush()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(shimmerBrush)
    ) {
        content?.invoke(this)
    }
}

/**
 * Creates and remembers a shimmer brush that can be used for custom shimmer implementations.
 *
 * The animation is automatically disposed when the composable leaves the composition.
 *
 * @return A [Brush] that creates a shimmer effect
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SHIMMER_ALPHA_LOW),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SHIMMER_ALPHA_HIGH),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SHIMMER_ALPHA_LOW)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = SHIMMER_TARGET_VALUE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - SHIMMER_OFFSET, 0f),
        end = Offset(translateAnimation, 0f)
    )
}
