package org.wordpress.android.ui.postsrs.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.postsrs.PostRsUiModel
import org.wordpress.android.ui.postsrs.PostTabUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsTabListScreen(
    state: PostTabUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (Long) -> Unit,
    modifier: Modifier = Modifier
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
            state.isLoading -> LoadingContent()
            state.error != null && state.posts.isEmpty() -> {
                ErrorContent(state.error)
            }
            state.posts.isEmpty() && !state.isRefreshing -> {
                EmptyContent()
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

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo
                .lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            canLoadMore && lastVisibleItem >= totalItems - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
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
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(error: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
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
private fun EmptyContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No posts",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val LOAD_MORE_THRESHOLD = 5
