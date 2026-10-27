package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.compose.RemoteImage

/**
 * The author block at the top of the redesigned comment detail, matching iOS's
 * `CommentAuthorHeader`: a 40dp circular avatar beside the author name, the post the comment was
 * left on, and the relative date.
 *
 * As on iOS the whole row opens the author-info sheet, with a trailing info icon as the cue. The
 * post line is the exception: its own tap opens the post in the Reader.
 */
@Composable
fun CommentAuthorHeader(
    authorName: String,
    authorAvatarUrl: String,
    postTitle: String,
    datePublished: String,
    onPostTitleClick: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.comment_author_info_show),
                onClick = onAuthorClick
            )
    ) {
        RemoteImage(
            imageUrl = authorAvatarUrl,
            fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = AVATAR_GAP)
        ) {
            Text(
                text = authorName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (postTitle.isNotBlank()) {
                Text(
                    text = onPostLink(postTitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Inner padding is inside the clickable, so the tap target clears the line.
                    modifier = Modifier
                        .padding(top = LINE_GAP)
                        .clickable(onClick = onPostTitleClick)
                        .padding(vertical = LINK_TOUCH_PADDING)
                )
            }
            Text(
                text = datePublished,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = LINE_GAP)
            )
        }
        // Decorative: the row's click label already announces the action.
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = AVATAR_GAP)
        )
    }
}

/**
 * "on {post title}", with only the title link-styled - the secondary "on" prefix is what makes the
 * title read as the link rather than the whole line.
 */
@Composable
private fun onPostLink(postTitle: String): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    val formatted = stringResource(R.string.comment_detail_on_post, postTitle)
    return remember(formatted, postTitle, linkColor) {
        buildAnnotatedString {
            append(formatted)
            val start = formatted.indexOf(postTitle)
            if (start >= 0) {
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    start,
                    start + postTitle.length
                )
            }
        }
    }
}

private val AVATAR_SIZE = 40.dp
private val AVATAR_GAP = 12.dp
private val LINE_GAP = 2.dp
private val LINK_TOUCH_PADDING = 4.dp

@Preview(showBackground = true)
@Composable
private fun CommentAuthorHeaderPreview() {
    AppThemeM3 {
        CommentAuthorHeader(
            authorName = "Jane Doe",
            authorAvatarUrl = "",
            postTitle = "My first post",
            datePublished = "2 hours ago",
            onPostTitleClick = {},
            onAuthorClick = {}
        )
    }
}
