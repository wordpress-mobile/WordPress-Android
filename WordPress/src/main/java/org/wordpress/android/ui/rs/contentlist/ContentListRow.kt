package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ShimmerBox
import java.text.NumberFormat

/**
 * One row of the redesigned posts/pages list.
 *
 * Deliberately post- and page-agnostic: the overflow menu is a slot so each screen can supply its
 * own actions, and everything else is plain data. Both the posts and pages rs screens render from
 * this so the two lists cannot drift apart again.
 */
data class ContentListRowUiState(
    val id: Long,
    val title: String,
    val excerpt: String = "",
    val dateLabel: String,
    val imageUrl: String? = null,
    val isImagePending: Boolean = false,
    val viewCount: Long? = null,
    val commentCount: Long? = null,
    val areMetricsPending: Boolean = false,
    val badges: List<Int> = emptyList(),
    val isSyncing: Boolean = false,
    val hasSyncFailed: Boolean = false
)

/**
 * Compact row: title and metadata on the left, featured image trailing.
 *
 * Rows without a featured image let the text span the full card rather than reserving an empty
 * placeholder square, so a text-only blog does not read as a column of grey boxes.
 */
@Composable
fun ContentListRow(
    state: ContentListRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menu: (@Composable () -> Unit)? = null
) {
    ContentListCard(onClick = onClick, isSyncing = state.isSyncing, modifier = modifier) {
        Row(
            modifier = Modifier.padding(
                start = CARD_PADDING,
                top = CARD_PADDING,
                end = CARD_PADDING,
                bottom = CARD_PADDING_WITH_MENU
            ),
            horizontalArrangement = Arrangement.spacedBy(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                RowBadges(state.badges)
                RowTitle(title = state.title, fontSize = TITLE_SIZE, lineHeight = TITLE_LINE_HEIGHT)
                RowExcerpt(state.excerpt)
                Spacer(modifier = Modifier.height(TITLE_META_GAP))
                RowMetaLine(state = state, menu = menu)
            }
            RowThumbnail(imageUrl = state.imageUrl, isImagePending = state.isImagePending)
        }
    }
}

/**
 * Lead row: the featured image runs the full width of the card above the text. Used for the newest
 * item in a list, which is what gives the redesign its reason to care about featured images.
 */
@Composable
fun ContentListHeroRow(
    state: ContentListRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menu: (@Composable () -> Unit)? = null
) {
    ContentListCard(onClick = onClick, isSyncing = state.isSyncing, modifier = modifier) {
        Column {
            if (state.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.featured_image_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HERO_IMAGE_HEIGHT),
                    contentScale = ContentScale.Crop
                )
            } else if (state.isImagePending) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HERO_IMAGE_HEIGHT)
                )
            }
            Column(
                modifier = Modifier.padding(
                    start = CARD_PADDING,
                    top = CARD_PADDING,
                    end = CARD_PADDING,
                    bottom = CARD_PADDING_WITH_MENU
                )
            ) {
                RowBadges(state.badges)
                RowTitle(
                    title = state.title,
                    fontSize = HERO_TITLE_SIZE,
                    lineHeight = HERO_TITLE_LINE_HEIGHT
                )
                RowExcerpt(state.excerpt)
                Spacer(modifier = Modifier.height(TITLE_META_GAP))
                RowMetaLine(state = state, menu = menu)
            }
        }
    }
}

/** Section header separating the date buckets, e.g. "THIS WEEK". */
@Composable
fun ContentListGroupHeader(
    group: ContentDateGroup,
    modifier: Modifier = Modifier
) {
    Text(
        text = group.label().uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = GROUP_HEADER_TRACKING,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            start = GROUP_HEADER_INSET,
            end = GROUP_HEADER_INSET,
            top = GROUP_HEADER_TOP_PADDING,
            bottom = GROUP_HEADER_BOTTOM_PADDING
        )
    )
}

