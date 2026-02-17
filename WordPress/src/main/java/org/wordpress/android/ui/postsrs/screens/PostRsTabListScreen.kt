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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.PostRsUiModel
import org.wordpress.android.ui.postsrs.PostTabUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsTabListScreen(
    state: PostTabUiState,
    emptyMessageResId: Int,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (Long) -> Unit,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier,
    isSearchIdle: Boolean = false,
    isSearching: Boolean = false
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
            state.isLoading -> ShimmerList()
            state.error != null && state.posts.isEmpty() -> {
                ErrorContent(state.error)
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
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                onLoadMore = onLoadMore,
                onPostClick = onPostClick
            )
        }
    }
}

@Composable
private fun PostListContent(
    posts: List<PostRsUiModel>,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onPostClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()

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
            items = posts,
            key = { it.remotePostId }
        ) { post ->
            PostRsListItem(
                post = post,
                onClick = { onPostClick(post.remotePostId) }
            )
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
private fun ShimmerList() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(SHIMMER_ITEM_COUNT) {
            PlaceholderItem()
        }
    }
}

@Composable
private fun ErrorContent(error: String) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

private const val LOAD_MORE_THRESHOLD = 5
private const val SHIMMER_ITEM_COUNT = 8
