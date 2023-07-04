package org.wordpress.android.ui.layoutpicker

import org.wordpress.android.R
import com.google.android.material.R as MaterialR

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
        get() = if (selected) R.attr.categoriesButtonBackgroundSelected else R.attr.categoriesButtonBackground

    val textColor: Int
        get() = if (selected) MaterialR.attr.colorOnPrimary else MaterialR.attr.colorOnSurface

    val checkIconVisible: Boolean
        get() = selected

    val emojiIconVisible: Boolean
        get() = !selected

    val contentDescriptionResId: Int
        get() = if (selected) R.string.mlp_selected_description else R.string.mlp_notselected_description
}
