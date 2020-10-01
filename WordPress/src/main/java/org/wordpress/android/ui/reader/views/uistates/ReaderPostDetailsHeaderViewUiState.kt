package org.wordpress.android.ui.reader.views.uistates

import org.wordpress.android.ui.utils.UiString

sealed class ReaderPostDetailsHeaderViewUiState {
    data class ReaderPostDetailsHeaderUiState(
        val title: UiString?,
        val authorName: String?,
        val blogSectionUiState: ReaderBlogSectionUiState,
        val followButtonUiState: FollowButtonUiState
    ) : ReaderPostDetailsHeaderViewUiState()
}
