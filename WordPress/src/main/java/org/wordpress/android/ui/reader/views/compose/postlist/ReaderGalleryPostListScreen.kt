package org.wordpress.android.ui.reader.views.compose.postlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.accent.ReaderImageAccentSampler
import org.wordpress.android.ui.reader.accent.readerImageMeta
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListItem
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListUiState
import org.wordpress.android.ui.reader.views.compose.detail.MEDIUM_EMPHASIS_ALPHA
import org.wordpress.android.ui.reader.views.compose.postcard.ReaderPostMoreMenuItem
import org.wordpress.android.ui.reader.views.compose.postcard.readerPostTitleColor
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem

private val TILE_MIN_WIDTH = 240.dp
private const val MIN_COLUMNS = 2
private const val MAX_COLUMNS = 5
private val GRID_SPACING = 12.dp
private val HERO_HEIGHT = 420.dp
private val TILE_CORNER = 14.dp

// tiles are kept between 4:5 and square, as in WPFolio; until an image is sampled they alternate
private const val MIN_TILE_RATIO = 0.8f
private const val MAX_TILE_RATIO = 1f
private const val TILE_PATTERN_SIZE = 4
private const val TITLE_MAX_LINES = 3
private const val HERO_EXCERPT_MAX_LINES = 3
private const val QUOTE_MAX_LINES = 9
private const val MOSAIC_IMAGE_COUNT = 3
/** The height of a blog preview's cover below the status bar, as WPFolio's */
internal val ReaderGalleryCoverHeight = 220.dp

// about the height of a site's header below its cover, held while it loads
private val HEADER_PLACEHOLDER_HEIGHT = 240.dp
private val MOSAIC_GAP = 2.dp
private const val MOSAIC_LEAD_WEIGHT = 1.4f

/**
 * The Reader post list as a gallery, ported from WPFolio: the first post with an image as a large hero, the rest as
 * tiles in a staggered grid at their images' own shapes, and posts without an image as cards in their accent color.
 * It shows the same rows as [ReaderPostListScreen], from the same state.
 */
