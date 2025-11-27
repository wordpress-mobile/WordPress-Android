package org.wordpress.android.ui.reader.views.uistates

data class FollowButtonUiState(
    val onFollowButtonClicked: (() -> Unit)?,
    val isFollowed: Boolean,
    val isVisible: Boolean = true,
    val isFollowActionRunning: Boolean = false
)
