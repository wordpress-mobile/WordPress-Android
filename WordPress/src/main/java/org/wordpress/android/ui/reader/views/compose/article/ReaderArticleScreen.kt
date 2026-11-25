package org.wordpress.android.ui.reader.views.compose.article

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.compose.postcard.CircleAvatar
import org.wordpress.android.ui.reader.views.compose.postcard.ReaderPostMoreMenuItem
import org.wordpress.android.ui.reader.views.compose.readableWidth
import java.util.Locale

private val LIGHT_HERO_HEIGHT = 330.dp
private val DARK_HERO_HEIGHT = 300.dp
private val NO_IMAGE_HERO_HEIGHT = 96.dp
private val SHEET_OVERLAP = 26.dp
private val BOTTOM_PADDING = 120.dp
private const val HERO_TOP_SCRIM_ALPHA = 0.4f
private const val DARK_HERO_TOP_SCRIM_ALPHA = 0.35f
private const val OVERLAY_BUTTON_ALPHA = 0.35f
private const val PROGRESS_TRACK_ALPHA = 0.08f
private const val COMPACT_THOUSAND = 1_000
private const val COMPACT_TEN_THOUSAND = 10_000
private const val COMPACT_MILLION = 1_000_000

/** The post the native detail shows: its body's HTML is parsed into blocks off the main thread */
data class ReaderArticlePost(
    val key: String,
    val title: String,
    val url: String,
    val html: String,
    // the featured image's own URL, which its sampled accent is saved under and the photo viewer opens
    val heroSourceUrl: String?,
    // the featured image sized for the hero
    val heroUrl: String?,
)

@Suppress("LongParameterList")
class ReaderArticleScreenActions(
    val onBack: () -> Unit,
    val onMoreClick: () -> Unit,
    val onMoreDismiss: () -> Unit,
    val onMoreItemClick: (ReaderPostCardActionType) -> Unit,
    val onUseWebRenderer: () -> Unit,
    val onShare: () -> Unit,
    val onRefresh: () -> Unit,
    val onLinkClick: (String) -> Unit,
)

/**
 * The post detail as WPFolio draws it, natively: the featured image as a hero under a rounded sheet, the site and
 * byline, the title, then the body as blocks, the detail's usual [footer], and a floating pill of actions that
 * hides as the post scrolls.
 */
@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderArticleScreen(
    post: ReaderArticlePost?,
    detailsState: ReaderPostDetailsUiState?,
    accent: Color,
    topInsetPx: Int,
    isActive: Boolean,
    isRefreshing: Boolean,
    images: ReaderArticleImages,
    actions: ReaderArticleScreenActions,
    footer: @Composable () -> Unit,
) {
    val colors = articleColors
    // a related post replacing this one in place starts at its top
    val listState = remember(post?.key) { LazyListState() }
    val density = LocalDensity.current
    val topInset = with(density) { topInsetPx.toDp() }
    val heroHeight = when {
        post?.heroUrl == null -> topInset + NO_IMAGE_HERO_HEIGHT
        colors.isDark -> DARK_HERO_HEIGHT
        else -> LIGHT_HERO_HEIGHT
    }
    val blocks by produceState<List<ReaderArticleBlock>?>(null, post?.key, post?.html) {
        value = post?.let {
            withContext(Dispatchers.Default) {
                ReaderArticleBlocks.parse(it.html, it.url).withoutHero(it.heroSourceUrl)
            }
        }
    }
    var footnote by rememberSaveable { mutableStateOf<String?>(null) }
    // links open as the WebView's do; footnote markers open their note instead
    val uriHandler = remember(actions) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val note = ReaderArticleBlocks.footnote(uri)
                if (note != null) footnote = note else actions.onLinkClick(uri)
            }
        }
    }
    val pill = remember { HideOnScrollState() }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(pill.connection)
    ) {
        CompositionLocalProvider(
            LocalUriHandler provides uriHandler,
            LocalReaderArticleImages provides images,
        ) {
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = actions.onRefresh) {
                // text can be selected to copy a passage; links (including footnotes) still open on tap
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = BOTTOM_PADDING),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "hero") { Hero(post, heroHeight, accent) }
                        item(key = "header") {
                            Box(Modifier.readableWidth()) { Header(post, detailsState, accent) }
                        }
                        val loaded = blocks
                        if (loaded == null) {
                            item(key = "loading") {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = colors.textSecondary)
                                }
                            }
                        } else {
                            items(loaded) { block ->
                                Box(Modifier.readableWidth()) { ReaderArticleBlockView(block, accent, isActive) }
                            }
                            item(key = "open_original") {
                                Box(Modifier.readableWidth()) { OpenOriginal(post, actions.onLinkClick) }
                            }
                            item(key = "footer") { Box(Modifier.readableWidth()) { footer() } }
                        }
                    }
                }
            }
        }
        ProgressBar(listState, heroHeight, topInset, accent)
        TopButtons(topInset, detailsState, actions)
        AnimatedVisibility(
            visible = pill.isVisible && detailsState != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            detailsState?.let { ActionPill(it, actions.onShare) }
        }
        footnote?.let { ReaderArticleFootnoteSheet(it, onDismiss = { footnote = null }) }
    }
}

