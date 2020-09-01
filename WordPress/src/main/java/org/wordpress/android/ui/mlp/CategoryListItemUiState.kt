package org.wordpress.android.ui.mlp

/**
 * The category list item
 */
data class CategoryListItemUiState(
    val slug: String,
    val title: String,
    val emoji: String,
    val selected: Boolean,
    val onItemTapped: (() -> Unit)
)
