package org.wordpress.android.ui.dataview

data class DataViewItem(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val dateLine: String? = null,
    val imageUrl: String? = null,
)
