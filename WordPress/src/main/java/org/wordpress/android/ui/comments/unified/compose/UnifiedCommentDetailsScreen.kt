package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel.CommentDetailsUiState
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.suggestion.Suggestion

/**
 * The unified (wordpress-rs) comment detail screen: comment content in a weighted scrollable
 * region so the moderation toolbar stays pinned to the bottom while loading, plus the
 * trash/delete confirmation dialogs and the reply sheet.
 */
@Composable
@Suppress("LongParameterList")
fun UnifiedCommentDetailsScreen(
    uiState: CommentDetailsUiState,
    replyText: TextFieldValue,
    onReplyTextChange: (TextFieldValue) -> Unit,
    suggestions: List<Suggestion>,
    showLikeButton: Boolean,
    focusReplyFieldOnLaunch: Boolean,
    snackbarHostState: SnackbarHostState,
    actions: CommentDetailsActions,
    modifier: Modifier = Modifier
) {
    var showTrashConfirm by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showReplyEditor by rememberSaveable { mutableStateOf(false) }

    // Opened from a notification's reply action. There is no pinned field to focus, so
    // "start replying" means opening the reply sheet. The host recomputes
    // focusReplyFieldOnLaunch as false after a config change, so this does not fire again on
    // rotation or re-open a screen the user dismissed.
    LaunchedEffect(Unit) {
        if (focusReplyFieldOnLaunch) showReplyEditor = true
    }

    val replyHint = if (uiState.authorName.isNotBlank()) {
        stringResource(R.string.comment_reply_to_user, uiState.authorName)
    } else {
        stringResource(R.string.reader_hint_comment_on_post)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // The weighted box keeps its space while the comment loads, pinning the moderation
                // toolbar to the bottom (the XML layout used INVISIBLE for this).
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.contentVisible) {
                        CommentDetailsContent(
                            uiState = uiState,
                            showLikeButton = showLikeButton,
                            hasReplyDraft = replyText.text.isNotBlank(),
                            actions = actions,
                            onReplyClick = { showReplyEditor = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                CommentModerationToolbar(
                    status = uiState.status,
                    canModerate = uiState.canModerate,
                    onApproveClick = actions.onModerateClick,
                    onSpamClick = actions.onSpamClick,
                    // Trashing is committed server-side immediately, so it normally confirms first -
                    // but a comment with no replies loses nothing recoverable, so that case goes
                    // straight through, as on iOS. An unknown count still confirms.
                    onTrashClick = if (uiState.replyCount == 0) {
                        actions.onTrashClick
                    } else {
                        { showTrashConfirm = true }
                    },
                    // Restore is the inverse of however the comment got here: un-spam for a spam
                    // comment, untrash for a trashed one. Both ViewModel actions toggle back to
                    // approved, but only from their own status.
                    onRestoreClick = actions.onRestoreClick,
                    onDeletePermanentlyClick = { showDeleteConfirm = true },
                    pendingAction = uiState.pendingAction
                )
            }
            if (uiState.showProgress) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showTrashConfirm) {
        ConfirmDialog(
            // A comment with replies warns that they are left behind; an unknown count falls back
            // to the generic wording rather than claiming something it cannot know.
            messageRes = if ((uiState.replyCount ?: 0) > 0) {
                R.string.comment_trash_has_replies
            } else {
                R.string.dlg_confirm_trash_comments
            },
            confirmRes = R.string.dlg_confirm_action_trash,
            onConfirm = {
                showTrashConfirm = false
                actions.onTrashClick()
            },
            onDismiss = { showTrashConfirm = false }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            messageRes = R.string.dlg_sure_to_delete_comment,
            confirmRes = R.string.delete,
            onConfirm = {
                showDeleteConfirm = false
                actions.onDeletePermanentlyClick()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // A bottom sheet keeps the comment being answered visible behind it.
    if (showReplyEditor) {
        CommentReplySheet(
            replyText = replyText,
            onReplyTextChange = onReplyTextChange,
            suggestions = suggestions,
            hint = replyHint,
            isReplyInProgress = uiState.isReplyInProgress,
            // A notification's reply action opens the sheet before the comment loads, and a reply
            // sent then would be dropped as the sheet closes.
            isSendEnabled = uiState.contentVisible,
            onSendClick = {
                showReplyEditor = false
                actions.onSendReply(replyText.text)
            },
            // Clearing the field is what deletes the draft: the host's onPause save sees blank
            // text and removes the stored entry.
            onDeleteDraft = { onReplyTextChange(TextFieldValue("")) },
            onDismiss = { showReplyEditor = false }
        )
    }
}

/**
 * The comment content region, following iOS's `CommentDetailView`: status pill, author header
 * and optional "in reply to" strip above the comment body. The moderation toolbar is pinned by the
 * caller.
 *
 * Unlike iOS the header scrolls with the body rather than staying pinned. With large text on a
 * short screen a pinned header can take all the height the toolbar leaves, measuring the body to
 * 0dp and making the comment unreachable - a pinned header is not worth losing the content to.
 */
@Composable
private fun CommentDetailsContent(
    uiState: CommentDetailsUiState,
    showLikeButton: Boolean,
    hasReplyDraft: Boolean,
    actions: CommentDetailsActions,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAuthorInfo by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(
                start = CONTENT_H_PADDING,
                end = CONTENT_H_PADDING,
                top = CONTENT_V_PADDING,
                bottom = CONTENT_V_PADDING
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommentStatusPill(status = uiState.status, customLabel = uiState.customStatusLabel)
                Spacer(modifier = Modifier.weight(1f))
                CommentDetailOverflowMenu(
                    status = uiState.status,
                    showCommentUrlActions = uiState.commentUrl.isNotEmpty(),
                    canModerate = uiState.canModerate,
                    onUnapproveClick = actions.onModerateClick,
                    onEditClick = actions.onEditClick,
                    onCopyLinkClick = actions.onCopyLinkClick,
                    onShareLinkClick = actions.onShareLinkClick
                )
            }
            CommentAuthorHeader(
                authorName = uiState.authorName,
                authorAvatarUrl = uiState.authorAvatarUrl,
                postTitle = uiState.postTitle,
                datePublished = uiState.datePublished,
                onPostTitleClick = actions.onPostTitleClick,
                onAuthorClick = { showAuthorInfo = true },
                modifier = Modifier.padding(top = CONTENT_HEADER_GAP)
            )
        }
        if (uiState.parentAuthorName.isNotBlank()) {
            HorizontalDivider()
            CommentParentStrip(
                parentAuthorName = uiState.parentAuthorName,
                parentSnippet = uiState.parentSnippet,
                modifier = Modifier.padding(
                    horizontal = CONTENT_H_PADDING,
                    vertical = CONTENT_STRIP_V_PADDING
                )
            )
        }
        HorizontalDivider()
        // The reactions scroll with the comment rather than being pinned, as on iOS: they belong
        // to the comment above them, and pinning them would stack a second action bar directly on
        // top of the moderation toolbar.
        Column(
            modifier = Modifier.padding(horizontal = CONTENT_H_PADDING, vertical = CONTENT_V_PADDING)
        ) {
            CommentHtmlBody(html = uiState.commentText)
            CommentReactionRow(
                isLiked = uiState.isLiked,
                showLikeButton = showLikeButton,
                onReplyClick = onReplyClick,
                onLikeClick = actions.onLikeClick,
                hasReplyDraft = hasReplyDraft,
                modifier = Modifier
                    .padding(top = CONTENT_REACTIONS_GAP)
                    .offset(x = -REACTION_ROW_INSET)
            )
        }
    }
    if (showAuthorInfo) {
        // Keyed on the sheet being shown rather than the tap, so a sheet restored after process
        // death still loads its count and bio.
        LaunchedEffect(Unit) { actions.onAuthorInfoShown() }
        CommentAuthorInfoSheet(
            authorName = uiState.authorName,
            authorAvatarUrl = uiState.authorAvatarUrl,
            info = uiState.authorInfo,
            onDismiss = { showAuthorInfo = false }
        )
    }
}

private val CONTENT_H_PADDING = 16.dp
private val CONTENT_V_PADDING = 12.dp
private val CONTENT_HEADER_GAP = 12.dp
private val CONTENT_STRIP_V_PADDING = 10.dp
private val CONTENT_REACTIONS_GAP = 8.dp

// Cancels CommentReactionRow's own action padding so Reply lines up with the body text.
private val REACTION_ROW_INSET = 8.dp

@Composable
private fun ConfirmDialog(
    messageRes: Int,
    confirmRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(confirmRes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun UnifiedCommentDetailsScreenPreview() {
    AppThemeM3 {
        UnifiedCommentDetailsScreen(
            uiState = CommentDetailsUiState(
                contentVisible = true,
                authorName = "Jane Doe",
                datePublished = "2 hours ago",
                commentText = "This is a <b>great</b> post, thanks for sharing!",
                postTitle = "My first post",
                commentUrl = "https://example.com/post#comment-1",
                status = UNAPPROVED,
                canModerate = true
            ),
            replyText = TextFieldValue(""),
            onReplyTextChange = {},
            suggestions = emptyList(),
            showLikeButton = true,
            focusReplyFieldOnLaunch = false,
            snackbarHostState = SnackbarHostState(),
            actions = CommentDetailsActions(
                onModerateClick = {},
                onSpamClick = {},
                onLikeClick = {},
                onEditClick = {},
                onTrashClick = {},
                onRestoreClick = {},
                onDeletePermanentlyClick = {},
                onCopyLinkClick = {},
                onShareLinkClick = {},
                onPostTitleClick = {},
                onAuthorInfoShown = {},
                onSendReply = {}
            )
        )
    }
}
