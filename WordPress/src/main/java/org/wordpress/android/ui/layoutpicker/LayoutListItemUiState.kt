package org.wordpress.android.ui.layoutpicker

import org.wordpress.android.R

/**
 * The layout list item
 */
data class LayoutListItemUiState(
    val slug: String,
    val title: String,
    val preview: String,
    val mShotPreview: String,
    val selected: Boolean,
    val onItemTapped: (() -> Unit),
    val onThumbnailReady: (() -> Unit),
    private val tapOpensPreview: Boolean
) {
    val contentDescriptionResId: Int
        get() = when {
            tapOpensPreview -> R.string.hpp_preview_tapped_theme
            selected -> R.string.mlp_selected_description
            else -> R.string.mlp_notselected_description
        }
    val selectedOverlayVisible: Boolean
        get() = !tapOpensPreview && selected
}
