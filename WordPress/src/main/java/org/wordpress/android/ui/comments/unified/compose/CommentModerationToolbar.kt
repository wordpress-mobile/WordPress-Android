package org.wordpress.android.ui.comments.unified.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.ui.compose.theme.AppThemeM3

/**
 * The moderation controls pinned to the bottom of the redesigned comment detail, matching iOS's
 * `CommentModerationToolbar`: full-width buttons whose shape is driven by the comment's status
 * rather than a fixed row of every action.
 *
 * - pending: a prominent **Approve**, over a Spam | Trash row
 * - approved: just the Spam | Trash row
 * - spam or trashed: **Restore**, over **Delete Permanently**
 *
 * Renders nothing when the user can't moderate - unlike the pre-redesign footer, which showed the
 * actions disabled. There is nothing else on this bar to keep, so an all-disabled row would be a
 * strip of dead controls; reply and the overflow actions live elsewhere and stay reachable.
 */
@Composable
fun CommentModerationToolbar(
    status: CommentStatus,
    canModerate: Boolean,
    onApproveClick: () -> Unit,
    onSpamClick: () -> Unit,
    onTrashClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeletePermanentlyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!canModerate) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TOOLBAR_H_PADDING, vertical = TOOLBAR_V_PADDING),
        verticalArrangement = Arrangement.spacedBy(BUTTON_GAP)
    ) {
        when (status) {
            SPAM, TRASH -> {
                OutlinedToolbarButton(
                    // Restore reads as the inverse of however the comment got here.
                    labelResId = if (status == SPAM) {
                        R.string.mnu_comment_unspam
                    } else {
                        R.string.mnu_comment_untrash
                    },
                    icon = Icons.Filled.Restore,
                    onClick = onRestoreClick,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedToolbarButton(
                    labelResId = R.string.mnu_comment_delete_permanently,
                    icon = Icons.Filled.DeleteForever,
                    onClick = onDeletePermanentlyClick,
                    isDestructive = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                if (status == UNAPPROVED) {
                    Button(
                        onClick = onApproveClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ButtonContent(R.string.mnu_comment_approve, Icons.Filled.Check)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP)) {
                    SpamAndTrashButtons(
                        status = status,
                        onSpamClick = onSpamClick,
                        onTrashClick = onTrashClick
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.SpamAndTrashButtons(
    status: CommentStatus,
    onSpamClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    OutlinedToolbarButton(
        labelResId = if (status == SPAM) R.string.mnu_comment_unspam else R.string.mnu_comment_spam,
        icon = Icons.Filled.Block,
        onClick = onSpamClick,
        isDestructive = true,
        modifier = Modifier.weight(1f)
    )
    OutlinedToolbarButton(
        labelResId = R.string.mnu_comment_trash,
        icon = Icons.Filled.Delete,
        onClick = onTrashClick,
        isDestructive = true,
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun OutlinedToolbarButton(
    @StringRes labelResId: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
    ) {
        ButtonContent(labelResId, icon)
    }
}

@Composable
private fun ButtonContent(@StringRes labelResId: Int, icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(BUTTON_ICON_SIZE)
    )
    Text(
        text = stringResource(labelResId),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = BUTTON_ICON_GAP)
    )
}

private val TOOLBAR_H_PADDING = 16.dp
private val TOOLBAR_V_PADDING = 10.dp
private val BUTTON_GAP = 10.dp
private val BUTTON_ICON_SIZE = 18.dp
private val BUTTON_ICON_GAP = 8.dp

@Preview(showBackground = true, name = "Pending")
@Composable
private fun CommentModerationToolbarPendingPreview() {
    AppThemeM3 {
        CommentModerationToolbar(
            status = UNAPPROVED,
            canModerate = true,
            onApproveClick = {},
            onSpamClick = {},
            onTrashClick = {},
            onRestoreClick = {},
            onDeletePermanentlyClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Approved")
@Composable
private fun CommentModerationToolbarApprovedPreview() {
    AppThemeM3 {
        CommentModerationToolbar(
            status = APPROVED,
            canModerate = true,
            onApproveClick = {},
            onSpamClick = {},
            onTrashClick = {},
            onRestoreClick = {},
            onDeletePermanentlyClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Trashed")
@Composable
private fun CommentModerationToolbarTrashedPreview() {
    AppThemeM3 {
        CommentModerationToolbar(
            status = TRASH,
            canModerate = true,
            onApproveClick = {},
            onSpamClick = {},
            onTrashClick = {},
            onRestoreClick = {},
            onDeletePermanentlyClick = {}
        )
    }
}
