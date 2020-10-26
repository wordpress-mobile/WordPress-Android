package org.wordpress.android.ui.sitecreation.theme

import androidx.annotation.DimenRes
import org.wordpress.android.R.dimen
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import kotlin.math.min

private const val ratio = 0.75

class ThumbDimensionProvider @Inject constructor(
    private val contextProvider: ContextProvider,
    private val displayUtilsWrapper: DisplayUtilsWrapper
) {
    private val containerWidth: Int
        get() = min(getPixels(dimen.hpp_maximum_container_width), displayUtilsWrapper.getDisplayPixelWidth())

    private val isTabletOrLandscape: Boolean
        get() = displayUtilsWrapper.isTablet() || displayUtilsWrapper.isLandscape()

    private val largeScreenColumns: Int
        get() = (containerWidth - (2 * getPixels(dimen.hpp_card_margin_outer)) - getPixels(dimen.hpp_card_padding)) /
                (getPixels(dimen.hpp_card_width) + getPixels(dimen.hpp_card_padding))

    private val phoneColumns: Int
        get() = 2

    private val largeScreenWidth: Int
        get() = getPixels(dimen.hpp_card_width)

    private val phoneWidth: Int
        get() = (containerWidth - ((phoneColumns + 1) * getPixels(dimen.hpp_card_padding))) / phoneColumns

    val columns: Int
        get() = if (isTabletOrLandscape) largeScreenColumns else phoneColumns

    val width: Int
        get() = if (isTabletOrLandscape) largeScreenWidth else phoneWidth

    val height: Int
        get() = (width / ratio).toInt()

    val scale: Double = 1.0

    private fun getPixels(@DimenRes id: Int) = contextProvider.getContext().resources.getDimensionPixelSize(id)
}