/** Drops the featured image from the body when the post repeats it near the top, as many do */
private fun List<ReaderArticleBlock>.withoutHero(hero: String?): List<ReaderArticleBlock> {
    val heroPath = hero?.substringBefore('?') ?: return this
    val index = take(HERO_DUPLICATE_SEARCH).indexOfFirst {
        it is ReaderArticleBlock.Image && it.url.substringBefore('?') == heroPath
    }
    return if (index >= 0) filterIndexed { i, _ -> i != index } else this
}

private const val HERO_DUPLICATE_SEARCH = 3

@Composable
private fun Hero(post: ReaderArticlePost?, height: Dp, accent: Color) {
    val colors = articleColors
    val images = LocalReaderArticleImages.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(accent)
    ) {
        post?.heroUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { images.onClick(post.heroSourceUrl ?: url) },
            )
        }
        if (colors.isDark) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = DARK_HERO_TOP_SCRIM_ALPHA),
                            0.3f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1f to colors.background,
                        )
                    )
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = HERO_TOP_SCRIM_ALPHA),
                            0.3f to Color.Transparent
                        )
                    )
            )
            // the text sheet's rounded top, overlapping the image
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(SHEET_OVERLAP)
                    .clip(RoundedCornerShape(topStart = SHEET_OVERLAP, topEnd = SHEET_OVERLAP))
                    .background(colors.background)
            )
        }
    }
}

@Composable
private fun Header(post: ReaderArticlePost?, detailsState: ReaderPostDetailsUiState?, accent: Color) {
    val colors = articleColors
    val header = detailsState?.headerUiState
    val blogSection = header?.blogSectionUiState
    val blogName = blogSection?.let { uiStringText(it.blogName) }.orEmpty()
    val readingTime = header?.readingTime?.let { uiStringText(it) }
    val onSiteClick = blogSection?.blogSectionClickData?.onBlogSectionClicked
    val siteModifier = if (onSiteClick != null) Modifier.clickable(onClick = onSiteClick) else Modifier
    Column(
        Modifier.padding(
            start = ReaderArticleTextPadding,
            end = ReaderArticleTextPadding,
            top = if (colors.isDark) 14.dp else 4.dp,
            bottom = 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (colors.isDark) {
            Text(
                listOfNotNull(blogName.ifEmpty { null }, readingTime).joinToString(" · ")
                    .uppercase(Locale.getDefault()),
                style = ui(12f, 600, tracking = 0.06f),
                color = colors.bodyMuted,
                modifier = siteModifier,
            )
        } else if (blogSection != null) {
            Row(
                siteModifier,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val avatar = blogSection.avatarOrBlavatarUrl
                if (avatar != null) {
                    CircleAvatar(url = avatar, imageType = blogSection.blavatarType, size = 30.dp)
                } else {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
                Column {
                    Text(blogName, style = ui(13.5f, 600), color = colors.text)
                    val byline = listOfNotNull(header.authorName?.ifEmpty { null }, readingTime).joinToString(" · ")
                    if (byline.isNotEmpty()) Text(byline, style = ui(12f, 500), color = colors.textSecondary)
                }
            }
        }
        val title = post?.title?.ifEmpty { null } ?: header?.title?.let { uiStringText(it) }
        title?.let {
            Text(it, style = ui(27f, 600, lineHeight = 1.14f, tracking = -0.02f), color = colors.text)
        }
    }
}

@Composable
private fun OpenOriginal(post: ReaderArticlePost?, onClick: (String) -> Unit) {
    val colors = articleColors
    val url = post?.url?.takeIf { it.isNotBlank() } ?: return
    Box(
        Modifier
            .fillMaxWidth()
            .padding(ReaderArticleTextPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.reader_article_open_original),
            style = ui(14f, 600),
            color = colors.text,
            modifier = Modifier
                .clip(CircleShape)
                .border(1.5.dp, colors.text, CircleShape)
                .clickable { onClick(url) }
                .padding(horizontal = 22.dp, vertical = 12.dp)
        )
    }
}

/** The accent progress bar starts at the top of the text sheet and sticks under the status bar */
@Composable
private fun ProgressBar(listState: LazyListState, heroHeight: Dp, topInset: Dp, accent: Color) {
    val colors = articleColors
    val density = LocalDensity.current
    val heroPx = with(density) { heroHeight.roundToPx() }
    val topPx = with(density) { topInset.roundToPx() }
    val overlapPx = with(density) { if (colors.isDark) 0 else SHEET_OVERLAP.roundToPx() }
    val progress by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            when {
                total == 0 -> 0f
                !listState.canScrollForward -> 1f
                else -> {
                    val first = info.visibleItemsInfo.firstOrNull()
                    val fraction = if (first != null && first.size > 0) -first.offset.toFloat() / first.size else 0f
                    ((listState.firstVisibleItemIndex + fraction) / total).coerceIn(0f, 1f)
                }
            }
        }
    }
    Box(
        Modifier
            .offset {
                val scrolled = if (listState.firstVisibleItemIndex == 0) {
                    listState.firstVisibleItemScrollOffset
                } else {
                    Int.MAX_VALUE / 2
                }
                IntOffset(0, (heroPx - overlapPx - scrolled).coerceAtLeast(topPx))
            }
            .padding(horizontal = 26.dp)
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(colors.text.copy(alpha = PROGRESS_TRACK_ALPHA))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(CircleShape)
                .background(accent)
        )
    }
}

