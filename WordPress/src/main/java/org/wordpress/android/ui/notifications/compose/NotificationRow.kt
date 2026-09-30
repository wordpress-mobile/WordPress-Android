package org.wordpress.android.ui.notifications.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.rs.contentlist.ContentListCard

/** A notification in the shared [ContentListCard] chrome used by the rs lists. */
@Composable
fun NotificationRow(
    row: NotificationRowUiModel,
    onClick: () -> Unit,
    onInlineAction: (NotificationInlineAction) -> Unit,
    modifier: Modifier = Modifier
) {
    ContentListCard(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier.padding(
                start = CARD_PADDING,
                top = CARD_PADDING,
                bottom = CARD_PADDING,
                // The action button carries its own inset.
                end = if (row.inlineAction == null) CARD_PADDING else 0.dp
            )
        ) {
            NotificationAvatars(urls = row.avatarUrls, isUnread = row.isUnread)
            Column(
                modifier = Modifier
                    .padding(start = AVATAR_GAP)
                    .weight(1f)
            ) {
                Row {
                    if (row.hasUserReplied) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = REPLY_ICON_TOP_PADDING, end = REPLY_ICON_GAP)
                                .size(REPLY_ICON_SIZE)
                        )
                    }
                    Text(
                        text = row.subject,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = SUBJECT_MAX_LINES,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (row.snippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(TEXT_GAP))
                    Text(
                        text = row.snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = SNIPPET_MAX_LINES,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(TEXT_GAP))
                Text(
                    text = row.dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = META_SIZE,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            row.inlineAction?.let { action ->
                InlineActionButton(action = action, onClick = { onInlineAction(action) })
            }
        }
    }
}

@Composable
private fun InlineActionButton(action: NotificationInlineAction, onClick: () -> Unit) {
    val isLiked = when (action) {
        is NotificationInlineAction.LikeComment -> action.isLiked
        is NotificationInlineAction.LikePost -> action.isLiked
        NotificationInlineAction.Share -> false
    }
    val iconResId = when {
        action is NotificationInlineAction.Share -> R.drawable.block_share
        isLiked -> R.drawable.star_filled
        else -> R.drawable.star_empty
    }
    val labelResId = when {
        action is NotificationInlineAction.Share -> R.string.share_action
        isLiked -> R.string.mnu_comment_liked
        else -> R.string.reader_label_like
    }
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = stringResource(labelResId),
            tint = if (isLiked) {
                colorResource(R.color.inline_action_filled)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(ACTION_ICON_SIZE)
        )
    }
}

@Composable
private fun NotificationAvatars(urls: List<String>, isUnread: Boolean) {
    Box(modifier = Modifier.size(AVATAR_SIZE)) {
        when (urls.size) {
            0, 1 -> Avatar(url = urls.firstOrNull(), size = AVATAR_SIZE)
            2 -> {
                Avatar(url = urls[1], size = DOUBLE_AVATAR_SIZE, modifier = Modifier.align(Alignment.TopStart))
                Avatar(url = urls[0], size = DOUBLE_AVATAR_SIZE, modifier = Modifier.align(Alignment.BottomEnd))
            }
            else -> {
                Avatar(url = urls[2], size = TRIPLE_AVATAR_SIZE, modifier = Modifier.align(Alignment.BottomStart))
                Avatar(url = urls[1], size = TRIPLE_AVATAR_SIZE, modifier = Modifier.align(Alignment.TopCenter))
                Avatar(url = urls[0], size = TRIPLE_AVATAR_SIZE, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
        if (isUnread) {
            val unreadLabel = stringResource(R.string.notifications_unread_content_description)
            Box(
                modifier = Modifier
                    .offset(x = -UNREAD_DOT_OFFSET, y = -UNREAD_DOT_OFFSET)
                    .size(UNREAD_DOT_SIZE)
                    .border(AVATAR_BORDER_WIDTH, MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(AVATAR_BORDER_WIDTH)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .semantics { contentDescription = unreadLabel }
            )
        }
    }
}

@Composable
private fun Avatar(url: String?, size: Dp, modifier: Modifier = Modifier) {
    val fallback = rememberVectorPainter(Icons.Filled.Person)
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = fallback,
        fallback = fallback,
        error = fallback,
        modifier = modifier
            .size(size)
            .border(AVATAR_BORDER_WIDTH, MaterialTheme.colorScheme.surface, CircleShape)
            .padding(AVATAR_BORDER_WIDTH)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

private val CARD_PADDING = 14.dp
private val AVATAR_SIZE = 40.dp
private val DOUBLE_AVATAR_SIZE = 28.dp
private val TRIPLE_AVATAR_SIZE = 23.dp
private val AVATAR_BORDER_WIDTH = 1.5.dp
private val AVATAR_GAP = 12.dp
private val UNREAD_DOT_SIZE = 12.dp
private val UNREAD_DOT_OFFSET = 3.dp
private val TEXT_GAP = 4.dp
private val REPLY_ICON_SIZE = 16.dp
private val REPLY_ICON_GAP = 4.dp
private val REPLY_ICON_TOP_PADDING = 3.dp
private val ACTION_ICON_SIZE = 20.dp
private val META_SIZE = 13.sp
private const val SUBJECT_MAX_LINES = 2
private const val SNIPPET_MAX_LINES = 2
