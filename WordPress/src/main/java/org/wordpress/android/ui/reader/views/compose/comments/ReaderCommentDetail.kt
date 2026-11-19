package org.wordpress.android.ui.reader.views.compose.comments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.comments.unified.compose.CommentParentStrip
import org.wordpress.android.ui.reader.ReaderCommentDetailUiState
import org.wordpress.android.ui.reader.ReaderCommentListUiState

/**
 * A single comment opened from the list: what it replies to, the comment itself, then its direct replies.
 * Tapping the parent or a reply opens that comment in turn.
 */
@Composable
internal fun ReaderCommentDetail(
    uiState: ReaderCommentListUiState,
    detail: ReaderCommentDetailUiState,
    actions: ReaderCommentListActions
) {
    val listState = rememberLazyListState()
    // handles the scroll a reply asks for here, so it isn't left for the list to act on when this closes
    LaunchedEffect(uiState.scrollToCommentId) {
        val commentId = uiState.scrollToCommentId ?: return@LaunchedEffect
        // the post header, then the parent strip if there is one, then the comment and its replies
        val offset = if (detail.parent != null) 2 else 1
        val index = (listOf(detail.comment) + detail.replies).indexOfFirst { it.commentId == commentId }
        if (index >= 0) listState.animateScrollToItem(index + offset)
        actions.onScrollHandled()
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "header") { PostHeader(uiState, actions.onHeaderClick) }
        detail.parent?.let { parent ->
            item(key = "parent") {
                HorizontalDivider(modifier = Modifier.padding(start = 24.dp))
                CommentParentStrip(
                    parentAuthorName = parent.authorName,
                    parentSnippet = parent.snippet,
                    modifier = Modifier
                        .clickable { actions.onCommentClick(parent.commentId) }
                        .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                )
            }
        }
        item(key = detail.comment.commentId) { CommentRow(detail.comment, actions, onClick = null) }
        items(detail.replies, key = { it.commentId }) { reply ->
            CommentRow(reply, actions, onClick = { actions.onCommentClick(reply.commentId) })
        }
    }
}
