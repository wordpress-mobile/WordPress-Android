package org.wordpress.android.ui.dataview

import androidx.annotation.StringRes

/**
 * Represents a single filter for displaying in a [DataViewItemCard].
 */
data class DataViewItemFilter(
    val id: Long,
    @StringRes val titleRes: Int,
)

