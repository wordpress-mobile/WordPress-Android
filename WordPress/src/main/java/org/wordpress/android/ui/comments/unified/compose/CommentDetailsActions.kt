package org.wordpress.android.ui.comments.unified.compose

/** The comment detail screen's callbacks, implemented by the hosting fragment's ViewModel. */
@Suppress("LongParameterList")
class CommentDetailsActions(
    val onModerateClick: () -> Unit,
    val onSpamClick: () -> Unit,
    val onLikeClick: () -> Unit,
    val onEditClick: () -> Unit,
    val onTrashClick: () -> Unit,
    val onDeletePermanentlyClick: () -> Unit,
    val onCopyLinkClick: () -> Unit,
    val onShareLinkClick: () -> Unit,
    val onPostTitleClick: () -> Unit,
    val onSendReply: (String) -> Unit
)
