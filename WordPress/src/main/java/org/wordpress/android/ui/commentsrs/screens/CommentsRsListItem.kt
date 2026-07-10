package org.wordpress.android.ui.commentsrs.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.commentsrs.CommentRsUiModel

private val AVATAR_SIZE = 40.dp
private val PENDING_INDICATOR_WIDTH = 4.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentsRsListItem(
    comment: CommentRsUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // The pending indicator: a colored bar on the row's leading edge, like the legacy
            // list's status indicator.
            Box(
                modifier = Modifier
                    .width(PENDING_INDICATOR_WIDTH)
                    .fillMaxHeight()
                    .background(
                        if (comment.isPending) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            Color.Transparent
                        }
                    )
            )
            Row(modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
                // Tapping the avatar toggles selection (the discoverable path into selection
                // mode); long-press anywhere on the row does the same.
                CommentAvatar(comment = comment, isSelected = isSelected, onClick = onLongClick)
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = commentTitle(comment),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (comment.snippet.isNotBlank()) {
                        Text(
                            text = comment.snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        text = comment.relativeDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
    }
}

@Composable
private fun CommentAvatar(comment: CommentRsUiModel, isSelected: Boolean, onClick: () -> Unit) {
    Crossfade(targetState = isSelected, label = "avatar") { selected ->
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.comment_checkmark_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
            )
        } else {
            val fallback = rememberVectorPainter(Icons.Filled.Person)
            AsyncImage(
                model = comment.avatarUrl.ifBlank { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                fallback = fallback,
                error = fallback,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
            )
        }
    }
}

/**
 * The row title: "{author} on {post title}" via the shared [R.string.comment_title] template with
 * both parts bold (like legacy `CommentListUiUtils.formatCommentTitle`), or just the bold author
 * name while the post title is unresolved.
 */
@Composable
private fun commentTitle(comment: CommentRsUiModel): AnnotatedString {
    val postTitle = comment.postTitle?.trim().orEmpty()
    val formatted = if (postTitle.isEmpty()) {
        comment.authorName
    } else {
        stringResource(R.string.comment_title, comment.authorName, postTitle)
    }
    return buildAnnotatedString {
        append(formatted)
        boldRange(formatted, comment.authorName)
        if (postTitle.isNotEmpty()) boldRange(formatted, postTitle)
    }
}

private fun AnnotatedString.Builder.boldRange(formatted: String, part: String) {
    val start = formatted.indexOf(part)
    if (start >= 0) {
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + part.length)
    }
}
