package org.wordpress.android.ui.postsrs.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.postsrs.PostRsListTab
import org.wordpress.android.ui.postsrs.PostTabUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsListScreen(
    selectedTab: PostRsListTab,
    tabStateFlow: (PostRsListTab) -> StateFlow<PostTabUiState>,
    onTabSelected: (PostRsListTab) -> Unit,
    onPostClick: (Long) -> Unit,
    onRefresh: (PostRsListTab) -> Unit,
    onLoadMore: (PostRsListTab) -> Unit,
    onCreatePost: () -> Unit,
    onBack: () -> Unit
) {
    AppThemeM3 {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                R.string.my_site_btn_blog_posts
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled
                                    .ArrowBack,
                                contentDescription =
                                    stringResource(R.string.back)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onCreatePost,
                    containerColor = MaterialTheme
                        .colorScheme.primaryContainer,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription =
                            stringResource(
                                R.string.new_post
                            )
                    )
                }
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal
                ) {
                    PostRsListTab.entries.forEach { tab ->
                        Tab(
                            selected = tab == selectedTab,
                            onClick = {
                                onTabSelected(tab)
                            },
                            text = {
                                Text(
                                    stringResource(
                                        tab.titleResId
                                    )
                                )
                            }
                        )
                    }
                }

                TabContent(
                    tab = selectedTab,
                    tabStateFlow = tabStateFlow,
                    onTabSelected = onTabSelected,
                    onPostClick = onPostClick,
                    onRefresh = onRefresh,
                    onLoadMore = onLoadMore,
                    onCreatePost = onCreatePost
                )
            }
        }
    }
}

@Composable
private fun TabContent(
    tab: PostRsListTab,
    tabStateFlow: (PostRsListTab) -> StateFlow<PostTabUiState>,
    onTabSelected: (PostRsListTab) -> Unit,
    onPostClick: (Long) -> Unit,
    onRefresh: (PostRsListTab) -> Unit,
    onLoadMore: (PostRsListTab) -> Unit,
    onCreatePost: () -> Unit
) {
    val state by tabStateFlow(tab).collectAsState()
    val emptyMessage = stringResource(
        when (tab) {
            PostRsListTab.PUBLISHED ->
                R.string.posts_empty_list
            PostRsListTab.DRAFTS ->
                R.string.posts_draft_empty
            PostRsListTab.SCHEDULED ->
                R.string.posts_scheduled_empty
            PostRsListTab.TRASHED ->
                R.string.posts_trashed_empty
        }
    )

    LaunchedEffect(tab) {
        onTabSelected(tab)
    }

    PostRsTabListScreen(
        state = state,
        emptyMessage = emptyMessage,
        onPostClick = onPostClick,
        onRefresh = { onRefresh(tab) },
        onLoadMore = { onLoadMore(tab) },
        onCreatePost = onCreatePost
    )
}
