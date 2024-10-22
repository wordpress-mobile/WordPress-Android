package org.wordpress.android.ui.compose.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2

private const val DEFAULT_ICON_SIZE = 32
private const val DEFAULT_ICON_BORDER_WIDTH = 2

// this proportion is calculated based on the Figma design for Jetpack Social (Th1ahHKq53k5JT1PNDMavY-fi-865_13166)
private const val ICON_OFFSET_PROPORTION = 29f / 36f


data class TrainOfIconsModel(
    val data: Any?,
    val alpha: Float = 1f,
)

/**
 * This component uses coil's [AsyncImage] internally so it supports any model the regular [AsyncImage] composable can
 * use. The icons are laid out horizontally, with the first icon being the first element in the list and the last icon
 * being the last element in the list. The icons are laid out in a way that they overlap each other slightly.
 *
 * The space an individual icon occupies is calculated by the sum of the [iconSize] and [iconBorderWidth] parameters.
 *
 * @param iconModels a list of models to be used for the icons. The list must have at least 1 element.
 * @param modifier the modifier to be applied to the layout.
 * @param contentDescription the content description of the container.
 * @param iconSize the size of an individual icon.
 * @param iconBorderWidth the width of the icon border.
 * @param placeholderPainter the placeholder [Painter] to be used while the icons are loading.
 */
@Composable
fun TrainOfIcons(
    iconModels: List<TrainOfIconsModel>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    iconSize: Dp = DEFAULT_ICON_SIZE.dp,
    iconBorderWidth: Dp = DEFAULT_ICON_BORDER_WIDTH.dp,
    iconBorderColor: Color = MaterialTheme.colors.surface,
    placeholderPainter: Painter = ColorPainter(colorResource(R.color.placeholder)),
) {
    if (iconModels.isEmpty()) {
        return
    }

    val iconSizeWithBorder = (iconSize.value + 2 * iconBorderWidth.value).toInt()
    val semanticsModifier = contentDescription?.let {
        modifier.semantics(mergeDescendants = true) { this.contentDescription = it }
    }

    Layout(
        modifier = semanticsModifier ?: modifier,
        content = {
            iconModels.forEach { iconModel ->
                AsyncImage(
                    model = iconModel.data,
                    alpha = iconModel.alpha,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = placeholderPainter,
                    fallback = placeholderPainter,
                    error = placeholderPainter,
                    modifier = Modifier
                        .size(iconSizeWithBorder.dp)
                        .clip(CircleShape)
                        .background(iconBorderColor)
                        .padding(iconBorderWidth)
                        .clip(CircleShape)
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
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TrainOfIconsPreview() {
    AppThemeM2 {
        TrainOfIcons(
            iconModels = listOf(
                R.drawable.login_prologue_second_asset_three,
                R.drawable.login_prologue_second_asset_two,
                R.drawable.login_prologue_third_asset_one,
                R.mipmap.app_icon
            ).map { TrainOfIconsModel(it) },
            contentDescription = "Train of icons",
        )
    }
}
