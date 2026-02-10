package org.wordpress.android.ui.posts.rslist.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.posts.rslist.PostTabUiState
import org.wordpress.android.ui.posts.rslist.PostUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsTabListScreen(
    state: PostTabUiState,
    emptyMessage: String,
    onPostClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                }
            }
            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                }
            }
            state.posts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.align(
                            Alignment.Center
                        ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        Text(text = emptyMessage)
                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )
                        Button(onClick = onCreatePost) {
                            Text(
                                text = stringResource(
                                    R.string
                                        .posts_empty_list_button
                                )
                            )
                        }
                    }
                }
            }
            else -> {
                PostList(
                    state = state,
                    onPostClick = onPostClick,
                    onLoadMore = onLoadMore
                )
            }
        }
    }
}

@Composable
private fun PostList(
    state: PostTabUiState,
    onPostClick: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    ObserveLoadMore(
        listState = listState,
        itemCount = state.posts.size,
        canLoadMore = state.canLoadMore,
        isLoadingMore = state.isLoadingMore,
        onLoadMore = onLoadMore
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = state.posts,
            key = { it.remoteId }
        ) { post ->
            PostRsListItem(
                post = post,
                onClick = { onPostClick(post.remoteId) }
            )
        }

        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// region Previews

private val samplePosts = listOf(
    PostUiModel(
        remoteId = 1L,
        title = "Hello World",
        excerpt = "Welcome to WordPress.",
        dateFormatted = "2 hours ago",
        statusLabel = "Published"
    ),
    PostUiModel(
        remoteId = 2L,
        title = "My Second Post",
        excerpt = "This is another sample post.",
        dateFormatted = "1 day ago",
        statusLabel = "Published"
    ),
    PostUiModel(
        remoteId = 3L,
        title = "Draft Ideas",
        excerpt = "",
        dateFormatted = "3 days ago",
        statusLabel = "Draft"
    )
)

@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PostRsTabListScreenPreview() {
    AppThemeM3 {
        PostRsTabListScreen(
            state = PostTabUiState(posts = samplePosts),
            emptyMessage = "No posts yet",
            onPostClick = {},
            onRefresh = {},
            onLoadMore = {},
            onCreatePost = {}
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun PostRsTabListScreenLoadingPreview() {
    AppThemeM3 {
        PostRsTabListScreen(
            state = PostTabUiState(isLoading = true),
            emptyMessage = "No posts yet",
            onPostClick = {},
            onRefresh = {},
            onLoadMore = {},
            onCreatePost = {}
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun PostRsTabListScreenEmptyPreview() {
    AppThemeM3 {
        PostRsTabListScreen(
            state = PostTabUiState(),
            emptyMessage = "No posts yet",
            onPostClick = {},
            onRefresh = {},
            onLoadMore = {},
            onCreatePost = {}
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun PostRsTabListScreenErrorPreview() {
    AppThemeM3 {
        PostRsTabListScreen(
            state = PostTabUiState(
                error = "Failed to load posts"
            ),
            emptyMessage = "No posts yet",
            onPostClick = {},
            onRefresh = {},
            onLoadMore = {},
            onCreatePost = {}
        )
    }
}

// endregion
