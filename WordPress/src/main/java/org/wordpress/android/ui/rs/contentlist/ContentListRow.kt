package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.PluralsRes
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
 */
@Composable
fun ContentListRow(
    state: ContentListRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    density: ContentListDensity = ContentListDensity.COMFORTABLE,
    menu: (@Composable () -> Unit)? = null
) {
    val padding = if (density.isCondensed) CONDENSED_CARD_PADDING else CARD_PADDING
    ContentListCard(onClick = onClick, isSyncing = state.isSyncing, modifier = modifier) {
        RowTextAndMenu(
            state = state,
            titleSize = TITLE_SIZE,
            titleLineHeight = TITLE_LINE_HEIGHT,
            density = density,
            padding = padding,
            menu = menu
        ) {
            FeaturedImage(
                imageUrl = state.imageUrl,
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
    menu: (@Composable () -> Unit)? = null
) {
    ContentListCard(onClick = onClick, isSyncing = state.isSyncing, modifier = modifier) {
        Column {
            FeaturedImage(
                imageUrl = state.imageUrl,
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
                menu = menu
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

@Composable
private fun ContentListCard(
    onClick: () -> Unit,
    isSyncing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // The design leans on a hairline border rather than a shadow, so elevation stays flat.
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LIST_HORIZONTAL_PADDING, vertical = CARD_VERTICAL_SPACING),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(CARD_BORDER_WIDTH, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(CARD_RADIUS)
    ) {
        CardBody(isSyncing = isSyncing, content = content)
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
 */
@Composable
private fun RowBody(
    state: ContentListRowUiState,
    titleSize: TextUnit,
    titleLineHeight: TextUnit,
    density: ContentListDensity
) {
    RowBadges(state.badges)
    RowTitle(title = state.title, fontSize = titleSize, lineHeight = titleLineHeight)
    if (!density.isCondensed) {
        RowExcerpt(state.excerpt)
    }
    Spacer(modifier = Modifier.height(TITLE_META_GAP))
    RowMetaLine(state = state, showMetrics = !density.isCondensed)
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
    showMetrics: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MetaText(state.dateLabel)
        // A condensed list does not fetch metrics, so it shows neither them nor a skeleton
        // waiting on a request that is never made.
        if (showMetrics) {
            RowMetrics(state)
        }
        if (state.hasSyncFailed) {
            MetaSeparator()
            MetaText(
                text = stringResource(R.string.post_rs_sync_failed),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** A separator followed by one metric, e.g. "· 1,204 views". */
@Composable
private fun MetricText(@PluralsRes pluralResId: Int, count: Long) {
    MetaSeparator()
    MetaText(
        pluralStringResource(
            pluralResId,
            count.toInt(),
            NumberFormat.getIntegerInstance().format(count)
        )
    )
}

/** Views and comments, or a single bar standing in for both while they are still being fetched. */
@Composable
private fun RowMetrics(state: ContentListRowUiState) {
    if (state.areMetricsPending) {
        // Views and comments arrive together, so one bar stands in for both rather than two that
        // would resolve on the same frame anyway.
        MetaSeparator()
        ShimmerBox(
            modifier = Modifier
                .width(METRICS_SKELETON_WIDTH)
                .height(METRICS_SKELETON_HEIGHT)
                .clip(RoundedCornerShape(PLACEHOLDER_RADIUS))
        )
        return
    }
    state.viewCount?.let { MetricText(R.plurals.content_list_view_count, it) }
    // A post with no comments still says so; unlike views, zero is meaningful here and the number
    // arrives in the same response, so hiding it would look like a gap.
    state.commentCount?.let { MetricText(R.plurals.content_list_comment_count, it) }
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
 * for the compact row's thumbnail.
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
    menu: (@Composable () -> Unit)?,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.padding(
            start = padding,
            top = padding,
            // The overflow button carries its own inset, so the card supplies none on that edge;
            // without a menu the card pads itself as usual.
            end = if (menu == null) padding else 0.dp,
            bottom = padding
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            RowBody(
                state = state,
                titleSize = titleSize,
                titleLineHeight = titleLineHeight,
                density = density
            )
        }
        trailing()
        menu?.invoke()
    }
}

private val LIST_HORIZONTAL_PADDING = 8.dp

// Applied per card, so the gap between two adjacent cards is twice this.
private val CARD_VERTICAL_SPACING = 2.dp
private val CARD_PADDING = 14.dp

private val CARD_RADIUS = 14.dp
private val CARD_BORDER_WIDTH = 1.dp
private val THUMBNAIL_SIZE = 72.dp
private val CONDENSED_THUMBNAIL_SIZE = 56.dp
private val CONDENSED_CARD_PADDING = 12.dp
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
