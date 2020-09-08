package org.wordpress.android.ui.mlp

import org.wordpress.android.R.attr

/**
 * The category list item
 */
data class CategoryListItemUiState(
    val slug: String,
    val title: String,
    val emoji: String,
    val selected: Boolean,
    val onItemTapped: (() -> Unit)
) {
    val background: Int
        get() = if (selected) attr.categoriesButtonBackgroundSelected else attr.categoriesButtonBackground

    val textColor: Int
        get() = if (selected) attr.categoriesButtonTextSelected else attr.categoriesButtonText
}
