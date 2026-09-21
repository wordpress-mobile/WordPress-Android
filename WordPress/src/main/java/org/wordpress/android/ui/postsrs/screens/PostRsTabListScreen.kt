package org.wordpress.android.ui.postsrs.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.rs.contentlist.ContentDateGroup
import org.wordpress.android.ui.rs.contentlist.ContentListDensity
import org.wordpress.android.ui.rs.contentlist.ContentDateGrouper
import org.wordpress.android.ui.rs.contentlist.ContentListGroupHeader
import org.wordpress.android.ui.rs.contentlist.ContentListHeroRow
import org.wordpress.android.ui.rs.contentlist.ContentListPlaceholderRow
import org.wordpress.android.ui.rs.contentlist.ContentListRow
import org.wordpress.android.ui.postsrs.PostRsMenuAction
import org.wordpress.android.ui.postsrs.PostRsUiModel
import org.wordpress.android.ui.postsrs.PostDisplayState
import org.wordpress.android.ui.postsrs.PostTabUiState
import org.wordpress.android.ui.postsrs.toContentListRowUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsTabListScreen(
    state: PostTabUiState,
    emptyMessageResId: Int,
    revealPostId: Long?,
    onRevealHandled: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (Long) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
    onCreatePost: () -> Unit,
    onRowsVisible: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    isSearchIdle: Boolean = false,
    isSearching: Boolean = false,
    isRedesignEnabled: Boolean = false,
    showDateGroups: Boolean = true,
    density: ContentListDensity = ContentListDensity.COMFORTABLE
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = state.isRefreshing,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        when {
            isSearchIdle -> Box(Modifier.fillMaxSize())
            state.isLoading -> ShimmerList(isRedesignEnabled)
            state.error != null && state.posts.isEmpty() -> FadeInOnAppear {
                ErrorContent(
                    error = state.error,
                    onRetry = if (state.isAuthError) null else onRefresh
                )
            }
            state.posts.isEmpty() && !state.isRefreshing -> FadeInOnAppear {
                EmptyContent(
                    emptyMessageResId = if (isSearching) {
                        R.string
                            .post_list_search_nothing_found
                    } else {
                        emptyMessageResId
                    },
                    onCreatePost = if (isSearching) {
                        null
                    } else {
                        onCreatePost
                    }
                )
            }
            else -> PostListContent(
                posts = state.posts,
                revealPostId = revealPostId,
                onRevealHandled = onRevealHandled,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                onLoadMore = onLoadMore,
                onPostClick = onPostClick,
                onPostMenuAction = onPostMenuAction,
                onRowsVisible = onRowsVisible,
                isRedesignEnabled = isRedesignEnabled,
                // Date buckets are computed against "now", so a list of future-dated posts would
                // land under "This week" wholesale. The Scheduled tab opts out instead.
                showDateGroups = showDateGroups && !isSearching,
                density = density
            )
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun PostListContent(
    posts: List<PostRsUiModel>,
    revealPostId: Long?,
    onRevealHandled: () -> Unit,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onPostClick: (Long) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
    onRowsVisible: (List<Long>) -> Unit,
    isRedesignEnabled: Boolean,
    showDateGroups: Boolean,
    density: ContentListDensity
) {
    val listState = rememberLazyListState()

    val entries = remember(posts, isRedesignEnabled, showDateGroups, density) {
        if (isRedesignEnabled) {
            buildEntries(posts, showDateGroups, density)
        } else {
            posts.map { PostListEntry.NonContent(it) }
        }
    }
    val currentEntries by rememberUpdatedState(entries)

    // Scrolls to a post the user just saved, once the refresh carrying it lands - until then this
    // list either isn't composed or doesn't contain it yet. requestScrollToItem applies at the next
    // measurement rather than to the content currently laid out, which matters because a keyed
    // LazyColumn re-anchors on its old first item when one is prepended: a plain scrollToItem here
    // would leave a newly published post just above the viewport.
    LaunchedEffect(revealPostId) {
        if (revealPostId == null) return@LaunchedEffect
        // Indexes the rendered entries rather than the posts: group headers are list items too, so
        // a post's position in `posts` is not its position in the LazyColumn.
        val index = withTimeoutOrNull(REVEAL_TIMEOUT_MS) {
            snapshotFlow { currentEntries.indexOfFirst { it.postId == revealPostId } }
                .first { it >= 0 }
        }
        if (index != null) listState.requestScrollToItem(index)
        // Disarm either way. A refresh replaces the list with page 1 only, so a post that sorts
        // beyond it never arrives here; leaving the request armed would fire it much later, when
        // load-more finally paged the post in and the user was reading something else.
        onRevealHandled()
    }

    // Per-post metrics are one request each, so the ViewModel is told which rows are actually on
    // screen rather than fetching for the whole loaded page. Post entries key on their remote id;
    // group headers key on a String, so filtering by type drops them.
    val currentOnRowsVisible by rememberUpdatedState(onRowsVisible)
    LaunchedEffect(listState, isRedesignEnabled) {
        if (!isRedesignEnabled) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Long }
        }
            // A fling changes the visible set on nearly every frame. Without settling first, each
            // of those emissions would start fetching for rows already gone from the screen.
            .debounce(VISIBLE_ROWS_DEBOUNCE_MS)
            .distinctUntilChanged()
            .collect { currentOnRowsVisible(it) }
    }

    LaunchedEffect(canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo
                .visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - LOAD_MORE_THRESHOLD
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad) onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = entries,
            key = { it.key },
            contentType = { it::class }
        ) { entry ->
            when (entry) {
                is PostListEntry.Header -> ContentListGroupHeader(
                    group = entry.group,
                    modifier = Modifier.animateItem()
                )
                is PostListEntry.NonContent ->
                    // The redesigned placeholder only belongs to the redesigned list; with the flag
                    // off every row, placeholder included, goes through the pre-redesign item.
                    if (isRedesignEnabled &&
                        entry.post.displayState == PostDisplayState.PLACEHOLDER
                    ) {
                        ContentListPlaceholderRow(modifier = Modifier.animateItem())
                    } else {
                        // The pre-redesign row still owns the error presentation.
                        PostRsListItem(
                            post = entry.post,
                            onClick = { onPostClick(entry.post.remotePostId) },
                            onMenuAction = { action ->
                                onPostMenuAction(entry.post.remotePostId, action)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                is PostListEntry.Row -> RedesignedRow(
                    entry = entry,
                    onPostClick = onPostClick,
                    onPostMenuAction = onPostMenuAction,
                    density = density,
                    modifier = Modifier.animateItem()
                )
            }
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShimmerList(isRedesignEnabled: Boolean) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(SHIMMER_ITEM_COUNT) {
            if (isRedesignEnabled) ContentListPlaceholderRow() else PlaceholderItem()
        }
    }
}

/**
 * Fades its content in the first time it appears.
 *
 * Driven by a [MutableTransitionState] rather than a plain `visible = true`: the states this wraps
 * only enter the composition once the list has turned out to be empty or failed, so a plain flag
 * would already sit at its target on first composition and the fade would be skipped entirely.
 */
@Composable
private fun FadeInOnAppear(content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = true

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(STATE_FADE_MS))
    ) {
        content()
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_generic),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun EmptyContent(
    emptyMessageResId: Int,
    onCreatePost: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(emptyMessageResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onCreatePost != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCreatePost) {
                Text(
                    text = stringResource(
                        R.string.posts_empty_list_button
                    )
                )
            }
        }
    }
}

