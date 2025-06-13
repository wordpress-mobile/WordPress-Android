package org.wordpress.android.ui.dataview

import androidx.annotation.StringRes

/**
 * Represents a single field in a [DataViewItem]
 */
data class DataViewItemField(
    @StringRes val titleResId: Int,
    val value: String,
    val subValue: String? = null,
    val fieldType: DataViewFieldType
)
