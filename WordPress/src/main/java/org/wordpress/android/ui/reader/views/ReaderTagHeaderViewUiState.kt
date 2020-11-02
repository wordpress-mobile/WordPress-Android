package org.wordpress.android.ui.reader.views

import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState

sealed class ReaderTagHeaderViewUiState {
    data class ReaderTagHeaderUiState(
        val title: String,
        val followButtonUiState: FollowButtonUiState
    ) : ReaderTagHeaderViewUiState()
}
