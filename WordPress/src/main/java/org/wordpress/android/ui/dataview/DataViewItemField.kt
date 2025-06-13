package org.wordpress.android.ui.dataview

/**
 * Represents a single field in a [DataViewItem]
 */
data class DataViewItemField(
    val value: String,
    val subValue: String? = null,
    val fieldType: DataViewFieldType
)
