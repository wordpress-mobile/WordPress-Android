package org.wordpress.android.ui.reader.views.uistates

class InteractionSectionUiState(
    val likeCount: Int,
    val commentCount: Int,
    val onLikesClicked: () -> Unit,
    val onCommentsClicked: () -> Unit,
)
