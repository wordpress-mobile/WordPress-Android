package org.wordpress.android.ui.layoutpicker

/**
 * The layout category row list item
 * @param slug the layout category slug
 * @param title the layout category title
 * @param description the layout category description
 * @param layouts the layouts list
 * @param isRecommended defines if the category is recommended (optional with default `false`)
 */
data class LayoutCategoryUiState(
    val slug: String,
    val title: String,
    val description: String,
    val layouts: List<LayoutListItemUiState>,
    val isRecommended: Boolean = false
)
