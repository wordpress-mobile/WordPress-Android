package org.wordpress.android.ui.accounts.login.components

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
import androidx.compose.ui.graphics.Outline.Rectangle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.accounts.login.components.SlotsEnum.Buttons
import org.wordpress.android.ui.accounts.login.components.SlotsEnum.ClippedBackground

/**
 * These slots are utilized below in a subcompose layout in order to measure the size of the buttons composable. The
 * measured height is then used to create a clip shape for the blurred background layer. This allows the background
 * composable to be aware of its sibling's dimensions within a single frame (i.e. it does not trigger a recomposition).
 */
private enum class SlotsEnum { Buttons, ClippedBackground }

@Composable
private fun ColumnWithTopGlassBorder(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
            modifier = modifier
                    .background(
                            brush = SolidColor(colorResource(R.color.bg_jetpack_login_splash_bottom_panel)),
                            alpha = 0.6f
                    )
    ) {
        Divider(
                thickness = 1.dp,
                color = colorResource(R.color.border_shadow_jetpack_login_splash_bottom_panel),
        )
        Divider(
                thickness = 1.dp,
                color = colorResource(R.color.border_highlight_jetpack_login_splash_bottom_panel),
        )
        content()
    }
}

@Composable
fun ColumnWithFrostedGlassBackground(
    content: @Composable () -> Unit,
) {
    val topBorderHeight = with(LocalDensity.current) { 1.dp.toPx() }

    SubcomposeLayout { constraints ->
        val buttonsPlaceables = subcompose(Buttons) {
            ColumnWithTopGlassBorder {
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
                                top = size.height - buttonsHeight + topBorderHeight,
                        )
                )
            }
        }

        val clippedBackgroundPlaceables = subcompose(ClippedBackground) {
            AutoScrollingTextBox(
                    modifier = Modifier.clip(buttonsClipShape),
                    textModifier = Modifier.composed {
                        if (VERSION.SDK_INT >= VERSION_CODES.S) {
                            blur(15.dp, BlurredEdgeTreatment.Unbounded)
                        } else {
                            // On versions older than Android 12 the blur modifier is not supported,
                            // so we make the text transparent to have the buttons stand out.
                            alpha(0.25f)
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
