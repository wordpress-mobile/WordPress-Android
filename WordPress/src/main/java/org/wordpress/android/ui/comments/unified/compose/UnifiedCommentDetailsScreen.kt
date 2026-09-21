package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsViewModel.CommentDetailsUiState
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.compose.RemoteImage
import org.wordpress.android.ui.suggestion.Suggestion

/**
 * The unified (wordpress-rs) comment detail screen: comment content in a weighted scrollable
 * region so the action footer and reply box stay pinned to the bottom while loading, plus the
 * trash/delete confirmation dialogs and the full-screen reply editor.
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
    modifier: Modifier = Modifier,
    isRedesignEnabled: Boolean = false
) {
    var showTrashConfirm by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showFullScreenReply by rememberSaveable { mutableStateOf(false) }

    // Opened from a notification's reply action. The redesign has no pinned field to focus, so
    // "start replying" means opening the reply screen. The host recomputes
    // focusReplyFieldOnLaunch as false after a config change, so this does not fire again on
    // rotation or re-open a screen the user dismissed.
    LaunchedEffect(Unit) {
        if (isRedesignEnabled && focusReplyFieldOnLaunch) showFullScreenReply = true
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
                // The weighted box keeps its space while the comment loads, pinning the action
                // footer and reply box to the bottom (the XML layout used INVISIBLE for this).
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.contentVisible) {
                        if (isRedesignEnabled) {
                            RedesignedCommentDetailsContent(
                                uiState = uiState,
                                showLikeButton = showLikeButton,
                                actions = actions,
                                onReplyClick = { showFullScreenReply = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CommentDetailsContent(
                                uiState = uiState,
                                onPostTitleClick = actions.onPostTitleClick,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(rememberNestedScrollInteropConnection())
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
                if (isRedesignEnabled) {
                    CommentModerationToolbar(
                        status = uiState.status,
                        canModerate = uiState.canModerate,
                        onApproveClick = actions.onModerateClick,
                        onSpamClick = actions.onSpamClick,
                        // Trashing is committed server-side immediately, so confirm it first.
                        onTrashClick = { showTrashConfirm = true },
                        // Restore is the inverse of however the comment got here: un-spam for a
                        // spam comment, untrash for a trashed one. Both ViewModel actions toggle
                        // back to approved, but only from their own status.
                        onRestoreClick = {
                            if (uiState.status == SPAM) actions.onSpamClick() else actions.onTrashClick()
                        },
                        onDeletePermanentlyClick = { showDeleteConfirm = true }
                    )
                } else {
                    CommentActionFooter(
                        status = uiState.status,
                        isLiked = uiState.isLiked,
                        showLikeButton = showLikeButton,
                        showCommentUrlActions = uiState.commentUrl.isNotEmpty(),
                        canModerate = uiState.canModerate,
                        onModerateClick = actions.onModerateClick,
                        onSpamClick = actions.onSpamClick,
                        onLikeClick = actions.onLikeClick,
                        onEditClick = actions.onEditClick,
                        // Trashing is committed server-side immediately (no undo affordance like
                        // the legacy list flow), so confirm it first; restoring needs none.
                        onTrashClick = {
                            if (uiState.status == TRASH) actions.onTrashClick() else showTrashConfirm = true
                        },
                        onCopyLinkClick = actions.onCopyLinkClick,
                        onShareLinkClick = actions.onShareLinkClick,
                        onDeletePermanentlyClick = { showDeleteConfirm = true }
                    )
                }
                // The redesign replies on a separate screen, reached from the Reply action under
                // the comment, so it has no pinned reply field.
                if (!isRedesignEnabled) {
                    CommentReplyBox(
                        replyText = replyText,
                        onReplyTextChange = onReplyTextChange,
                        suggestions = suggestions,
                        hint = replyHint,
                        isReplyInProgress = uiState.isReplyInProgress,
                        focusOnLaunch = focusReplyFieldOnLaunch,
                        onSendClick = { actions.onSendReply(replyText.text) },
                        onExpandClick = { showFullScreenReply = true }
                    )
                }
            }
            if (uiState.showProgress) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showTrashConfirm) {
        ConfirmDialog(
            messageRes = R.string.dlg_confirm_trash_comments,
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

    if (showFullScreenReply) {
        FullScreenReplyDialog(
            replyText = replyText,
            onReplyTextChange = onReplyTextChange,
            suggestions = suggestions,
            hint = replyHint,
            isReplyInProgress = uiState.isReplyInProgress,
            onSendClick = {
                showFullScreenReply = false
                actions.onSendReply(replyText.text)
            },
            onCollapseClick = { showFullScreenReply = false },
            isStandalone = isRedesignEnabled
        )
    }
}

/**
 * The redesigned content region, matching iOS's `CommentDetailView`: a fixed block of status pill,
 * author header and optional "in reply to" strip, then the comment body scrolling on its own below
 * a divider. The moderation toolbar and reply box are pinned by the caller.
 *
 * Keeping the header fixed rather than scrolling it away is the point of the design - who wrote
 * the comment and what state it's in are what the moderation decision is made on, so they stay put
 * however long the comment runs.
 */