/**
 * One rendered item. Group headers occupy list positions of their own, so the list is built from
 * entries rather than straight from the posts.
 */
private sealed interface PostListEntry {
    val key: Any

    /** Remote id of the post this entry shows, or null for a header. */
    val postId: Long?

    /**
     * [ordinal] disambiguates the key. A sticky post floats to the top regardless of its date, so
     * the same bucket can legitimately open twice in one list - and a repeated LazyColumn key
     * throws rather than merely looking odd.
     */
    data class Header(val group: ContentDateGroup, val ordinal: Int) : PostListEntry {
        override val key get() = "header_${ordinal}_${group.key}"
        override val postId: Long? get() = null
    }

    data class Row(val post: PostRsUiModel, val isHero: Boolean) : PostListEntry {
        override val key get() = post.remotePostId
        override val postId get() = post.remotePostId
    }

    /**
     * A row that is not a loaded post: still loading, or failed. Which of the two it is comes from
     * the post's own [PostRsUiModel.displayState] at render time.
     */
    data class NonContent(val post: PostRsUiModel) : PostListEntry {
        override val key get() = post.remotePostId
        override val postId get() = post.remotePostId
    }
}

private fun buildEntries(
    posts: List<PostRsUiModel>,
    showDateGroups: Boolean,
    density: ContentListDensity
): List<PostListEntry> {
    val entries = mutableListOf<PostListEntry>()
    var currentGroupKey: String? = null
    var headerCount = 0

    posts.forEach { post ->
        if (post.displayState == PostDisplayState.PLACEHOLDER ||
            post.displayState == PostDisplayState.ERROR
        ) {
            entries += PostListEntry.NonContent(post)
            return@forEach
        }

        if (showDateGroups && post.dateGmtMillis > 0L) {
            val group = ContentDateGrouper.groupOf(post.dateGmtMillis)
            if (group.key != currentGroupKey) {
                entries += PostListEntry.Header(group, headerCount++)
                currentGroupKey = group.key
            }
        }

        // Every post with a featured image gets the hero treatment, not just the newest. Keyed off
        // the featured image *id*, which is present as soon as the post loads, rather than the
        // resolved URL, which arrives a network call later - keying off the URL would pop rows from
        // compact to hero as their images resolved.
        // An image-led card with no image is just a compact card with the wrong padding, so a
        // post whose media could not be resolved falls back to the compact shape.
        val isHero = !density.isCondensed &&
            showDateGroups &&
            post.featuredImageId != 0L &&
            !post.isFeaturedImageUnresolvable
        entries += PostListEntry.Row(post, isHero)
    }
    return entries
}

