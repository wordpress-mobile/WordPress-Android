package org.wordpress.android.ui.reader.views.compose.postlist

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
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
import org.wordpress.android.ui.compose.components.LoadingState
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.accent.ReaderImageAccentSampler
import org.wordpress.android.ui.reader.accent.readerImageMeta
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListItem
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListUiState
import org.wordpress.android.ui.reader.views.compose.detail.MEDIUM_EMPHASIS_ALPHA
import org.wordpress.android.ui.reader.views.compose.postcard.ReaderPostMoreMenuItem
import org.wordpress.android.ui.reader.views.compose.postcard.countText
import org.wordpress.android.ui.reader.views.compose.postcard.readerPostTitleColor
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem
import org.wordpress.android.util.PhotonUtils

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
private const val EXCERPT_ALPHA = 0.85f
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
                    LoadingState(Modifier.height(HEADER_PLACEHOLDER_HEIGHT))
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
                uiState.featuredImageSourceUrl != null || uiState.firstStripImage() != null -> ImageTile(uiState)
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
            modifier = modifier.modifier
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
                SiteRow(uiState, color = Color.White.copy(alpha = 0.85f), onMoreClick = modifier.onMoreClick)
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
        Column(modifier = modifier.modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(TILE_CORNER))
                    .background(accent)
            ) {
                // a gallery post has no featured image, so it shows the first of its gallery's
                TileImage(uiState.featuredImageTileUrl ?: uiState.firstStripImage(), Modifier.fillMaxSize())
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
            SiteRow(
                uiState,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onMoreClick = modifier.onMoreClick,
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
    val textColor = Color.White.copy(alpha = if (uiState.isSeen) MEDIUM_EMPHASIS_ALPHA else 1f)
    TileMenuBox(uiState) { modifier ->
        Column(
            modifier = modifier.modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(TILE_CORNER))
                .background(accent)
                .padding(16.dp)
        ) {
            // the title, then as much of the excerpt as fits
            Column(Modifier.weight(1f)) {
                uiState.title?.let {
                    Text(
                        text = uiStringText(it),
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = TITLE_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                excerpt?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = textColor.copy(alpha = textColor.alpha * EXCERPT_ALPHA),
                        maxLines = QUOTE_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SiteRow(
                uiState,
                color = Color.White.copy(alpha = 0.75f),
                onMoreClick = modifier.onMoreClick,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * The site and date, which open the site as the card's blog section does, and the tile's "⋯" menu button
 */
@Composable
private fun SiteRow(uiState: ReaderPostUiState, color: Color, onMoreClick: () -> Unit, modifier: Modifier = Modifier) {
    val blogSection = uiState.blogSection
    val onSiteClick = blogSection.onClicked
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${uiStringText(blogSection.blogName)} · ${blogSection.dateLine}",
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onSiteClick != null) {
                        Modifier.clickable { onSiteClick(blogSection.postId, blogSection.blogId) }
                    } else {
                        Modifier
                    }
                ),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.ic_more_ellipsis_horizontal_squares),
            contentDescription = stringResource(R.string.show_more_desc),
            tint = color,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(20.dp)
                .clickable(
                    interactionSource = null,
                    indication = ripple(bounded = false),
                    role = Role.Button,
                    onClick = onMoreClick,
                ),
        )
    }
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

/** What a tile's content needs from its [TileMenuBox]: the clickable modifier, and how to open the menu */
private class TileScope(val modifier: Modifier, val onMoreClick: () -> Unit)

/**
 * Taps open the post. The tile's "⋯" button, or a long press, opens a short menu: save, like (with its count) and
 * share, so a tile needs no action row of its own. Screen readers get save and like directly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileMenuBox(uiState: ReaderPostUiState, content: @Composable (TileScope) -> Unit) {
    var isMenuOpen by remember(uiState.postId, uiState.blogId) { mutableStateOf(false) }
    val primaryActions = uiState.tilePrimaryActions()
    val openMenu = {
        isMenuOpen = true
        // the view model fills in the more menu's items, as it does for the card
        if (uiState.moreMenuVisibility) uiState.onMoreButtonClicked(uiState)
    }
    val closeMenu = {
        isMenuOpen = false
        if (uiState.moreMenuItems != null) uiState.onMoreDismissed(uiState)
    }
    val moreLabel = stringResource(R.string.show_more_desc)
    val actionLabels = primaryActions.map { primaryActionLabel(it) }
    Box {
        content(
            TileScope(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { uiState.onItemClicked(uiState.postId, uiState.blogId) },
                        onLongClick = openMenu,
                    )
                    .semantics {
                        customActions = primaryActions.mapIndexed { index, action ->
                            CustomAccessibilityAction(actionLabels[index]) {
                                action.onClicked?.invoke(uiState.postId, uiState.blogId, action.type)
                                true
                            }
                        } + CustomAccessibilityAction(moreLabel) {
                            openMenu()
                            true
                        }
                    },
                onMoreClick = openMenu,
            )
        )
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = closeMenu) {
            TileMenuActions(uiState, primaryActions, onActionClick = closeMenu)
            // only Share from the card's more menu, which the view model fills in when the menu opens
            uiState.moreMenuItems
                ?.filter { it is SecondaryAction && it.type == ReaderPostCardActionType.SHARE }
                ?.forEach { menuItem ->
                    ReaderPostMoreMenuItem(menuItem) {
                        isMenuOpen = false
                        it.onClicked(uiState.postId, uiState.blogId, it.type)
                    }
                }
        }
    }
}

/** Save and like, from the card's action row, with the like count */
@Composable
private fun TileMenuActions(
    uiState: ReaderPostUiState,
    actions: List<PrimaryAction>,
    onActionClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val countFormat = remember(locale) {
        CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
    }
    actions.forEach { action ->
        val tint = if (action.isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        DropdownMenuItem(
            text = { Text(primaryActionLabel(action)) },
            leadingIcon = {
                Icon(painterResource(primaryActionIcon(action)), contentDescription = null, tint = tint)
            },
            trailingIcon = countText(action, countFormat)?.takeIf { action.count > 0 }?.let { count ->
                { Text(count, style = MaterialTheme.typography.labelMedium) }
            },
            onClick = {
                onActionClick()
                action.onClicked?.invoke(uiState.postId, uiState.blogId, action.type)
            },
        )
    }
}

/** The card's actions a tile offers, save and like, when they can be used */
private fun ReaderPostUiState.tilePrimaryActions(): List<PrimaryAction> =
    listOf(bookmarkAction, likeAction).filter { it.isEnabled && it.onClicked != null }

@Composable
private fun primaryActionLabel(action: PrimaryAction): String = stringResource(
    when (action.type) {
        ReaderPostCardActionType.BOOKMARK -> {
            if (action.isSelected) R.string.reader_secondary_bookmarked else R.string.save
        }
        else -> if (action.isSelected) R.string.mnu_comment_liked else R.string.like
    }
)

private fun primaryActionIcon(action: PrimaryAction): Int = when (action.type) {
    ReaderPostCardActionType.BOOKMARK -> if (action.isSelected) {
        R.drawable.ic_bookmark_fill_new_24dp
    } else {
        R.drawable.ic_bookmark_outline_new_24dp
    }
    else -> if (action.isSelected) {
        R.drawable.ic_like_fill_new_24dp
    } else {
        R.drawable.ic_like_outline_new_24dp
    }
}

/** The first image of a gallery post's strip, at tile width, for the tile to show */
@Composable
private fun ReaderPostUiState.firstStripImage(): String? {
    val image = thumbnailStripSection?.images?.firstOrNull() ?: return null
    // the window's width, which is the screen's unless the app shares it
    val widthPx = LocalWindowInfo.current.containerSize.width / 2
    return remember(image, widthPx) { PhotonUtils.getPhotonImageUrl(image, widthPx, 0) }
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
    Color(sampled ?: ReaderImageAccentSampler.fallbackAccent(uiState.blogId, uiState.postId))

// alternates square and 4:5 tiles until the image's own shape is known, so the grid still staggers
private fun placeholderRatio(uiState: ReaderPostUiState): Float {
    val index = Math.floorMod(uiState.postId, TILE_PATTERN_SIZE.toLong()).toInt()
    return if (index == 0 || index == TILE_PATTERN_SIZE - 1) MIN_TILE_RATIO else MAX_TILE_RATIO
}
