package org.wordpress.android.ui.dataview

/**
 * Represents a basic model for data to be displayed in a [DataViewItemCard]. The optional image
 * will appear to the left of the fields. Note that [data] is optional but is intended to store
 * the actual data object associated with the item..
 */
data class DataViewItem(
    val id: Long,
    val image: DataViewItemImage?,
    val title: String,
    val fields: List<DataViewItemField>,
    val data: Any? = null
)