@Composable
private fun RedesignedRow(
    entry: PostListEntry.Row,
    onPostClick: (Long) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
    density: ContentListDensity,
    modifier: Modifier = Modifier
) {
    val post = entry.post
    val state = post.toContentListRowUiState()
    val onClick = { onPostClick(post.remotePostId) }
    val menu: (@Composable () -> Unit)? = if (post.actions.isEmpty()) {
        null
    } else {
        {
            PostRsOverflowMenu(
                actions = post.actions,
                onAction = { action -> onPostMenuAction(post.remotePostId, action) }
            )
        }
    }

    // The two shapes are different layouts, not one layout resized, so the swap is crossfaded
    // rather than left to snap. Only the lead row ever takes this branch; the rest change size
    // only, which the card animates itself.
    AnimatedContent(
        targetState = entry.isHero,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier,
        label = "content list row density"
    ) { isHero ->
        if (isHero) {
            ContentListHeroRow(state = state, onClick = onClick, menu = menu)
        } else {
            ContentListRow(state = state, onClick = onClick, density = density, menu = menu)
        }
    }
}

@Composable
private fun PostRsOverflowMenu(
    actions: List<PostRsMenuAction>,
    onAction: (PostRsMenuAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                val color = if (action.isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(action.labelResId), color = color) },
                    onClick = {
                        expanded = false
                        onAction(action)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(action.iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(MENU_ICON_SIZE),
                            tint = color
                        )
                    }
                )
            }
        }
    }
}

private val MENU_ICON_SIZE = 20.dp

/** Long enough to read as a fade rather than a flicker, short enough not to feel sluggish. */
private const val STATE_FADE_MS = 300

/** How long the visible-row set must settle before metrics are fetched for it. */
private const val VISIBLE_ROWS_DEBOUNCE_MS = 300L

private const val LOAD_MORE_THRESHOLD = 5
private const val SHIMMER_ITEM_COUNT = 8

/**
 * How long a reveal waits for the refresh carrying the post to land before giving up.
 */
private const val REVEAL_TIMEOUT_MS = 15_000L
