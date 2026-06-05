package org.wordpress.android.ui.pagesrs.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.pagesrs.PageRsListTab
import org.wordpress.android.ui.pagesrs.PageTabUiState
import org.wordpress.android.ui.postsrs.SnackbarMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PagesRsListScreen(
    tabStates: Map<PageRsListTab, PageTabUiState>,
    isOpeningPage: Boolean,
    snackbarMessages: Flow<SnackbarMessage> = emptyFlow(),
    onInitTab: (PageRsListTab) -> Unit,
    onTabChanged: (PageRsListTab) -> Unit,
    onRefreshTab: (PageRsListTab) -> Unit,
    onLoadMore: (PageRsListTab) -> Unit,
    onNavigateBack: () -> Unit,
    onPageClick: (Long, PageRsListTab) -> Unit
) {
    val tabs = PageRsListTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessages) {
        snackbarMessages.collect { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg.message,
                actionLabel = msg.actionLabel
            )
            if (result == SnackbarResult.ActionPerformed) {
                msg.onAction?.invoke()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.my_site_btn_site_pages)) },
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
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.settledPage,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.settledPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(text = stringResource(tab.labelResId)) }
                    )
                }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }
                    .distinctUntilChanged()
                    .withIndex()
                    .collect { (index, page) ->
                        onInitTab(tabs[page])
                        if (index > 0) onTabChanged(tabs[page])
                    }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val tab = tabs[page]
                val tabState = tabStates[tab] ?: PageTabUiState(isLoading = true)

                PageRsTabListScreen(
                    state = tabState,
                    emptyMessageResId = tab.emptyMessageResId,
                    onRefresh = { onRefreshTab(tab) },
                    onLoadMore = { onLoadMore(tab) },
                    onPageClick = { pageId -> onPageClick(pageId, tab) }
                )
            }
        }
    }

    if (isOpeningPage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
