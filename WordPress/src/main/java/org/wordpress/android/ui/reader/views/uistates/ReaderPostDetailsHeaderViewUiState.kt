package org.wordpress.android.ui.reader.views.uistates

import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.utils.UiString

sealed class ReaderPostDetailsHeaderViewUiState {
    data class ReaderPostDetailsHeaderUiState(
        val title: UiString?,
        val authorName: String?,
        val tagItems: List<TagUiState>,
        val tagItemsVisibility: Boolean,
        val blogSectionUiState: ReaderBlogSectionUiState,
        val followButtonUiState: FollowButtonUiState,
        val dateLine: String
    ) : ReaderPostDetailsHeaderViewUiState() {
        val dotSeparatorVisibility: Boolean = authorName != null
    }
}