@Composable
private fun TopButtons(topInset: Dp, detailsState: ReaderPostDetailsUiState?, actions: ReaderArticleScreenActions) {
    Row(
        Modifier
            .padding(start = 14.dp, end = 14.dp, top = topInset + 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OverlayButton(
            Icons.AutoMirrored.Rounded.ArrowBack,
            stringResource(R.string.navigate_up_desc),
            onClick = actions.onBack
        )
        Box {
            OverlayButton(
                Icons.Rounded.MoreVert,
                stringResource(R.string.show_more_desc),
                onClick = actions.onMoreClick
            )
            // the view model fills in the items when More is tapped, and clears them to close the menu
            val items = detailsState?.moreMenuItems
            DropdownMenu(expanded = items != null, onDismissRequest = actions.onMoreDismiss) {
                items?.forEach { item -> ReaderPostMoreMenuItem(item) { actions.onMoreItemClick(it.type) } }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_article_use_web_renderer)) },
                    onClick = {
                        actions.onMoreDismiss()
                        actions.onUseWebRenderer()
                    },
                )
            }
        }
    }
}

/** A round translucent button drawn over the hero */
@Composable
private fun OverlayButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = OVERLAY_BUTTON_ALPHA))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** Save, like, comments and share, floating over the post */
@Composable
private fun ActionPill(state: ReaderPostDetailsUiState, onShare: () -> Unit) {
    val colors = articleColors
    val postActions = state.actions
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .clip(CircleShape)
            .background(colors.pill)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PillAction(state, postActions.bookmarkAction, Icons.Rounded.BookmarkBorder, Icons.Rounded.Bookmark)
        PillAction(state, postActions.likeAction, Icons.Rounded.FavoriteBorder, Icons.Rounded.Favorite)
        PillAction(
            state,
            postActions.commentsAction,
            Icons.AutoMirrored.Rounded.Comment,
            Icons.AutoMirrored.Rounded.Comment
        )
        PillButton(Icons.Rounded.Share, stringResource(R.string.share_desc), count = null, onClick = onShare)
    }
}

@Composable
private fun PillAction(
    state: ReaderPostDetailsUiState,
    action: PrimaryAction,
    icon: ImageVector,
    selectedIcon: ImageVector,
) {
    // a count still shows for an action that can't be used, such as likes for someone signed out
    if (!action.isEnabled && action.count == 0) return
    PillButton(
        icon = if (action.isSelected) selectedIcon else icon,
        label = action.contentDescription?.let { uiStringText(it) }.orEmpty(),
        count = action.count,
        isEnabled = action.isEnabled,
        onClick = { action.onClicked?.invoke(state.postId, state.blogId, action.type) },
    )
}

@Composable
private fun PillButton(
    icon: ImageVector,
    label: String,
    count: Int?,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shown = count?.takeIf { it > 0 }
    Row(
        Modifier
            .height(44.dp)
            .widthIn(min = 44.dp)
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (shown != null) 12.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        if (shown != null) Text(compactCount(shown), style = ui(13f, 600), color = Color.White)
    }
}

/** "7", "1.2k", "18k", "1.2M": short enough to sit beside an icon */
private fun compactCount(n: Int): String = when {
    n < COMPACT_THOUSAND -> "$n"
    n < COMPACT_TEN_THOUSAND -> "%.1fk".format(Locale.US, n / COMPACT_THOUSAND.toDouble()).replace(".0k", "k")
    n < COMPACT_MILLION -> "${n / COMPACT_THOUSAND}k"
    else -> "%.1fM".format(Locale.US, n / COMPACT_MILLION.toDouble()).replace(".0M", "M")
}

/**
 * Hides the pill as the post scrolls down and shows it as it scrolls up. It reacts to the scroll the list consumed,
 * so overscroll at either end doesn't toggle it.
 */
private class HideOnScrollState {
    var isVisible by mutableStateOf(true)
        private set

    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (consumed.y < -SCROLL_THRESHOLD) isVisible = false
            if (consumed.y > SCROLL_THRESHOLD) isVisible = true
            return Offset.Zero
        }
    }
}

private const val SCROLL_THRESHOLD = 1f
