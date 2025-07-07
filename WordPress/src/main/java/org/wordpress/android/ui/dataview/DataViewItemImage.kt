package org.wordpress.android.ui.dataview

import androidx.annotation.DrawableRes

/**
 * Represents a remote image with a fallback image to be displayed in a [DataViewItemCard]
 */
open class DataViewItemImage(
    val imageUrl: String?,
    @DrawableRes val fallbackImageRes: Int,
)