@Composable
private fun RedesignedCommentDetailsContent(
    uiState: CommentDetailsUiState,
    showLikeButton: Boolean,
    actions: CommentDetailsActions,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                start = REDESIGN_H_PADDING,
                end = REDESIGN_H_PADDING,
                top = REDESIGN_V_PADDING,
                bottom = REDESIGN_V_PADDING
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommentStatusPill(status = uiState.status)
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
                modifier = Modifier.padding(top = REDESIGN_HEADER_GAP)
            )
        }
        if (uiState.parentAuthorName.isNotBlank()) {
            HorizontalDivider()
            CommentParentStrip(
                parentAuthorName = uiState.parentAuthorName,
                parentSnippet = uiState.parentSnippet,
                modifier = Modifier.padding(
                    horizontal = REDESIGN_H_PADDING,
                    vertical = REDESIGN_STRIP_V_PADDING
                )
            )
        }
        HorizontalDivider()
        // The reactions scroll with the comment rather than being pinned, as on iOS: they belong
        // to the comment above them, and pinning them would stack a second action bar directly on
        // top of the moderation toolbar.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(rememberNestedScrollInteropConnection())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = REDESIGN_H_PADDING, vertical = REDESIGN_V_PADDING)
        ) {
            CommentHtmlBody(html = uiState.commentText)
            CommentReactionRow(
                isLiked = uiState.isLiked,
                showLikeButton = showLikeButton,
                onReplyClick = onReplyClick,
                onLikeClick = actions.onLikeClick,
                // Pulled back by the action's own padding so it lines up with the body text.
                modifier = Modifier
                    .padding(top = REDESIGN_REACTIONS_GAP)
                    .offset(x = -REACTION_ROW_INSET)
            )
        }
    }
}

@Composable
private fun CommentDetailsContent(
    uiState: CommentDetailsUiState,
    onPostTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                imageUrl = uiState.authorAvatarUrl,
                fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = uiState.authorName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = uiState.datePublished,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
                    fontSize = 12.sp
                )
            }
            CommentStatusLabel(uiState.status)
        }

        if (uiState.postTitle.isNotBlank()) {
            Text(
                text = uiState.postTitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPostTitleClick)
                    .padding(top = 16.dp)
            )
        }

        CommentHtmlBody(
            html = uiState.commentText,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

private val REDESIGN_H_PADDING = 16.dp
private val REDESIGN_V_PADDING = 12.dp
private val REDESIGN_HEADER_GAP = 12.dp
private val REDESIGN_STRIP_V_PADDING = 10.dp
private val REDESIGN_REACTIONS_GAP = 8.dp

// Matches CommentReactionRow's own horizontal action padding, so the Reply icon sits flush with
// the comment text rather than indented by the tap target's padding.
private val REACTION_ROW_INSET = 8.dp

@Composable
private fun CommentStatusLabel(status: CommentStatus) {
    val labelRes = when (status) {
        APPROVED -> R.string.comment_status_approved
        UNAPPROVED -> R.string.comment_status_unapproved
        SPAM -> R.string.comment_status_spam
        TRASH -> R.string.comment_status_trash
        else -> R.string.comment_status_all
    }
    val isDestructive = status == TRASH || status == SPAM
    Text(
        text = stringResource(labelRes),
        color = if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA)
        },
        fontSize = 12.sp
    )
}

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
                onDeletePermanentlyClick = {},
                onCopyLinkClick = {},
                onShareLinkClick = {},
                onPostTitleClick = {},
                onSendReply = {}
            )
        )
    }
}
