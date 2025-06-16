package org.wordpress.android.ui.dataview

/**
 * Represents a basic model for data to be displayed in a [DataViewItemCard]. The optional image
 * will appear to the left of the fields.
 */
data class DataViewItem(
    val id: Long,
    val image: DataViewItemImage?,
    val fields: List<DataViewItemField>,
)
