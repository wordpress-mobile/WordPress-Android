package org.wordpress.android.ui.comments.unified.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH

/**
 * The secondary comment actions on the redesigned detail screen: edit, copy/share link and the
 * "move to pending" counterpart of Approve. Reply and like are not here - they sit in
 * [CommentReactionRow] directly under the comment, as on iOS.
 *
 * iOS puts these in the navigation bar. Android can't: this screen renders inside two different
 * hosts (the comments-list pager and the notification detail), each owning its own toolbar, so the
 * menu travels with the content instead and both hosts get it for free.
 */
@Composable
@Suppress("LongParameterList")
fun CommentDetailOverflowMenu(
    status: CommentStatus,
    showCommentUrlActions: Boolean,
    canModerate: Boolean,
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
private fun MenuItem(@StringRes labelResId: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(labelResId)) },
        onClick = onClick
    )
}
