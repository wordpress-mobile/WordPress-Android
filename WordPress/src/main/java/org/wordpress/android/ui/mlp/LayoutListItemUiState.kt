package org.wordpress.android.ui.mlp

/**
 * The layout list item
 */
data class LayoutListItemUiState(
    val slug: String,
    val title: String,
    val preview: String,
    val selected: Boolean,
    val onItemTapped: (() -> Unit)
)
