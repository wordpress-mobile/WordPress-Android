package org.wordpress.android.ui.reader.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import org.wordpress.android.R
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.ui.reader.views.compose.postcard.ReaderPostCard
import org.wordpress.android.ui.reader.views.compose.postlist.ReaderPostListEmptyState
import org.wordpress.android.ui.rs.contentlist.ContentListPullToRefreshBox
import org.wordpress.android.ui.rs.contentlist.LoadMoreOnScrollToEnd
import org.wordpress.android.ui.rs.contentlist.contentListLoadingMoreItem
import uniffi.wp_api.ReaderSavedKey

/** The saved posts, as the same cards as the Reader's other post lists. */
@Composable
fun ReaderSavedPostsScreen(
    uiState: ReaderSavedPostsViewModel.UiState,
    listState: LazyListState,
    actions: ReaderSavedPostsActions,
) {
    ContentListPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = actions.onRefresh,
        // outermost, so the host's app bar collapses and expands before the list scrolls
        modifier = Modifier
            .nestedScroll(rememberNestedScrollInteropConnection())
            .background(colorResource(R.color.reader_post_list_background))
    ) {
        val posts = uiState.posts
        val margin = dimensionResource(R.dimen.reader_card_margin)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items = posts, key = { _, post -> post.item.key.stableKey() }) { index, post ->
                Column(modifier = Modifier.padding(horizontal = margin)) {
                    ReaderPostCard(
                        uiState = post.card,
                        onCardClick = { post.card.onItemClicked(post.card.postId, post.card.blogId) }
                    )
                    if (index < posts.lastIndex) {
                        HorizontalDivider(color = colorResource(R.color.on_surface_divider))
                    }
                }
            }
            emptyState(uiState)?.let { emptyState ->
                item(key = "empty") {
                    ReaderPostListEmptyState(
                        emptyState = emptyState,
                        onButtonClick = actions.onEmptyStateButtonClicked,
                        isTopAligned = false,
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
            itemCount = posts.size,
            canLoadMore = uiState.hasMore,
            onLoadMore = actions.onLoadMore
        )
    }
}

private fun emptyState(uiState: ReaderSavedPostsViewModel.UiState): ReaderPostListEmptyState? = when {
    uiState.posts.isNotEmpty() || !uiState.isLoaded -> null
    uiState.isRefreshing -> ReaderPostListEmptyState.Updating
    else -> uiState.error ?: ReaderPostListEmptyState.Bookmarks
}

private fun ReaderSavedKey.stableKey(): String = when (this) {
    is ReaderSavedKey.Post -> "post:$blogId:$postId"
    is ReaderSavedKey.FeedItem -> "feed-item:$feedItemId"
    is ReaderSavedKey.Url -> "url:$url"
}
