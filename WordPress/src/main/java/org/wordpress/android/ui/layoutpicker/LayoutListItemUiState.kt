package org.wordpress.android.ui.layoutpicker

import org.wordpress.android.R

/**
 * The layout list item
 */
data class LayoutListItemUiState(
    val slug: String,
    val title: String,
    val preview: String,
    val selected: Boolean,
    val onItemTapped: (() -> Unit),
    val onThumbnailReady: (() -> Unit)
) {
    val contentDescriptionResId: Int
        get() = if (selected) R.string.mlp_selected_description else R.string.mlp_notselected_description

    val selectedOverlayVisible: Boolean
        get() = selected

    val previewTweaked: String
        get() {
            val ratio: Double = 160.0 / 250.0
            val width = 400.0
            val height = width / ratio
            return preview.substring(0, preview.lastIndexOf('?')) + "?vpw=$width&vph=$height&w=$width&h=$height"
        }
}
