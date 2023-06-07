package org.wordpress.android.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme

private const val DEFAULT_ICON_SIZE = 32
private const val DEFAULT_ICON_BORDER_WIDTH = 2
// this proportion is calculated based on the Figma design for Jetpack Social (Th1ahHKq53k5JT1PNDMavY-fi-865_13166)
private const val ICON_OFFSET_PROPORTION = 29f / 36f

/**
 * This component uses coil's [AsyncImage] internally so it supports any model the regular [AsyncImage] composable can
 * use. The icons are laid out horizontally, with the first icon being the first element in the list and the last icon
 * being the last element in the list. The icons are laid out in a way that they overlap each other slightly.
 *
 * The space an individual icon occupies is calculated by the sum of the [iconSize] and [iconBorderWidth] parameters.
 *
 * @param iconModels a list of models to be used for the icons. The list must have at least 1 element.
 * @param modifier the modifier to be applied to the layout.
 * @param iconSize the size of an individual icon.
 * @param placeholder the placeholder to be used while the icons are loading.
 */
@Composable
fun TrainOfIcons(
    iconModels: List<Any>,
    modifier: Modifier = Modifier,
    iconSize: Dp = DEFAULT_ICON_SIZE.dp,
    iconBorderWidth: Dp = DEFAULT_ICON_BORDER_WIDTH.dp,
    placeholder: Painter = ColorPainter(colorResource(R.color.placeholder)),
) {
    require(iconModels.isNotEmpty()) { "TrainOfIcons must have at least 1 icon" }

    val iconSizeWithBorder = (iconSize.value + 2 * iconBorderWidth.value).toInt()

    Layout(
        modifier = modifier,
        content = {
            iconModels.forEach { iconModel ->
                AsyncImage(
                    model = iconModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = placeholder,
                    modifier = Modifier
                        .size(iconSizeWithBorder.dp)
                        .border(
                            width = iconBorderWidth,
                            color = AppColor.White,
                            shape = CircleShape
                        )
                        .padding(iconBorderWidth)
                        .clip(CircleShape)
                        .background(AppColor.White)
                )
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val measuredIconSize = placeables[0].width
        val iconOffset = (measuredIconSize * ICON_OFFSET_PROPORTION).toInt()
        val totalWidth = iconOffset * (placeables.size - 1) + measuredIconSize

        val width = totalWidth.coerceIn(constraints.minWidth, constraints.maxWidth)

        layout(width, measuredIconSize) {
            placeables.forEachIndexed { index, placeable ->
                val offsetX = index * iconOffset
                placeable.placeRelative(offsetX, 0, 0f)
            }
        }
    }
}

@Preview
@Composable
fun TrainOfIconsPreview() {
    AppTheme {
        TrainOfIcons(
            iconModels = List(4) {
                R.drawable.login_prologue_fifth_asset_one
            }
        )
    }
}
