package org.wordpress.android.ui.reader

import org.wordpress.android.ui.reader.views.ReaderFollowButton

sealed class FollowCommentsUiState {
    data class UpdateFollowCommentsUiState(
        val type: FollowCommentsUiStateType,
        val showFollowButton: Boolean = false,
        val isFollowing: Boolean = false,
        val animate: Boolean = false,
        val onFollowButtonClick: ((Boolean) -> Unit)? = null
    ) : FollowCommentsUiState()

    object Nop : FollowCommentsUiState()
}

enum class FollowCommentsUiStateType {
    DISABLED,
    LOADING,
    GONE,
    VISIBLE_WITH_STATE
}
