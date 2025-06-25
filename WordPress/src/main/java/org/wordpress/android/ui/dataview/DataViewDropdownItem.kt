package org.wordpress.android.ui.dataview

import androidx.annotation.StringRes

/**
 * Represents a single menu item for displaying in a dropdown menu. Currently
 * used in [DataViewScreen] for both filter and sort.
 */
data class DataViewDropdownItem(
    val id: Long,
    @StringRes val titleRes: Int,
)

