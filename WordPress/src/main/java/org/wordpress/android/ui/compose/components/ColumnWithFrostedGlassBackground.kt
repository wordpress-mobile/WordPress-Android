package org.wordpress.android.ui.compose.components

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline.Rectangle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import org.wordpress.android.ui.compose.components.SlotsEnum.Buttons
import org.wordpress.android.ui.compose.components.SlotsEnum.ClippedBackground

/**
 * These slots are utilized below in a subcompose layout in order to measure the size of the buttons composable. The
 * measured height is then used to create a clip shape for the blurred background layer. This allows the background
 * composable to be aware of its sibling's dimensions within a single frame (i.e. it does not trigger a recomposition).
 */
private enum class SlotsEnum { Buttons, ClippedBackground }

@Composable
private fun ColumnWithTopGlassBorder(
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
            modifier = Modifier.background(backgroundColor)
    ) {
        Divider(color = borderColor)
        content()
    }
}

@Composable
fun ColumnWithFrostedGlassBackground(
    blurRadius: Dp,
    backgroundColor: Color,
    borderColor: Color,
    background: @Composable (clipModifier: Modifier, blurModifier: Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val buttonsPlaceables = subcompose(Buttons) {
            ColumnWithTopGlassBorder(
                    backgroundColor = backgroundColor,
                    borderColor = borderColor,
            ) {
                content()
            }
        }.map { it.measure(constraints) }

        val buttonsHeight = buttonsPlaceables[0].height
        val buttonsClipShape = object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Rectangle {
                return Rectangle(
                        Rect(
                                bottom = size.height,
                                left = 0f,
                                right = size.width,
                                top = size.height - buttonsHeight,
                        )
                )
            }
        }

        val clippedBackgroundPlaceables = subcompose(ClippedBackground) {
            background(
                    clipModifier = Modifier.clip(buttonsClipShape),
                    blurModifier = Modifier.composed {
                        if (VERSION.SDK_INT >= VERSION_CODES.S) {
                            blur(blurRadius, BlurredEdgeTreatment.Unbounded)
                        } else {
                            // On versions older than Android 12 the blur modifier is not supported,
                            // so we make the text transparent to have the buttons stand out.
                            alpha(0.05f)
                        }
                    }
            )
        }.map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            clippedBackgroundPlaceables.forEach { it.placeRelative(0, 0) }
            buttonsPlaceables.forEach { it.placeRelative(0, constraints.maxHeight - buttonsHeight) }
        }
    }
}
