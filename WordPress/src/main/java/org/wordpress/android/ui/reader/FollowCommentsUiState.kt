package org.wordpress.android.ui.reader

data class FollowCommentsUiState(
    val type: FollowCommentsUiStateType,
    val showFollowButton: Boolean = false,
    val isFollowing: Boolean = false,
    val animate: Boolean = false,
    val onFollowButtonClick: ((Boolean) -> Unit)? = null
)

enum class FollowCommentsUiStateType {
    DISABLED,
    LOADING,
    GONE,
    VISIBLE_WITH_STATE
}
