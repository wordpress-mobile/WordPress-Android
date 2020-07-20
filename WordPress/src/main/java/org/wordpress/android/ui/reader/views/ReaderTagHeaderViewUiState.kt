package org.wordpress.android.ui.reader.views

sealed class ReaderTagHeaderViewUiState {
    data class ReaderTagHeaderUiState(
        val title: String,
        val followButtonUiState: FollowButtonUiState
    ) : ReaderTagHeaderViewUiState() {
        data class FollowButtonUiState(
            val onFollowButtonClicked: (() -> Unit)?,
            val isVisible: Boolean,
            val isFollowed: Boolean,
            val isEnabled: Boolean,
            val shouldAnimate: Boolean
        )
    }
}
