package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ShimmerBox
import java.text.NumberFormat

/**
 * Compact row: title and metadata on the left, featured image trailing.
 *
 * Rows without a featured image let the text span the full card rather than reserving an empty
 * placeholder square, so a text-only blog does not read as a column of grey boxes.
 *
 * [leading] draws ahead of the text, for rows that stand for something other than a plain entry -
 * the pages list marks its homepage and posts-page rows that way.
 *
 * [actions] supplies the overflow menu. When it also has quick actions, the metrics and the menu
 * move into a footer beneath the text with the actions as icon buttons between them, and the date
 * moves up under the title.
 */
@Composable
fun ContentListRow(
    state: ContentListRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    density: ContentListDensity = ContentListDensity.COMFORTABLE,
    actions: ContentListRowActions = ContentListRowActions(),
    leading: (@Composable () -> Unit)? = null
) {
    val padding = if (density.isCondensed) CONDENSED_CARD_PADDING else CARD_PADDING
    ContentListCard(onClick = onClick, isSyncing = state.isSyncing, modifier = modifier) {
        RowTextAndMenu(
            state = state,
            titleSize = TITLE_SIZE,
            titleLineHeight = TITLE_LINE_HEIGHT,
            density = density,
            padding = padding,
            actions = actions,
            leading = leading
        ) {
            FeaturedImage(
                imageUrl = state.thumbnailImageUrl,
                isImagePending = state.isImagePending,
                modifier = Modifier
                    .padding(start = padding)
                    .size(if (density.isCondensed) CONDENSED_THUMBNAIL_SIZE else THUMBNAIL_SIZE)
                    .clip(RoundedCornerShape(THUMBNAIL_RADIUS))
            )
        }
    }
}

/**
 * Image-led row: the featured image runs the full width of the card above the text. Used for every
 * post that has one, which is what gives the redesign its reason to care about featured images.
 *
 * Comfortable density only - a condensed list draws no images this way, so this is never condensed.
 */
