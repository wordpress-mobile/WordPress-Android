package org.wordpress.android.ui.reader.views.compose.postlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import org.wordpress.android.R
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListItem
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListUiState
import org.wordpress.android.ui.reader.views.compose.postcard.ReaderPostCard
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem

private const val NEW_POSTS_BAR_SCROLL_DELAY_MS = 1000L

/**
 * The Reader post list: the rows ReaderPostListViewModel builds, its empty state, and the "new posts" bar
 */
@Composable
fun ReaderPostListScreen(
    uiState: ReaderPostListUiState,
    listState: LazyListState,
    isSearch: Boolean,
    actions: ReaderPostListActions,
) {
    ContentListPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = actions.onRefresh,
        // search results are brief snapshots, and a refresh can't start while older posts are loading
        enabled = !isSearch && !uiState.isLoadingMore,
        // outermost, so the host's app bar collapses and expands before the list scrolls
        modifier = Modifier
            .nestedScroll(rememberNestedScrollInteropConnection())
            .background(colorResource(R.color.reader_post_list_background))
    ) {
        PostList(uiState, listState, actions)
        NewPostsBar(
            isVisible = uiState.newPostsBarVisible,
            listState = listState,
            onClick = {
                listState.requestScrollToItem(0)
                actions.onNewPostsBarClicked()
            },
            onScrolledAway = actions.onNewPostsBarScrolledAway,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun PostList(
    uiState: ReaderPostListUiState,
    listState: LazyListState,
    actions: ReaderPostListActions,
) {
    val emptyState = uiState.emptyState
    // the empty state replaces the posts, but not the site or tag header above them
    val items = if (emptyState == null) {
        uiState.items
    } else {
        uiState.items.filter { it is ReaderPostListItem.SiteHeader || it is ReaderPostListItem.TagHeader }
    }
    val hasPosts = items.any { it is ReaderPostListItem.Post || it is ReaderPostListItem.XPost }
    val margin = dimensionResource(R.dimen.reader_card_margin)

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(items = items, key = { it.key }, contentType = { it::class }) { item ->
            // the site header (and its divider) stays out of sight until its blog info is read
            if (item is ReaderPostListItem.SiteHeader && item.info == null) return@items
            Column(modifier = Modifier.padding(horizontal = margin)) {
                PostListRow(item, actions)
                if (item !== items.last()) {
                    HorizontalDivider(color = colorResource(R.color.on_surface_divider))
                }
            }
        }
        if (emptyState != null) {
            item(key = "empty") {
                ReaderPostListEmptyState(
                    emptyState = emptyState,
                    onButtonClick = actions.onEmptyStateButtonClicked,
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        }
        if (uiState.isLoadingMore) {
            contentListLoadingMoreItem()
        }
    }

    LoadMoreOnScrollToEnd(
        listState = listState,
        itemCount = items.size,
        canLoadMore = hasPosts && !uiState.isLoadingMore,
        onLoadMore = actions.onLoadMore
    )
    KeepTopInViewOnPrepend(listState, items.firstOrNull()?.key, uiState.scrollToPosition)
    PendingScroll(listState, uiState.scrollToPosition, actions.onScrollHandled)
    ReportFirstItemVisibility(listState, actions.onFirstItemVisibilityChanged)
}

@Composable
private fun PostListRow(item: ReaderPostListItem, actions: ReaderPostListActions) {
    when (item) {
        is ReaderPostListItem.Post -> {
            val uiState = item.uiState
            // records the railcar render event, which the view model only sends once per post
            LaunchedEffect(item.key) { uiState.onItemRendered(uiState) }
            ReaderPostCard(
                uiState = uiState,
                onCardClick = { uiState.onItemClicked(uiState.postId, uiState.blogId) }
            )
        }

        is ReaderPostListItem.SiteHeader -> item.info?.let { info ->
            ReaderSiteHeader(
                info = info,
                onFollowClick = actions.onSiteHeaderFollowClicked,
                onSettingsClick = actions.onSiteHeaderSettingsClicked
            )
        }

        is ReaderPostListItem.TagHeader -> ReaderTagHeader(item, actions.onTagHeaderFollowClicked)
        is ReaderPostListItem.GapMarker -> ReaderGapMarker(item.isFilling, actions.onGapMarkerClicked)
        is ReaderPostListItem.XPost -> ReaderXPostRow(item.post) { actions.onXPostClicked(item.post) }
        is ReaderPostListItem.RemovedPost -> ReaderRemovedPostRow(item.post) { actions.onRemovedPostClicked(item.post) }
    }
}

/**
 * A keyed LazyColumn stays anchored on its first row when rows are added above it, so newer posts would
 * land above the screen - keep the top in view instead, as the RecyclerView did
 */
@Composable
private fun KeepTopInViewOnPrepend(listState: LazyListState, firstKey: String?, scrollToPosition: Int?) {
    val previousFirstKey = remember { arrayOfNulls<String>(1) }
    // runs before this frame's measure, so the list still reports the position from before the change
    SideEffect {
        val previous = previousFirstKey[0]
        previousFirstKey[0] = firstKey
        if (previous != null && firstKey != previous && scrollToPosition == null &&
            listState.firstVisibleItemIndex == 0
        ) {
            listState.requestScrollToItem(0)
        }
    }
}

@Composable
private fun PendingScroll(listState: LazyListState, scrollToPosition: Int?, onScrollHandled: () -> Unit) {
    val currentOnScrollHandled by rememberUpdatedState(onScrollHandled)
    LaunchedEffect(scrollToPosition) {
        if (scrollToPosition != null) {
            listState.requestScrollToItem(scrollToPosition)
            currentOnScrollHandled()
        }
    }
}

@Composable
private fun ReportFirstItemVisibility(listState: LazyListState, onChanged: (Boolean) -> Unit) {
    val currentOnChanged by rememberUpdatedState(onChanged)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 }
            .distinctUntilChanged()
            .collect { currentOnChanged(it) }
    }
}

@Composable
private fun NewPostsBar(
    isVisible: Boolean,
    listState: LazyListState,
    onClick: () -> Unit,
    onScrolledAway: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        val currentOnScrolledAway by rememberUpdatedState(onScrolledAway)
        LaunchedEffect(Unit) {
            // give the user a moment to see the bar before a scroll hides it
            delay(NEW_POSTS_BAR_SCROLL_DELAY_MS)
            snapshotFlow { listState.isScrollInProgress }.first { it }
            currentOnScrolledAway()
        }
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.padding(top = 24.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 4.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.reader_label_new_posts),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.reader_label_new_posts_subtitle),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun ReaderPostListRowSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Box { content() }
    }
}
