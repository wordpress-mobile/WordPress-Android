package org.wordpress.android.ui.compose.utils

import androidx.compose.material.LocalElevationOverlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.google.android.material.R as MaterialR


private val materialBottomSheetElevation: Dp
    @Composable
    @ReadOnlyComposable
    get() = dimensionResource(MaterialR.dimen.design_bottom_sheet_elevation)

/**
 * This function applies an Elevation Overlay to the current color, which should be usually the "surface" color, because
 * in the Dark Theme, the concept of elevation is translated into an overlay on top of the color, making it lighter. The
 * higher the elevation, the lighter the color. See more here:
 * https://m2.material.io/design/color/dark-theme.html#properties
 *
 * @param elevation The elevation to be applied to the color.
 * @return The color with the elevation applied.
 *
 * @see LocalElevationOverlay
 */
@Composable
fun Color.withElevation(elevation: Dp): Color = LocalElevationOverlay.current
    ?.apply(this, elevation)
    ?: this

/**
 * This function is the same as [withElevation], but it uses the default elevation for Material Bottom Sheets, which is
 * a pretty common component used throughout our application.
 */
@Composable
fun Color.withBottomSheetElevation(): Color = withElevation(materialBottomSheetElevation)
