package org.wordpress.android.ui.domains.management.composable

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.wordpress.android.ui.themes.ghost


@Composable
fun PendingGhostStrip(width: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pending ghost strip transition")
    val color by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.ghost.copy(alpha = 0.06f),
        targetValue = MaterialTheme.colorScheme.ghost.copy(alpha = 0.1f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Pending ghost strip color"
    )
    Box(
        modifier = Modifier
            .width(width)
            .height(lineHeightDp)
            .background(color)
    )
}

private val lineHeightDp
    @Composable
    get() = with(LocalDensity.current) {
        LocalTextStyle.current.lineHeight.toDp()
    }
