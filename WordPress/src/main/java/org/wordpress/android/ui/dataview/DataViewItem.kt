package org.wordpress.android.ui.dataview

/**
 * Represents a basic model for data to be displayed in a [DataViewItemCard].
 */
open class DataViewItem(
    val id: Long,
    val image: DataViewItemImage?,
    val fields: List<DataViewItemField>,
)
