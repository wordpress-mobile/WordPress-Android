package org.wordpress.android.ui.reader.views.compose.comments

import androidx.compose.ui.text.input.TextFieldValue
import org.wordpress.android.ui.reader.ReaderCommentModeration

data class ReaderCommentListActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
    val onScrollHandled: () -> Unit,
    val onHeaderClick: () -> Unit,
    val onAuthorClick: (Long) -> Unit,
    val onReplyClick: (Long) -> Unit,
    val onLikeClick: (Long) -> Unit,
    val onModerateClick: (Long, ReaderCommentModeration) -> Unit,
    val onEditClick: (Long) -> Unit,
    val onShareClick: (Long) -> Unit,
    val onImageClick: (String) -> Unit,
    val onComposerClick: () -> Unit,
    val onReplyTextChange: (TextFieldValue) -> Unit,
    val onSendReply: () -> Unit,
    val onDeleteDraft: () -> Unit,
    val onReplySheetDismissed: () -> Unit
)
