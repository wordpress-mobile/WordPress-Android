package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.compose.theme.AppThemeM3

/**
 * The comment's moderation status as a tinted capsule at the top of the detail screen, matching
 * iOS's `CommentStatusPill`.
 *
 * The tints are the app's own Material roles rather than iOS's literal green/orange/red, so the
 * pill stays legible in both themes. Pending takes `tertiary`, which is the role the rs comments
 * list already uses for its pending marker, so the list and the detail agree.
 */
@Composable
fun CommentStatusPill(
    status: CommentStatus,
    modifier: Modifier = Modifier
) {
    val tint = status.pillTint()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(tint.copy(alpha = PILL_CONTAINER_ALPHA), CircleShape)
            .padding(horizontal = PILL_H_PADDING, vertical = PILL_V_PADDING)
    ) {
        Icon(
            imageVector = status.pillIcon(),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(PILL_ICON_SIZE)
        )
        Text(
            text = stringResource(status.pillLabelResId()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(start = PILL_ICON_GAP)
        )
    }
}

@Composable
private fun CommentStatus.pillTint(): Color = when (this) {
    CommentStatus.APPROVED -> MaterialTheme.colorScheme.primary
    CommentStatus.UNAPPROVED -> MaterialTheme.colorScheme.tertiary
    CommentStatus.SPAM, CommentStatus.TRASH, CommentStatus.DELETED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun CommentStatus.pillIcon(): ImageVector = when (this) {
    CommentStatus.APPROVED -> Icons.Filled.CheckCircle
    CommentStatus.UNAPPROVED -> Icons.Filled.Schedule
    CommentStatus.SPAM -> Icons.Filled.Block
    CommentStatus.TRASH, CommentStatus.DELETED -> Icons.Filled.Delete
    else -> Icons.AutoMirrored.Filled.HelpOutline
}

private fun CommentStatus.pillLabelResId(): Int = when (this) {
    CommentStatus.APPROVED -> R.string.comment_status_approved
    CommentStatus.UNAPPROVED -> R.string.comment_status_unapproved
    CommentStatus.SPAM -> R.string.comment_status_spam
    CommentStatus.TRASH, CommentStatus.DELETED -> R.string.comment_status_trash
    else -> R.string.comment_status_all
}

private const val PILL_CONTAINER_ALPHA = 0.15f
private val PILL_H_PADDING = 10.dp
private val PILL_V_PADDING = 4.dp
private val PILL_ICON_SIZE = 16.dp
private val PILL_ICON_GAP = 6.dp

@Preview(showBackground = true)
@Composable
private fun CommentStatusPillPreview() {
    AppThemeM3 {
        CommentStatusPill(status = CommentStatus.UNAPPROVED)
    }
}
