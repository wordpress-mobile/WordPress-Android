package org.wordpress.android.ui.sitecreation.theme

import org.wordpress.android.R

private const val viewPortWidth = 200

/**
 * The layout grid item
 */
data class LayoutGridItemUiState(
    val slug: String,
    val title: String,
    val preview: String,
    val selected: Boolean,
    val onItemTapped: () -> Unit,
    val onThumbnailReady: () -> Unit
) {
    val contentDescriptionResId: Int
        get() = if (selected) R.string.mlp_selected_description else R.string.mlp_notselected_description

    val selectedOverlayVisible: Boolean
        get() = selected

    val tweakedPreview: String
        get() {
            val ratio = 1200.0 / 1600.0
            val viewPortHeight = (viewPortWidth / ratio).toInt()
            return preview.replace("vpw=1200&vph=1600", "vpw=$viewPortWidth&vph=$viewPortHeight")
        }
}
