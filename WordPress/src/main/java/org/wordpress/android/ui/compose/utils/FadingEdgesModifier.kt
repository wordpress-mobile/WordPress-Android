package org.wordpress.android.ui.compose.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

// Fading edges for scrollable containers based on
// https://medium.com/@helmersebastian/fading-edges-modifier-in-jetpack-compose-af94159fdf1f

/**
 * Adds vertical fading edges to a scrollable container, usually a [Column].
 * It needs to be used right after the scrollable modifier [verticalScroll].
 *
 * The [topEdgeSize] defines the maximum size of the fading edge effect, which is used when that same amount of the
 * content is outside the scrollable area.
 *
 * @param scrollState the scroll state of the scrollable container (same used in [Modifier.verticalScroll])
 * @param topEdgeSize the size of the fading edge on the top
 * @param bottomEdgeSize the size of the fading edge on the bottom
 */
@Suppress("MagicNumber", "Unused")
fun Modifier.verticalFadingEdges(
    scrollState: ScrollState,
    topEdgeSize: Dp = 72.dp,
    bottomEdgeSize: Dp = 72.dp,
): Modifier = this
    // adding layer fixes issue with blending gradient and content
    .graphicsLayer { alpha = 0.999F }
    .drawWithContent {
        drawContent()

        val scrollAreaHeight = size.height - scrollState.maxValue
        val scrollAreaTopY = scrollState.value.toFloat()
        val scrollAreaBottomY = scrollAreaTopY + scrollAreaHeight

        // gradient size is equivalent to how much content is outside of the area in each side limited by edgeSize
        val topGradientHeight = min(topEdgeSize.toPx(), scrollState.value.toFloat())
        val bottomGradientHeight = min(bottomEdgeSize.toPx(), scrollState.maxValue.toFloat() - scrollState.value)

        // wherever the rectangle is drawn (green), the content will be transparent, creating the fading effect
        val topColors = listOf(Color.Green, Color.Transparent)
        val bottomColors = listOf(Color.Transparent, Color.Green)

        if (topGradientHeight != 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = topColors,
                    startY = scrollAreaTopY,
                    endY = scrollAreaTopY + topGradientHeight
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (bottomGradientHeight != 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = bottomColors,
                    startY = scrollAreaBottomY - bottomGradientHeight,
                    endY = scrollAreaBottomY
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

/**
 * Adds horizontal fading edges to a scrollable container, usually a [Row].
 * It needs to be used right after the scrollable modifier [horizontalScroll].
 *
 * The [edgeSize] defines the maximum size of the fading edge effect, which is used when that same amount of the
 * content is outside the scrollable area.
 *
 * @param scrollState the scroll state of the scrollable container (same used in [Modifier.horizontalScroll])
 * @param startEdgeSize the size of the fading edge on the start
 * @param endEdgeSize the size of the fading edge on the end
 */
@Suppress("MagicNumber", "Unused")
fun Modifier.horizontalFadingEdges(
    scrollState: ScrollState,
    startEdgeSize: Dp = 24.dp,
    endEdgeSize: Dp = 24.dp,
): Modifier = this
    // adding layer fixes issue with blending gradient and content
    .graphicsLayer { alpha = 0.99F }
    .composed {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        drawWithContent {
            drawContent()

            val scrollAreaWidth = size.width - scrollState.maxValue
            val scrollAreaLeftX: Float
            val scrollAreaRightX: Float
            val leftGradientWidth: Float
            val rightGradientWidth: Float

            if (!isRtl) {
                scrollAreaLeftX = scrollState.value.toFloat()
                scrollAreaRightX = scrollAreaLeftX + scrollAreaWidth
                leftGradientWidth = min(startEdgeSize.toPx(), scrollState.value.toFloat())
                rightGradientWidth = min(endEdgeSize.toPx(), scrollState.maxValue.toFloat() - scrollState.value)
            } else {
                scrollAreaRightX = size.width - scrollState.value.toFloat()
                scrollAreaLeftX = scrollAreaRightX - scrollAreaWidth
                leftGradientWidth = min(endEdgeSize.toPx(), scrollState.maxValue.toFloat() - scrollState.value)
                rightGradientWidth = min(startEdgeSize.toPx(), scrollState.value.toFloat())
            }

            val leftColors = listOf(Color.Green, Color.Transparent)
            val rightColors = listOf(Color.Transparent, Color.Green)

            if (leftGradientWidth != 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = leftColors,
                        startX = scrollAreaLeftX,
                        endX = scrollAreaLeftX + leftGradientWidth
                    ),
                    blendMode = BlendMode.DstOut,
                )
            }

            if (rightGradientWidth != 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = rightColors,
                        startX = scrollAreaRightX - rightGradientWidth,
                        endX = scrollAreaRightX
                    ),
                    blendMode = BlendMode.DstOut,
                )
            }
        }
    }
