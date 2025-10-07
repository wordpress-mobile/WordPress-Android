package org.wordpress.android.ui.dataview

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents a basic model for data to be displayed in a [DataViewItemCard]. Note that [data] is
 * optional but is intended to store the actual data object associated with the item..
 */
data class DataViewItem(
    val id: Long,
    val image: DataViewItemImage?,
    val title: String,
    val fields: List<DataViewItemField>,
    // Avoid adding the last field to the end of the card and follow regular alignment instead
    val skipEndPositioning: Boolean = false,
    val data: Any? = null,
    val indentation: Dp = 0.dp // Used to indent items
)
