package org.wordpress.android.ui.dataview

/**
 * Represents a single field in a [DataViewItem]
 */
data class DataViewItemField(
    val value: String,
    val valueType: DataViewFieldType,
    val subValue: String? = null,
    val subValueType: DataViewFieldType = DataViewFieldType.SUBTITLE,
    val weight: Float = 0f,
)
