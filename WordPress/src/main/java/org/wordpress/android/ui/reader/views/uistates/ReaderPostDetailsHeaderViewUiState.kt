package org.wordpress.android.ui.reader.views.uistates

sealed class ReaderPostDetailsHeaderViewUiState {
    data class ReaderPostDetailsHeaderUiState(
        val blogSectionUiState: ReaderBlogSectionUiState,
        val followButtonUiState: FollowButtonUiState
    ) : ReaderPostDetailsHeaderViewUiState()
}
