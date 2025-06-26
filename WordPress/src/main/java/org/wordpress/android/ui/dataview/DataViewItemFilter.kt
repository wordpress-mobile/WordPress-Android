package org.wordpress.android.ui.dataview

import androidx.annotation.StringRes

/**
 * Represents a single filter for displaying in a dropdown menu in [DataViewItemCard]
 */
data class DataViewItemFilter(
    val id: Long,
    @StringRes val titleRes: Int,
)

