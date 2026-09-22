package org.wordpress.android.ui.comments.unified.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

/**
 * The Reply and Like actions shown directly under the comment body, matching the reaction row on
 * iOS's comment detail.
 *
 * Unlike iOS there is no like *count* - the comment cache carries only whether the current user
 * liked it (`iLike`), with no total - so the label toggles between "Like" and "Liked" the way the
 * pre-redesign action footer did.
 */
@Composable
fun CommentReactionRow(
    isLiked: Boolean,
    showLikeButton: Boolean,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReactionAction(
            labelResId = R.string.reply,
            icon = Icons.AutoMirrored.Filled.Reply,
            onClick = onReplyClick
        )
        if (showLikeButton) {
            ReactionAction(
                labelResId = if (isLiked) R.string.mnu_comment_liked else R.string.like,
                icon = if (isLiked) Icons.Filled.Star else Icons.Filled.StarOutline,
                onClick = onLikeClick,
                isHighlighted = isLiked,
                modifier = Modifier.padding(start = ACTION_GAP)
            )
        }
    }
}

@Composable
private fun ReactionAction(
    @StringRes labelResId: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val contentColor = if (isHighlighted) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(ACTION_RADIUS))
            .clickable(onClick = onClick)
            // Inside the clickable, so the tap target clears the label's own line height.
            .padding(horizontal = ACTION_H_PADDING, vertical = ACTION_V_PADDING)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(ACTION_ICON_SIZE)
        )
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(start = ACTION_ICON_GAP)
        )
    }
}

private val ACTION_GAP = 8.dp
private val ACTION_ICON_SIZE = 18.dp
private val ACTION_ICON_GAP = 6.dp
private val ACTION_H_PADDING = 8.dp
private val ACTION_V_PADDING = 6.dp
private val ACTION_RADIUS = 8.dp

@Preview(showBackground = true)
@Composable
private fun CommentReactionRowPreview() {
    AppThemeM3 {
        CommentReactionRow(
            isLiked = false,
            showLikeButton = true,
            onReplyClick = {},
            onLikeClick = {}
        )
    }
}
