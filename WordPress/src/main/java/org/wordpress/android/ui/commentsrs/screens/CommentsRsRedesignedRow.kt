package org.wordpress.android.ui.commentsrs.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.commentsrs.CommentRsUiModel
import org.wordpress.android.ui.compose.components.ShimmerBox
import org.wordpress.android.ui.rs.contentlist.ContentListBadges
import org.wordpress.android.ui.rs.contentlist.ContentListCard
import org.wordpress.android.ui.rs.contentlist.ContentListDensity

/**
 * A comment row in the redesigned list: the shared [ContentListCard] chrome, with a comment-shaped
 * body rather than the posts/pages one.
 *
 * The shared [org.wordpress.android.ui.rs.contentlist.ContentListRow] is deliberately not reused -
 * it is built around a trailing featured image and has no room for a leading avatar, a second
 * author line or a selected state, all of which this row needs. Sharing the card keeps the two
 * lists visually identical where it matters (surface, border, radius, spacing).
 *
 * Condensed density drops the comment snippet, which is the direct analogue of the posts list
 * dropping its excerpt - it is the only part of the row tall enough to matter.
 */
@Composable
fun CommentsRsRedesignedRow(
    comment: CommentRsUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    density: ContentListDensity = ContentListDensity.COMFORTABLE
) {
    ContentListCard(
        onClick = onClick,
        modifier = modifier,
        isSelected = isSelected,
        onLongClick = onLongClick
    ) {
        Row(modifier = Modifier.padding(CARD_PADDING)) {
            // Tapping the avatar toggles selection - the discoverable path into selection mode,
            // kept from the pre-redesign row so the gesture does not change with the flag.
            CommentAvatar(comment = comment, isSelected = isSelected, onClick = onLongClick)
            Column(
                modifier = Modifier
                    .padding(start = AVATAR_GAP)
                    .weight(1f)
            ) {
                comment.statusBadgeResId?.let { ContentListBadges(badges = listOf(it)) }
                Text(
                    text = commentTitle(comment),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TITLE_SIZE,
                    lineHeight = TITLE_LINE_HEIGHT,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
                if (!density.isCondensed && comment.snippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(TEXT_GAP))
                    Text(
                        text = comment.snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = SNIPPET_MAX_LINES,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(TEXT_GAP))
                Text(
                    text = comment.relativeDate,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = META_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A card-shaped loading placeholder, so the list does not jump when the comments land. */
@Composable
fun CommentsRsPlaceholderRow(modifier: Modifier = Modifier) {
    ContentListCard(onClick = {}, modifier = modifier) {
        Row(modifier = Modifier.padding(CARD_PADDING)) {
            ShimmerBox(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier
                    .padding(start = AVATAR_GAP)
                    .weight(1f)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(PLACEHOLDER_TITLE_WIDTH)
                        .height(PLACEHOLDER_TITLE_HEIGHT)
                        .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
                )
                Spacer(modifier = Modifier.height(TEXT_GAP))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(PLACEHOLDER_SNIPPET_WIDTH)
                        .height(PLACEHOLDER_META_HEIGHT)
                        .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
                )
                Spacer(modifier = Modifier.height(TEXT_GAP))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(PLACEHOLDER_META_WIDTH)
                        .height(PLACEHOLDER_META_HEIGHT)
                        .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
                )
            }
        }
    }
}

private val CARD_PADDING = 14.dp
private val AVATAR_SIZE = 40.dp
private val AVATAR_GAP = 12.dp
private val TEXT_GAP = 6.dp
private val TITLE_SIZE = 17.sp
private val TITLE_LINE_HEIGHT = 22.sp
private val META_SIZE = 13.sp
private const val TITLE_MAX_LINES = 2
private const val SNIPPET_MAX_LINES = 2
private const val PLACEHOLDER_TITLE_WIDTH = 0.7f
private const val PLACEHOLDER_SNIPPET_WIDTH = 0.95f
private const val PLACEHOLDER_META_WIDTH = 0.4f
private val PLACEHOLDER_TITLE_HEIGHT = 20.dp
private val PLACEHOLDER_META_HEIGHT = 13.dp
private val PLACEHOLDER_RADIUS = 4.dp