/** Loading placeholder shaped like [ContentListRow] so the list does not jump when data lands. */
@Composable
fun ContentListPlaceholderRow(modifier: Modifier = Modifier) {
    ContentListCard(onClick = null, isSyncing = false, modifier = modifier) {
        Row(
            modifier = Modifier.padding(CARD_PADDING),
            horizontalArrangement = Arrangement.spacedBy(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(PLACEHOLDER_TITLE_WIDTH)
                        .height(PLACEHOLDER_TITLE_HEIGHT)
                        .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
                )
                repeat(EXCERPT_MAX_LINES) {
                    Spacer(modifier = Modifier.height(TITLE_META_GAP))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(PLACEHOLDER_EXCERPT_WIDTH)
                            .height(PLACEHOLDER_META_HEIGHT)
                            .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
                    )
                }
                Spacer(modifier = Modifier.height(TITLE_META_GAP))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(PLACEHOLDER_META_WIDTH)
                        .height(PLACEHOLDER_META_HEIGHT)
                        .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
                )
            }
            ShimmerBox(
                modifier = Modifier
                    .size(THUMBNAIL_SIZE)
                    .clip(RoundedCornerShape(THUMBNAIL_RADIUS))
            )
        }
    }
}

@Composable
private fun ContentListCard(
    onClick: (() -> Unit)?,
    isSyncing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(CARD_BORDER_WIDTH, MaterialTheme.colorScheme.outlineVariant)
    val shape = RoundedCornerShape(CARD_RADIUS)
    val cardModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = LIST_HORIZONTAL_PADDING, vertical = CARD_VERTICAL_SPACING)

    // The design leans on a hairline border rather than a shadow, so elevation stays flat.
    if (onClick == null) {
        Card(modifier = cardModifier, colors = colors, border = border, shape = shape) {
            CardBody(isSyncing = isSyncing, content = content)
        }
    } else {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = colors,
            border = border,
            shape = shape
        ) {
            CardBody(isSyncing = isSyncing, content = content)
        }
    }
}

