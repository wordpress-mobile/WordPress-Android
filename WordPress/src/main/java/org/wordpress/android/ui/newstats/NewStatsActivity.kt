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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.countries.CountriesCard
import org.wordpress.android.ui.newstats.countries.CountriesDetailActivity
import org.wordpress.android.ui.newstats.countries.CountriesViewModel
import org.wordpress.android.ui.newstats.mostviewed.MostViewedCard
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailActivity
import org.wordpress.android.ui.newstats.mostviewed.MostViewedViewModel
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
    val viewsStatsViewModel: ViewsStatsViewModel = viewModel()
    val selectedPeriod by viewsStatsViewModel.selectedPeriod.collectAsState()

    val tabs = StatsTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var showPeriodMenu by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    if (showDateRangePicker) {
        StatsDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onDateRangeSelected = { startDate, endDate ->
                viewsStatsViewModel.onPeriodChanged(StatsPeriod.Custom(startDate, endDate))
                showDateRangePicker = false
            }
        )
    }

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
                },
                actions = {
                    Box {
                        IconButton(onClick = { showPeriodMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = stringResource(
                                    R.string.stats_period_selector_content_description
                                )
                            )
                        }
                        StatsPeriodMenu(
                            expanded = showPeriodMenu,
                            selectedPeriod = selectedPeriod,
                            onDismiss = { showPeriodMenu = false },
                            onPresetSelected = { period ->
                                viewsStatsViewModel.onPeriodChanged(period)
                                showPeriodMenu = false
                            },
                            onCustomSelected = {
                                showPeriodMenu = false
                                showDateRangePicker = true
                            }
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
                StatsTabContent(tab = tabs[page], viewsStatsViewModel = viewsStatsViewModel)
            }
        }
    }
}

@Composable
private fun StatsTabContent(tab: StatsTab, viewsStatsViewModel: ViewsStatsViewModel) {
    when (tab) {
        StatsTab.TRAFFIC -> TrafficTabContent(viewsStatsViewModel = viewsStatsViewModel)
        else -> PlaceholderTabContent(tab)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrafficTabContent(
    viewsStatsViewModel: ViewsStatsViewModel,
    todaysStatsViewModel: TodaysStatsViewModel = viewModel(),
    mostViewedViewModel: MostViewedViewModel = viewModel(),
    countriesViewModel: CountriesViewModel = viewModel()
) {
    val context = LocalContext.current
    val todaysStatsUiState by todaysStatsViewModel.uiState.collectAsState()
    val viewsStatsUiState by viewsStatsViewModel.uiState.collectAsState()
    val mostViewedUiState by mostViewedViewModel.uiState.collectAsState()
    val countriesUiState by countriesViewModel.uiState.collectAsState()
    val selectedPeriod by viewsStatsViewModel.selectedPeriod.collectAsState()
    val isTodaysStatsRefreshing by todaysStatsViewModel.isRefreshing.collectAsState()
    val isViewsStatsRefreshing by viewsStatsViewModel.isRefreshing.collectAsState()
    val isMostViewedRefreshing by mostViewedViewModel.isRefreshing.collectAsState()
    val isCountriesRefreshing by countriesViewModel.isRefreshing.collectAsState()
    val isRefreshing = isTodaysStatsRefreshing || isViewsStatsRefreshing ||
        isMostViewedRefreshing || isCountriesRefreshing
    val pullToRefreshState = rememberPullToRefreshState()

    // Propagate period changes to the MostViewedViewModel and CountriesViewModel
    LaunchedEffect(selectedPeriod) {
        mostViewedViewModel.onPeriodChanged(selectedPeriod)
        countriesViewModel.onPeriodChanged(selectedPeriod)
    }

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            todaysStatsViewModel.refresh()
            viewsStatsViewModel.refresh()
            mostViewedViewModel.refresh()
            countriesViewModel.refresh()
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
            MostViewedCard(
                uiState = mostViewedUiState,
                onDataSourceChanged = mostViewedViewModel::onDataSourceChanged,
                onShowAllClick = {
                    val detailData = mostViewedViewModel.getDetailData()
                    MostViewedDetailActivity.start(
                        context = context,
                        dataSource = detailData.dataSource,
                        items = detailData.items,
                        totalViews = detailData.totalViews,
                        totalViewsChange = detailData.totalViewsChange,
                        totalViewsChangePercent = detailData.totalViewsChangePercent,
                        dateRange = detailData.dateRange
                    )
                },
                onRetry = mostViewedViewModel::onRetry
            )
            CountriesCard(
                uiState = countriesUiState,
                onShowAllClick = {
                    val detailData = countriesViewModel.getDetailData()
                    CountriesDetailActivity.start(
                        context = context,
                        countries = detailData.countries,
                        mapData = detailData.mapData,
                        minViews = detailData.minViews,
                        maxViews = detailData.maxViews,
                        totalViews = detailData.totalViews,
                        totalViewsChange = detailData.totalViewsChange,
                        totalViewsChangePercent = detailData.totalViewsChangePercent,
                        dateRange = detailData.dateRange
                    )
                },
                onRetry = countriesViewModel::onRetry
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

@Composable
private fun StatsPeriodMenu(
    expanded: Boolean,
    selectedPeriod: StatsPeriod,
    onDismiss: () -> Unit,
    onPresetSelected: (StatsPeriod) -> Unit,
    onCustomSelected: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        // Show preset periods
        StatsPeriod.presets().forEach { period ->
            val isSelected = selectedPeriod == period
            DropdownMenuItem(
                text = { Text(text = stringResource(id = period.labelResId)) },
                onClick = { onPresetSelected(period) },
                trailingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else {
                    null
                }
            )
        }
        // Show Custom option
        val isCustomSelected = selectedPeriod is StatsPeriod.Custom
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.stats_period_custom)) },
            onClick = { onCustomSelected() },
            trailingIcon = if (isCustomSelected) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else {
                null
            }
        )
    }
}

@Preview
@Composable
fun NewStatsScreenPreview() {
    AppThemeM3 {
        NewStatsScreen(onBackPressed = {})
    }
}