@Composable
fun ContentListHeroRow(
    state: ContentListRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: ContentListRowActions = ContentListRowActions(),
    leading: (@Composable () -> Unit)? = null
) {
    ContentListCard(onClick = onClick, isSyncing = state.isSyncing, modifier = modifier) {
        Column {
            FeaturedImage(
                imageUrl = state.heroImageUrl,
                isImagePending = state.isImagePending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HERO_IMAGE_HEIGHT)
            )
            RowTextAndMenu(
                state = state,
                titleSize = HERO_TITLE_SIZE,
                titleLineHeight = HERO_TITLE_LINE_HEIGHT,
                density = ContentListDensity.COMFORTABLE,
                padding = CARD_PADDING,
                actions = actions,
                leading = leading
            )
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
    // Its own Card rather than ContentListCard: a placeholder is not clickable, and it is the only
    // caller that would need that to be optional.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LIST_HORIZONTAL_PADDING, vertical = CARD_VERTICAL_SPACING),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(CARD_BORDER_WIDTH, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(CARD_RADIUS)
    ) {
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

/**
 * The card chrome every redesigned content row sits in: a flat `surface` card with a hairline
 * border, used by the posts, pages and comments lists so all three read as one list style.
 *
 * [isSelected] tints the container and border, for lists with a multi-select mode (comments).
 * [onLongClick] is what enters that mode; supplying it swaps the Card's own click handling for a
 * `combinedClickable`, so lists that don't need it keep the plain clickable Card unchanged.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentListCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val cardModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = LIST_HORIZONTAL_PADDING, vertical = CARD_VERTICAL_SPACING)
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val border = BorderStroke(CARD_BORDER_WIDTH, borderColor)
    val shape = RoundedCornerShape(CARD_RADIUS)

    // The design leans on a hairline border rather than a shadow, so elevation stays flat.
    if (onLongClick == null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = colors,
            border = border,
            shape = shape
        ) {
            CardBody(isSyncing = isSyncing, content = content)
        }
    } else {
        Card(
            modifier = cardModifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
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

/**
 * The text stack shared by both row shapes: badges, title, excerpt, then the metadata line. Only
 * the title's size, the density and whether the metadata line carries the overflow button differ.
 *
 * Neither shape puts the overflow button in here: both place it beside this column so that it lands
 * the same distance from the card edge on every row.
 *
 * Condensed drops the excerpt, which is what actually shortens the row, and the metrics, which the
 * ViewModel then does not fetch.
 *
 * [showMetaLine] is false when the row has a footer, which carries the metrics instead. The date
 * then sits under the title with the sync-failed warning, leaving the footer's narrower line room
 * for both metrics.
 */
@Composable
private fun RowBody(
    state: ContentListRowUiState,
    titleSize: TextUnit,
    titleLineHeight: TextUnit,
    density: ContentListDensity,
    showMetaLine: Boolean
) {
    ContentListBadges(state.badges)
    RowTitle(title = state.title, fontSize = titleSize, lineHeight = titleLineHeight)
    if (!showMetaLine && state.dateLabel.isNotBlank()) {
        Spacer(modifier = Modifier.height(TITLE_DATE_GAP))
        RowMetaLine(state = state, showDate = true, showMetrics = false, showSyncFailed = true)
    }
    if (!density.isCondensed) {
        RowExcerpt(state.excerpt)
    }
    // A row with no date - the pages list's synthetic Site Editor entry - has no metrics or sync
    // state either, so it drops the line altogether.
    if (showMetaLine && state.dateLabel.isNotBlank()) {
        Spacer(modifier = Modifier.height(TITLE_META_GAP))
        RowMetaLine(
            state = state,
            showDate = true,
            showMetrics = !density.isCondensed,
            showSyncFailed = true
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
 * "2d ago · 1,204 views", or just the metrics when the date sits under the title instead.
 *
 * The separator is drawn as its own [Text] so it can take the dimmer outline colour without
 * splitting the line into something a screen reader announces piecemeal.
 */
@Composable
private fun RowMetaLine(
    state: ContentListRowUiState,
    showDate: Boolean,
    showMetrics: Boolean,
    showSyncFailed: Boolean
) {
    val segments = buildList<@Composable () -> Unit> {
        if (showDate) add { MetaText(state.dateLabel) }
        // A condensed list does not fetch metrics, so it shows neither them nor a skeleton
        // waiting on a request that is never made.
        if (showMetrics) addMetrics(state)
        if (showSyncFailed && state.hasSyncFailed) {
            add {
                MetaText(
                    text = stringResource(R.string.post_rs_sync_failed),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) MetaSeparator()
            segment()
        }
    }
}

/** One metric, e.g. "1,204 views". */
@Composable
private fun MetricText(@PluralsRes pluralResId: Int, count: Long) {
    MetaText(
        pluralStringResource(
            pluralResId,
            count.toInt(),
            NumberFormat.getIntegerInstance().format(count)
        )
    )
}

/** Views and comments, or a single bar standing in for both while they are still being fetched. */
private fun MutableList<@Composable () -> Unit>.addMetrics(state: ContentListRowUiState) {
    if (state.areMetricsPending) {
        // Views and comments arrive together, so one bar stands in for both rather than two that
        // would resolve on the same frame anyway.
        add {
            ShimmerBox(
                modifier = Modifier
                    .width(METRICS_SKELETON_WIDTH)
                    .height(METRICS_SKELETON_HEIGHT)
                    .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
            )
        }
        return
    }
    state.viewCount?.let { count -> add { MetricText(R.plurals.content_list_view_count, count) } }
    // A post with no comments still says so; unlike views, zero is meaningful here and the number
    // arrives in the same response, so hiding it would look like a gap.
    state.commentCount?.let { count -> add { MetricText(R.plurals.content_list_comment_count, count) } }
}

@Composable
private fun MetaText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontSize = META_SIZE,
        color = color,
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
        color = MaterialTheme.colorScheme.outlineVariant,
        maxLines = 1
    )
}

/**
 * The short outlined qualifiers that sit above a row's title - a page's "Homepage", a comment's
 * "Pending". Public so every redesigned list labels the same way; [badges] are string resource ids.
 */
@Composable
fun ContentListBadges(
    @StringRes badges: List<Int>,
    modifier: Modifier = Modifier
) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = modifier.padding(bottom = BADGE_BOTTOM_PADDING),
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

/**
 * The row's featured image at whatever size [modifier] gives it: full width for the lead row, a
 * fixed square for the rest. Shimmers while the URL is still being resolved, and takes no space at
 * all when the post has no featured image.
 */
@Composable
private fun FeaturedImage(
    imageUrl: String?,
    isImagePending: Boolean,
    modifier: Modifier = Modifier
) {
    when {
        imageUrl != null -> AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.featured_image_desc),
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
        isImagePending -> ShimmerBox(modifier = modifier)
        else -> Unit
    }
}

/**
 * The text column with the overflow button beside it, and an optional [trailing] slot between them
 * for the compact row's thumbnail. When [actions] has quick actions, the button moves down into
 * [RowFooter] instead.
 *
 * Shared by both row shapes so the button lands the same distance from the card edge on every row -
 * they drifted apart once already, which is how the hero's button ended up 14dp further in.
 */
@Composable
private fun RowTextAndMenu(
    state: ContentListRowUiState,
    titleSize: TextUnit,
    titleLineHeight: TextUnit,
    density: ContentListDensity,
    padding: Dp,
    actions: ContentListRowActions,
    leading: (@Composable () -> Unit)?,
    trailing: @Composable () -> Unit = {}
) {
    val hasFooter = actions.quickActions.isNotEmpty()
    Column {
        Row(
            modifier = Modifier.padding(
                start = padding,
                top = padding,
                // The overflow button carries its own inset, so the card supplies none on that edge;
                // without a menu beside the text the card pads itself as usual.
                end = if (actions.menu == null || hasFooter) padding else 0.dp,
                // The footer's buttons carry their own inset too.
                bottom = if (hasFooter) 0.dp else padding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(LEADING_GAP))
            }
            Column(modifier = Modifier.weight(1f)) {
                RowBody(
                    state = state,
                    titleSize = titleSize,
                    titleLineHeight = titleLineHeight,
                    density = density,
                    showMetaLine = !hasFooter
                )
            }
            trailing()
            if (!hasFooter) actions.menu?.invoke()
        }
        if (hasFooter) {
            RowFooter(
                state = state,
                density = density,
                padding = padding,
                actions = actions
            )
        }
    }
}

/**
 * The metrics, then the quick actions and the overflow button pinned to the trailing edge. A long
 * metrics line ellipsizes rather than pushing the buttons off the card.
 */
@Composable
private fun RowFooter(
    state: ContentListRowUiState,
    density: ContentListDensity,
    padding: Dp,
    actions: ContentListRowActions
) {
    Row(
        modifier = Modifier.padding(start = padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            // The date and any sync failure are under the title, so this line carries only the
            // metrics - a failure placed after them would be the first thing squeezed out.
            RowMetaLine(
                state = state,
                showDate = false,
                showMetrics = !density.isCondensed,
                showSyncFailed = false
            )
        }
        actions.quickActions.forEach { action ->
            IconButton(onClick = action.onClick) {
                Icon(
                    imageVector = action.type.icon,
                    contentDescription = stringResource(action.type.labelResId),
                    modifier = Modifier.size(QUICK_ACTION_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        actions.menu?.invoke()
    }
}

private val LIST_HORIZONTAL_PADDING = 8.dp

// Applied per card, so the gap between two adjacent cards is twice this.
private val CARD_VERTICAL_SPACING = 2.dp
private val CARD_PADDING = 14.dp

// Internal so the view models request images at the size the row draws them.
internal const val THUMBNAIL_SIZE_DP = 72
internal const val HERO_IMAGE_HEIGHT_DP = 130

private val CARD_RADIUS = 14.dp
private val CARD_BORDER_WIDTH = 1.dp
private val THUMBNAIL_SIZE = THUMBNAIL_SIZE_DP.dp
private val CONDENSED_THUMBNAIL_SIZE = 56.dp
private val CONDENSED_CARD_PADDING = 12.dp
private val THUMBNAIL_RADIUS = 10.dp
private val HERO_IMAGE_HEIGHT = HERO_IMAGE_HEIGHT_DP.dp
private val LEADING_GAP = 12.dp
private val TITLE_DATE_GAP = 2.dp
private val QUICK_ACTION_ICON_SIZE = 20.dp
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
// Sits 4dp further in than the cards, as the mockup has it - so it tracks
// LIST_HORIZONTAL_PADDING rather than being an independent value.
private val GROUP_HEADER_INSET = LIST_HORIZONTAL_PADDING + 4.dp
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