@Composable
private fun CardBody(isSyncing: Boolean, content: @Composable () -> Unit) {
    content()
    if (isSyncing) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(SYNC_BAR_HEIGHT),
            color = MaterialTheme.colorScheme.primary.copy(alpha = SYNC_BAR_ALPHA),
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun RowTitle(
    title: String,
    fontSize: TextUnit,
    lineHeight: TextUnit
) {
    Text(
        text = title.ifBlank { stringResource(R.string.untitled_in_parentheses) },
        style = MaterialTheme.typography.titleMedium,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = TITLE_MAX_LINES,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RowExcerpt(excerpt: String) {
    if (excerpt.isBlank()) return
    Spacer(modifier = Modifier.height(TITLE_META_GAP))
    Text(
        text = excerpt,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = EXCERPT_MAX_LINES,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * "2d ago · 1,204 views", with the overflow menu pinned to the trailing edge.
 *
 * The separator is drawn as its own [Text] so it can take the dimmer outline colour without
 * splitting the line into something a screen reader announces piecemeal.
 */
@Composable
private fun RowMetaLine(
    state: ContentListRowUiState,
    menu: (@Composable () -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetaText(state.dateLabel)
            if (state.areMetricsPending) {
                // Views and comments arrive together, one request per row, so a single bar stands
                // in for both rather than two that would resolve on the same frame anyway.
                MetaSeparator()
                ShimmerBox(
                    modifier = Modifier
                        .width(METRICS_SKELETON_WIDTH)
                        .height(METRICS_SKELETON_HEIGHT)
                        .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
                )
            } else {
                state.viewCount?.let { views ->
                    MetaSeparator()
                    MetaText(
                        pluralStringResource(
                            R.plurals.content_list_view_count,
                            views.toInt(),
                            NumberFormat.getIntegerInstance().format(views)
                        )
                    )
                }
                // A post with no comments still says so; unlike views, zero is meaningful here and
                // the number arrives in the same response, so hiding it would look like a gap.
                state.commentCount?.let { comments ->
                    MetaSeparator()
                    MetaText(
                        pluralStringResource(
                            R.plurals.content_list_comment_count,
                            comments.toInt(),
                            NumberFormat.getIntegerInstance().format(comments)
                        )
                    )
                }
            }
            if (state.hasSyncFailed) {
                MetaSeparator()
                Text(
                    text = stringResource(R.string.post_rs_sync_failed),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = META_SIZE,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (menu != null) {
            menu()
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontSize = META_SIZE,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MetaSeparator() {
    Text(
        text = stringResource(R.string.bullet_with_spaces),
        style = MaterialTheme.typography.bodySmall,
        fontSize = META_SIZE,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun RowBadges(@StringRes badges: List<Int>) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = Modifier.padding(bottom = BADGE_BOTTOM_PADDING),
        horizontalArrangement = Arrangement.spacedBy(BADGE_SPACING)
    ) {
        badges.forEach { labelResId ->
            Text(
                text = stringResource(labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .border(
                        width = CARD_BORDER_WIDTH,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = BADGE_BORDER_ALPHA),
                        shape = RoundedCornerShape(BADGE_RADIUS)
                    )
                    .padding(horizontal = BADGE_H_PADDING, vertical = BADGE_V_PADDING)
            )
        }
    }
}

@Composable
private fun RowThumbnail(imageUrl: String?, isImagePending: Boolean) {
    when {
        imageUrl != null -> AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.featured_image_desc),
            modifier = Modifier
                .size(THUMBNAIL_SIZE)
                .clip(RoundedCornerShape(THUMBNAIL_RADIUS)),
            contentScale = ContentScale.Crop
        )
        isImagePending -> ShimmerBox(
            modifier = Modifier
                .size(THUMBNAIL_SIZE)
                .clip(RoundedCornerShape(THUMBNAIL_RADIUS))
        )
        else -> Unit
    }
}

private val LIST_HORIZONTAL_PADDING = 12.dp
private val CARD_VERTICAL_SPACING = 4.dp
private val CARD_PADDING = 14.dp

// The overflow button carries its own 48dp touch target, whose internal padding supplies most of
// the card's bottom inset - so the inset itself is trimmed to keep the row from growing.
private val CARD_PADDING_WITH_MENU = 4.dp
private val CARD_RADIUS = 14.dp
private val CARD_BORDER_WIDTH = 1.dp
private val THUMBNAIL_SIZE = 72.dp
private val THUMBNAIL_RADIUS = 10.dp
private val HERO_IMAGE_HEIGHT = 130.dp
private val TITLE_META_GAP = 6.dp
private val SYNC_BAR_HEIGHT = 2.dp
private const val SYNC_BAR_ALPHA = 0.5f
private const val TITLE_MAX_LINES = 2
private const val EXCERPT_MAX_LINES = 2
private val TITLE_SIZE = 19.sp
private val TITLE_LINE_HEIGHT = 24.sp
private val HERO_TITLE_SIZE = 21.sp
private val HERO_TITLE_LINE_HEIGHT = 26.sp
private val META_SIZE = 13.sp

// Roughly the width of "1,204 views · 8 comments", so the row does not visibly reflow when the
// real numbers replace the bar.
private val METRICS_SKELETON_WIDTH = 132.dp
private val METRICS_SKELETON_HEIGHT = 11.dp
private val GROUP_HEADER_TRACKING = 1.3.sp
private val GROUP_HEADER_INSET = 16.dp
private val GROUP_HEADER_TOP_PADDING = 16.dp
private val GROUP_HEADER_BOTTOM_PADDING = 4.dp
private val BADGE_BOTTOM_PADDING = 6.dp
private val BADGE_SPACING = 4.dp
private val BADGE_RADIUS = 4.dp
private val BADGE_H_PADDING = 6.dp
private val BADGE_V_PADDING = 2.dp
private const val BADGE_BORDER_ALPHA = 0.5f
private const val PLACEHOLDER_TITLE_WIDTH = 0.7f
private const val PLACEHOLDER_META_WIDTH = 0.4f
private const val PLACEHOLDER_EXCERPT_WIDTH = 0.95f
private val PLACEHOLDER_TITLE_HEIGHT = 20.dp
private val PLACEHOLDER_META_HEIGHT = 13.dp
private val PLACEHOLDER_RADIUS = 4.dp
