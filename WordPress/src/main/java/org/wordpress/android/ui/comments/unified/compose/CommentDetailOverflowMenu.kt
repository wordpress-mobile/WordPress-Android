package org.wordpress.android.ui.comments.unified.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH

/**
 * The secondary comment actions on the redesigned detail screen: like, edit, copy/share link and
 * the "move to pending" counterpart of Approve.
 *
 * iOS puts these in the navigation bar. Android can't: this screen renders inside two different
 * hosts (the comments-list pager and the notification detail), each owning its own toolbar, so the
 * menu travels with the content instead and both hosts get it for free.
 */
@Composable
@Suppress("LongParameterList")
fun CommentDetailOverflowMenu(
    status: CommentStatus,
    isLiked: Boolean,
    showLikeButton: Boolean,
    showCommentUrlActions: Boolean,
    canModerate: Boolean,
    onLikeClick: () -> Unit,
    onUnapproveClick: () -> Unit,
    onEditClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onShareLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (showLikeButton) {
                val likeTint = if (isLiked) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                MenuItem(
                    labelResId = if (isLiked) R.string.mnu_comment_liked else R.string.like,
                    onClick = {
                        expanded = false
                        onLikeClick()
                    },
                    icon = {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = null,
                            tint = likeTint
                        )
                    }
                )
            }
            // The counterpart to the toolbar's Approve: with the comment already approved there is
            // no prominent button for it, so unapproving lives here - as it does on iOS.
            if (canModerate && status == APPROVED) {
                MenuItem(
                    labelResId = R.string.mnu_comment_unapprove,
                    onClick = {
                        expanded = false
                        onUnapproveClick()
                    }
                )
            }
            if (canModerate && status != TRASH && status != SPAM) {
                MenuItem(
                    labelResId = R.string.edit,
                    onClick = {
                        expanded = false
                        onEditClick()
                    }
                )
            }
            if (showCommentUrlActions) {
                MenuItem(
                    labelResId = R.string.copy_link_address,
                    onClick = {
                        expanded = false
                        onCopyLinkClick()
                    }
                )
                MenuItem(
                    labelResId = R.string.share_link,
                    onClick = {
                        expanded = false
                        onShareLinkClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
    @StringRes labelResId: Int,
    onClick: () -> Unit,
    color: Color = Color.Unspecified,
    icon: (@Composable () -> Unit)? = null
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(labelResId), color = color) },
        onClick = onClick,
        leadingIcon = icon
    )
}
