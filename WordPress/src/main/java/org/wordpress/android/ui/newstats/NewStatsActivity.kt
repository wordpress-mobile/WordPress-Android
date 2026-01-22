package org.wordpress.android.ui.newstats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.todaysstats.TodaysStatsCard
import org.wordpress.android.ui.newstats.todaysstats.TodaysStatsViewModel
import org.wordpress.android.ui.newstats.viewsstats.ViewsStatsCard
import org.wordpress.android.ui.newstats.viewsstats.ViewsStatsViewModel

@AndroidEntryPoint
class NewStatsActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                NewStatsScreen(
                    onBackPressed = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, NewStatsActivity::class.java))
        }
    }
}

private enum class StatsTab(val titleResId: Int) {
    TRAFFIC(R.string.stats_traffic),
    INSIGHTS(R.string.stats_insights),
    SUBSCRIBERS(R.string.subscribers)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewStatsScreen(
    onBackPressed: () -> Unit
) {
    val tabs = StatsTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.stats))
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
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
                        text = { Text(text = stringResource(id = tab.titleResId)) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                StatsTabContent(tab = tabs[page])
            }
        }
    }
}

@Composable
private fun StatsTabContent(tab: StatsTab) {
    when (tab) {
        StatsTab.TRAFFIC -> TrafficTabContent()
        else -> PlaceholderTabContent(tab)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrafficTabContent(
    todaysStatsViewModel: TodaysStatsViewModel = viewModel(),
    viewsStatsViewModel: ViewsStatsViewModel = viewModel()
) {
    val todaysStatsUiState by todaysStatsViewModel.uiState.collectAsState()
    val viewsStatsUiState by viewsStatsViewModel.uiState.collectAsState()
    val isTodaysStatsRefreshing by todaysStatsViewModel.isRefreshing.collectAsState()
    val isViewsStatsRefreshing by viewsStatsViewModel.isRefreshing.collectAsState()
    val isRefreshing = isTodaysStatsRefreshing || isViewsStatsRefreshing
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            todaysStatsViewModel.refresh()
            viewsStatsViewModel.refresh()
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TodaysStatsCard(uiState = todaysStatsUiState)
            ViewsStatsCard(
                uiState = viewsStatsUiState,
                onChartTypeChanged = viewsStatsViewModel::onChartTypeChanged,
                onRetry = viewsStatsViewModel::onRetry
            )
        }
    }
}

@Composable
private fun PlaceholderTabContent(tab: StatsTab) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "${stringResource(id = tab.titleResId)} - Coming Soon")
    }
}

@Preview
@Composable
fun NewStatsScreenPreview() {
    AppThemeM3 {
        NewStatsScreen(onBackPressed = {})
    }
}
