package org.wordpress.android.ui.dataview

/**
 * Represents a basic item for displaying in a [DataViewItemCard].
 */
data class DataViewItem(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val dateLine: String? = null,
    val imageUrl: String? = null,
)
