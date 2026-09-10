package org.wordpress.android.ui.postsrs.screens

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
    showDateGroups: Boolean = true
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
            state.error != null && state.posts.isEmpty() -> {
                ErrorContent(
                    error = state.error,
                    onRetry = if (state.isAuthError) null else onRefresh
                )
            }
            state.posts.isEmpty() && !state.isRefreshing -> {
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
                showDateGroups = showDateGroups && !isSearching
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
    showDateGroups: Boolean
) {
    val listState = rememberLazyListState()

    val entries = remember(posts, isRedesignEnabled, showDateGroups) {
        if (isRedesignEnabled) {
            buildEntries(posts, showDateGroups)
        } else {
            posts.map { PostListEntry.Legacy(it) }
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
                is PostListEntry.Legacy -> PostRsListItem(
                    post = entry.post,
                    onClick = { onPostClick(entry.post.remotePostId) },
                    onMenuAction = { action ->
                        onPostMenuAction(entry.post.remotePostId, action)
                    },
                    modifier = Modifier.animateItem()
                )
                is PostListEntry.Placeholder ->
                    ContentListPlaceholderRow(modifier = Modifier.animateItem())
                is PostListEntry.Row -> RedesignedRow(
                    entry = entry,
                    onPostClick = onPostClick,
                    onPostMenuAction = onPostMenuAction,
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

    /** A row whose post has not loaded yet. */
    data class Placeholder(val post: PostRsUiModel) : PostListEntry {
        override val key get() = post.remotePostId
        override val postId get() = post.remotePostId
    }

    /** Rendered by the pre-redesign row, which still owns the error presentation. */
    data class Legacy(val post: PostRsUiModel) : PostListEntry {
        override val key get() = post.remotePostId
        override val postId get() = post.remotePostId
    }
}

private fun buildEntries(
    posts: List<PostRsUiModel>,
    showDateGroups: Boolean
): List<PostListEntry> {
    val entries = mutableListOf<PostListEntry>()
    var currentGroupKey: String? = null
    var headerCount = 0
    var hasContentRow = false

    posts.forEach { post ->
        when (post.displayState) {
            PostDisplayState.PLACEHOLDER -> {
                entries += PostListEntry.Placeholder(post)
                return@forEach
            }
            PostDisplayState.ERROR -> {
                entries += PostListEntry.Legacy(post)
                return@forEach
            }
            else -> Unit
        }

        if (showDateGroups && post.dateGmtMillis > 0L) {
            val group = ContentDateGrouper.groupOf(post.dateGmtMillis)
            if (group.key != currentGroupKey) {
                entries += PostListEntry.Header(group, headerCount++)
                currentGroupKey = group.key
            }
        }

        // The lead row keys off the featured image *id*, which is present as soon as the post
        // loads, rather than the resolved URL, which arrives a network call later. Keying off the
        // URL would pop the first row from compact to hero once the image resolved.
        val isHero = showDateGroups && !hasContentRow && post.featuredImageId != 0L
        hasContentRow = true
        entries += PostListEntry.Row(post, isHero)
    }
    return entries
}

@Composable
private fun RedesignedRow(
    entry: PostListEntry.Row,
    onPostClick: (Long) -> Unit,
    onPostMenuAction: (Long, PostRsMenuAction) -> Unit,
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

    if (entry.isHero) {
        ContentListHeroRow(state = state, onClick = onClick, modifier = modifier, menu = menu)
    } else {
        ContentListRow(state = state, onClick = onClick, modifier = modifier, menu = menu)
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

/** How long the visible-row set must settle before metrics are fetched for it. */
private const val VISIBLE_ROWS_DEBOUNCE_MS = 300L

private const val LOAD_MORE_THRESHOLD = 5
private const val SHIMMER_ITEM_COUNT = 8

/**
 * How long a reveal waits for the refresh carrying the post to land before giving up.
 */
private const val REVEAL_TIMEOUT_MS = 15_000L
