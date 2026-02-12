package org.wordpress.android.ui.postsrs.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.PostRsListTab
import org.wordpress.android.ui.postsrs.PostRsListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRsListScreen(
    viewModel: PostRsListViewModel,
    onNavigateBack: () -> Unit,
    onPostClick: (Long) -> Unit
) {
    val tabs = PostRsListTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.my_site_btn_blog_posts))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = stringResource(tab.labelResId)
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val tab = tabs[page]

                LaunchedEffect(tab) {
                    viewModel.initTab(tab)
                }

                val tabState by viewModel.getTabState(tab).collectAsState()

                PostRsTabListScreen(
                    state = tabState,
                    onRefresh = { viewModel.refreshTab(tab) },
                    onLoadMore = { viewModel.loadMorePosts(tab) },
                    onPostClick = onPostClick
                )
            }
        }
    }
}