@Composable
fun ReaderGalleryPostListScreen(
    uiState: ReaderPostListUiState,
    gridState: LazyStaggeredGridState,
    actions: ReaderPostListActions,
    canLoadMore: Boolean = true,
    // a blog preview, whose site header (with its cover) loads after its posts
    isSiteHeaderExpected: Boolean = false,
) {
    ContentListPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = actions.onRefresh,
        enabled = uiState.canPullToRefresh,
        modifier = Modifier
            .nestedScroll(rememberNestedScrollInteropConnection())
            .background(MaterialTheme.colorScheme.surface)
    ) {
        GalleryGrid(uiState, gridState, actions, canLoadMore, isSiteHeaderExpected)
        NewPostsBar(
            isVisible = uiState.newPostsBarVisible,
            listState = gridState,
            onClick = {
                gridState.requestScrollToItem(0)
                actions.onNewPostsBarClicked()
            },
            onScrolledAway = actions.onNewPostsBarScrolledAway,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun GalleryGrid(
    uiState: ReaderPostListUiState,
    gridState: LazyStaggeredGridState,
    actions: ReaderPostListActions,
    canLoadMore: Boolean,
    isSiteHeaderExpected: Boolean,
) {
    val allItems = uiState.items
    val emptyState = uiState.emptyState
    // a blog preview's posts usually load before its header, which would then land above them and push them
    // down; until it arrives, its place holds a cover made from the posts, and the posts wait
    val isWaitingForHeader = isSiteHeaderExpected && emptyState == null &&
        allItems.none { it is ReaderPostListItem.SiteHeader }
    val items = if (isWaitingForHeader) emptyList() else allItems
    val hasPosts = items.any { it is ReaderPostListItem.Post || it is ReaderPostListItem.XPost }
    // the first post with an image leads as the hero
    val heroKey = remember(items) {
        items.firstOrNull { it is ReaderPostListItem.Post && it.uiState.featuredImageSourceUrl != null }?.key
    }
    val mosaicImages = remember(allItems) {
        allItems.filterIsInstance<ReaderPostListItem.Post>()
            .mapNotNull { it.uiState.featuredImageTileUrl }
            .take(MOSAIC_IMAGE_COUNT)
    }

    LazyVerticalStaggeredGrid(
        columns = GalleryColumns,
        state = gridState,
        contentPadding = PaddingValues(GRID_SPACING),
        horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
        verticalItemSpacing = GRID_SPACING,
        modifier = Modifier.fillMaxSize()
    ) {
        if (isWaitingForHeader) {
            // keyed as the header, so the real one replaces it in place
            item(key = ReaderPostListItem.KEY_HEADER, span = StaggeredGridItemSpan.FullLine) {
                Column {
                    CoverMosaic(mosaicImages)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(HEADER_PLACEHOLDER_HEIGHT),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        items(
            items = items,
            key = { it.key },
            contentType = { if (it.key == heroKey) "hero" else it::class },
            span = { item ->
                if (item is ReaderPostListItem.Post && item.key != heroKey) {
                    StaggeredGridItemSpan.SingleLane
                } else {
                    StaggeredGridItemSpan.FullLine
                }
            }
        ) { item ->
            GalleryRow(item, isHero = item.key == heroKey, mosaicImages, actions)
        }
        if (emptyState != null) {
            item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                ReaderPostListEmptyState(
                    emptyState = emptyState,
                    onButtonClick = actions.onEmptyStateButtonClicked,
                    isTopAligned = emptyState.isTopAligned(hasItems = items.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (uiState.isLoadingMore) {
            contentListLoadingMoreItem()
        }
    }

    LoadMoreOnScrollToEnd(
        gridState = gridState,
        itemCount = items.size,
        canLoadMore = hasPosts && canLoadMore,
        onLoadMore = actions.onLoadMore
    )
    KeepTopInViewOnPrepend(
        firstKey = if (isWaitingForHeader) ReaderPostListItem.KEY_HEADER else items.firstOrNull()?.key,
        scrollToPosition = uiState.scrollToPosition,
        isAtTop = { gridState.firstVisibleItemIndex == 0 },
        scrollToTop = { gridState.requestScrollToItem(0) },
    )
    PendingScroll(uiState.scrollToPosition, actions.onScrollHandled) { gridState.requestScrollToItem(it) }
    ReportFirstItemVisibility(gridState, { gridState.firstVisibleItemIndex == 0 }, actions.onFirstItemVisibilityChanged)
}

@Composable
private fun GalleryRow(
    item: ReaderPostListItem,
    isHero: Boolean,
    mosaicImages: List<String>,
    actions: ReaderPostListActions,
) {
    when (item) {
        is ReaderPostListItem.Post -> {
            val uiState = item.uiState
            LaunchedEffect(item.key) { uiState.onItemRendered(uiState) }
            when {
                isHero -> HeroTile(uiState)
                uiState.featuredImageSourceUrl != null -> ImageTile(uiState)
                else -> QuoteTile(uiState)
            }
        }

        is ReaderPostListItem.SiteHeader -> Column {
            // shown even without images, since the preview's buttons sit over it
            CoverMosaic(mosaicImages)
            ReaderSiteHeader(
                info = item.info,
                onFollowClick = actions.onSiteHeaderFollowClicked,
                onSettingsClick = actions.onSiteHeaderSettingsClicked
            )
        }

        is ReaderPostListItem.TagHeader -> ReaderTagHeader(item, actions.onTagHeaderFollowClicked)
        is ReaderPostListItem.GapMarker -> ReaderGapMarker(item.isFilling, actions.onGapMarkerClicked)
        is ReaderPostListItem.XPost -> item.post.let { post -> ReaderXPostRow(post) { actions.onXPostClicked(post) } }
        is ReaderPostListItem.RemovedPost -> item.post.let { post ->
            ReaderRemovedPostRow(post) { actions.onRemovedPostClicked(post) }
        }
    }
}

/**
 * The lead post: its image tall and full width, with the site, title and excerpt over a scrim
 */
@Composable
private fun HeroTile(uiState: ReaderPostUiState) {
    val meta = readerImageMeta(uiState.featuredImageSourceUrl)
    val accent = accentColor(uiState, meta?.accent)
    TileMenuBox(uiState) { modifier ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT)
                .clip(RoundedCornerShape(TILE_CORNER))
                .background(accent)
        ) {
            TileImage(uiState.featuredImageUncroppedUrl, Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.35f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                SiteLine(uiState, color = Color.White.copy(alpha = 0.85f))
                uiState.title?.let {
                    Text(
                        text = uiStringText(it),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = TITLE_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                uiState.excerpt?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = HERO_EXCERPT_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * A post with an image: the image at its own shape (within 4:5 and square), then the title and site
 */
@Composable
private fun ImageTile(uiState: ReaderPostUiState) {
    val meta = readerImageMeta(uiState.featuredImageSourceUrl)
    // fixed once the tile first shows, so a late sample doesn't resize it on screen
    val ratio = remember(uiState.postId, uiState.blogId) {
        meta?.ratio?.coerceIn(MIN_TILE_RATIO, MAX_TILE_RATIO) ?: placeholderRatio(uiState)
    }
    val accent = accentColor(uiState, meta?.accent)
    TileMenuBox(uiState) { modifier ->
        Column(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(TILE_CORNER))
                    .background(accent)
            ) {
                TileImage(uiState.featuredImageTileUrl, Modifier.fillMaxSize())
            }
            uiState.title?.let {
                Text(
                    text = uiStringText(it),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = readerPostTitleColor(uiState.isSeen),
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SiteLine(
                uiState,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * A post without an image: its words in white on its accent color
 */
@Composable
private fun QuoteTile(uiState: ReaderPostUiState) {
    val accent = accentColor(uiState, null)
    val excerpt = uiState.excerpt?.takeIf { it.isNotBlank() }
    TileMenuBox(uiState) { modifier ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(TILE_CORNER))
                .background(accent)
                .padding(16.dp)
        ) {
            Text(
                text = excerpt ?: uiState.title?.let { uiStringText(it) }.orEmpty(),
                modifier = Modifier.weight(1f),
                fontSize = if (excerpt != null) 15.sp else 19.sp,
                lineHeight = if (excerpt != null) 21.sp else 25.sp,
                fontWeight = if (excerpt != null) FontWeight.Normal else FontWeight.SemiBold,
                color = Color.White.copy(alpha = if (uiState.isSeen) MEDIUM_EMPHASIS_ALPHA else 1f),
                maxLines = QUOTE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            SiteLine(uiState, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun SiteLine(uiState: ReaderPostUiState, color: Color, modifier: Modifier = Modifier) {
    val blogName = uiStringText(uiState.blogSection.blogName)
    Text(
        text = "$blogName · ${uiState.blogSection.dateLine}",
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TileImage(url: String?, modifier: Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/**
 * Taps open the post; a long press opens its more menu, which the card shows from its button
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileMenuBox(uiState: ReaderPostUiState, content: @Composable (Modifier) -> Unit) {
    Box {
        content(
            Modifier.combinedClickable(
                onClick = { uiState.onItemClicked(uiState.postId, uiState.blogId) },
                onLongClick = if (uiState.moreMenuVisibility) {
                    { uiState.onMoreButtonClicked(uiState) }
                } else {
                    null
                },
            )
        )
        val menuItems = uiState.moreMenuItems
        DropdownMenu(expanded = menuItems != null, onDismissRequest = { uiState.onMoreDismissed(uiState) }) {
            menuItems?.forEach { menuItem ->
                ReaderPostMoreMenuItem(menuItem) { it.onClicked(uiState.postId, uiState.blogId, it.type) }
            }
        }
    }
}

/**
 * The three latest images above a site's header, in site previews, edge to edge as WPFolio's cover is: the
 * newest large on the start side, the next two stacked beside it
 */
@Composable
private fun CoverMosaic(images: List<String>) {
    val placeholder = colorResource(R.color.placeholder)
    // the preview is edge to edge, so the cover also fills the space under the status bar
    val density = LocalDensity.current
    val view = LocalView.current
    val statusBarPx = WindowInsets.statusBars.getTop(density).takeIf { it > 0 }
        ?: ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.statusBars())?.top
        ?: 0
    @Composable
    fun Tile(index: Int, modifier: Modifier) {
        TileImage(images.getOrNull(index), modifier.background(placeholder))
    }
    Box(
        Modifier
            // past the grid's padding, to the screen's edges and the top bar
            .bleed(horizontal = GRID_SPACING, top = GRID_SPACING)
            .fillMaxWidth()
            .height(ReaderGalleryCoverHeight + with(density) { statusBarPx.toDp() })
            .background(placeholder)
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MOSAIC_GAP)) {
            Tile(0, Modifier.weight(MOSAIC_LEAD_WEIGHT).fillMaxHeight())
            if (images.size > 1) {
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(MOSAIC_GAP)
                ) {
                    Tile(1, Modifier.weight(1f).fillMaxWidth())
                    if (images.size > 2) Tile(2, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

/** Lets an item ignore the grid's padding: [horizontal] on both sides, and [top] above it */
private fun Modifier.bleed(horizontal: Dp, top: Dp) = layout { measurable, constraints ->
    val extraWidth = (horizontal * 2).roundToPx()
    val extraTop = top.roundToPx()
    val width = constraints.maxWidth + extraWidth
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, (placeable.height - extraTop).coerceAtLeast(0)) {
        placeable.place(-extraWidth / 2, -extraTop)
    }
}

/** As many columns as fit at [TILE_MIN_WIDTH] each, but never fewer than two or more than five */
private object GalleryColumns : StaggeredGridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): IntArray {
        val count = (availableSize / TILE_MIN_WIDTH.roundToPx()).coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val cellSize = (availableSize - spacing * (count - 1)) / count
        val remainder = (availableSize - spacing * (count - 1)) % count
        return IntArray(count) { index -> cellSize + if (index < remainder) 1 else 0 }
    }
}

private fun accentColor(uiState: ReaderPostUiState, sampled: Int?) =
    Color(sampled ?: ReaderImageAccentSampler.fallbackAccent("${uiState.blogId}:${uiState.postId}"))

// alternates square and 4:5 tiles until the image's own shape is known, so the grid still staggers
private fun placeholderRatio(uiState: ReaderPostUiState): Float {
    val index = Math.floorMod(uiState.postId, TILE_PATTERN_SIZE.toLong()).toInt()
    return if (index == 0 || index == TILE_PATTERN_SIZE - 1) MIN_TILE_RATIO else MAX_TILE_RATIO
}
