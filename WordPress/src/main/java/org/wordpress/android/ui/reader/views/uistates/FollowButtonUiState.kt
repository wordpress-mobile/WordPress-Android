package org.wordpress.android.ui.reader.views.uistates

data class FollowButtonUiState(
    val onFollowButtonClicked: (() -> Unit)?,
    val isFollowed: Boolean,
    val isEnabled: Boolean,
    val isVisible: Boolean = true
)
