package org.wordpress.android.ui.newstats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.AddStatsCardBottomSheet
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.countries.CountriesCard
import org.wordpress.android.ui.newstats.countries.CountriesDetailActivity
import org.wordpress.android.ui.newstats.countries.CountriesViewModel
import org.wordpress.android.ui.newstats.mostviewed.MostViewedCard
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailActivity
import org.wordpress.android.ui.newstats.mostviewed.MostViewedViewModel
import org.wordpress.android.ui.newstats.todaysstats.TodaysStatsCard
import org.wordpress.android.ui.newstats.todaysstats.TodaysStatsViewModel
import org.wordpress.android.ui.newstats.authors.AuthorsCard
import org.wordpress.android.ui.newstats.authors.AuthorsDetailActivity
import org.wordpress.android.ui.newstats.authors.AuthorsViewModel
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showPeriodMenu = true }
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = selectedPeriod.getDisplayLabel(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = stringResource(
                                    R.string.stats_period_selector_content_description
                                ),
                                modifier = Modifier.padding(start = 4.dp)
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
    countriesViewModel: CountriesViewModel = viewModel(),
    authorsViewModel: AuthorsViewModel = viewModel(),
    newStatsViewModel: NewStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val todaysStatsUiState by todaysStatsViewModel.uiState.collectAsState()
    val viewsStatsUiState by viewsStatsViewModel.uiState.collectAsState()
    val postsUiState by mostViewedViewModel.postsUiState.collectAsState()
    val referrersUiState by mostViewedViewModel.referrersUiState.collectAsState()
    val countriesUiState by countriesViewModel.uiState.collectAsState()
    val authorsUiState by authorsViewModel.uiState.collectAsState()
    val selectedPeriod by viewsStatsViewModel.selectedPeriod.collectAsState()
    val isTodaysStatsRefreshing by todaysStatsViewModel.isRefreshing.collectAsState()
    val isViewsStatsRefreshing by viewsStatsViewModel.isRefreshing.collectAsState()
    val isMostViewedPostsRefreshing by mostViewedViewModel
        .isPostsRefreshing.collectAsState()
    val isMostViewedReferrersRefreshing by mostViewedViewModel
        .isReferrersRefreshing.collectAsState()
    val isCountriesRefreshing by countriesViewModel.isRefreshing.collectAsState()
    val isAuthorsRefreshing by authorsViewModel.isRefreshing.collectAsState()
    val isRefreshing = listOf(
        isTodaysStatsRefreshing, isViewsStatsRefreshing,
        isMostViewedPostsRefreshing, isMostViewedReferrersRefreshing,
        isCountriesRefreshing, isAuthorsRefreshing
    ).any { it }
    val pullToRefreshState = rememberPullToRefreshState()

    // Card configuration state
    val visibleCards by newStatsViewModel.visibleCards.collectAsState()
    val hiddenCards by newStatsViewModel.hiddenCards.collectAsState()
    val isNetworkAvailable by newStatsViewModel.isNetworkAvailable.collectAsState()
    val cardsToLoad by newStatsViewModel.cardsToLoad.collectAsState()
    val isPeriodInitialized by viewsStatsViewModel.isPeriodInitialized.collectAsState()
    var showAddCardSheet by remember { mutableStateOf(false) }
    val addCardSheetState = rememberModalBottomSheetState()

    // Propagate period changes only to visible card ViewModels.
    // cardsToLoad is empty until the configuration is loaded from the repository,
    // preventing data fetches for the default card set before the real config is known.
    // isPeriodInitialized prevents fetching with a stale default period before the
    // persisted period is restored from preferences.
    LaunchedEffect(selectedPeriod, cardsToLoad, isPeriodInitialized) {
        if (!isPeriodInitialized) return@LaunchedEffect
        cardsToLoad.dispatchToVisibleCards(
            onTodaysStats = { todaysStatsViewModel.loadDataIfNeeded() },
            onViewsStats = { viewsStatsViewModel.loadDataIfNeeded() },
            onMostViewedPosts = {
                mostViewedViewModel.onPeriodChangedPosts(selectedPeriod)
            },
            onMostViewedReferrers = {
                mostViewedViewModel.onPeriodChangedReferrers(
                    selectedPeriod
                )
            },
            onCountries = {
                countriesViewModel.onPeriodChanged(selectedPeriod)
            },
            onAuthors = {
                authorsViewModel.onPeriodChanged(selectedPeriod)
            }
        )
    }

    if (showAddCardSheet) {
        AddStatsCardBottomSheet(
            sheetState = addCardSheetState,
            availableCards = hiddenCards,
            onDismiss = { showAddCardSheet = false },
            onCardSelected = { cardType ->
                newStatsViewModel.addCard(cardType)
            }
        )
    }

    // Track whether to show the no-connection screen
    // Once user retries or network becomes available, show cards instead
    var showNoConnectionScreen by remember { mutableStateOf(!isNetworkAvailable) }

    val loadVisibleCards = {
        visibleCards.dispatchToVisibleCards(
            onTodaysStats = { todaysStatsViewModel.loadData() },
            onViewsStats = { viewsStatsViewModel.loadData() },
            onMostViewedPosts = { mostViewedViewModel.loadPosts() },
            onMostViewedReferrers = {
                mostViewedViewModel.loadReferrers()
            },
            onCountries = { countriesViewModel.loadData() },
            onAuthors = { authorsViewModel.loadData() }
        )
    }

    // React to network availability changes
    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable && showNoConnectionScreen) {
            showNoConnectionScreen = false
            loadVisibleCards()
        } else if (!isNetworkAvailable && !showNoConnectionScreen) {
            showNoConnectionScreen = true
        }
    }

    // Show no connection screen only when network is unavailable
    if (showNoConnectionScreen) {
        NoConnectionContent(
            onRetry = {
                val isAvailable = newStatsViewModel.checkNetworkStatus()
                if (isAvailable) {
                    showNoConnectionScreen = false
                    loadVisibleCards()
                }
            }
        )
        return
    }

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            newStatsViewModel.checkNetworkStatus()
            visibleCards.dispatchToVisibleCards(
                onTodaysStats = { todaysStatsViewModel.refresh() },
                onViewsStats = { viewsStatsViewModel.refresh() },
                onMostViewedPosts = {
                    mostViewedViewModel.refreshPosts()
                },
                onMostViewedReferrers = {
                    mostViewedViewModel.refreshReferrers()
                },
                onCountries = { countriesViewModel.refresh() },
                onAuthors = { authorsViewModel.refresh() }
            )
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
            if (visibleCards.isEmpty()) {
                // Empty state message
                val emptyStateMessage = stringResource(R.string.stats_no_cards_message)
                Text(
                    text = emptyStateMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .semantics { contentDescription = emptyStateMessage },
                    textAlign = TextAlign.Center
                )
            }

            // Memoize card positions to avoid recalculation on every recomposition
            val cardPositions = remember(visibleCards) {
                visibleCards.mapIndexed { index, _ ->
                    CardPosition(index = index, totalCards = visibleCards.size)
                }
            }

            // Dynamic card rendering based on configuration
            visibleCards.forEachIndexed { index, cardType ->
                val cardPosition = cardPositions[index]
                when (cardType) {
                    StatsCardType.TODAYS_STATS -> TodaysStatsCard(
                        uiState = todaysStatsUiState,
                        onRemoveCard = { newStatsViewModel.removeCard(cardType) },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = { newStatsViewModel.moveCardToTop(cardType) },
                        onMoveDown = { newStatsViewModel.moveCardDown(cardType) },
                        onMoveToBottom = { newStatsViewModel.moveCardToBottom(cardType) }
                    )
                    StatsCardType.VIEWS_STATS -> ViewsStatsCard(
                        uiState = viewsStatsUiState,
                        onChartTypeChanged = viewsStatsViewModel::onChartTypeChanged,
                        onRetry = viewsStatsViewModel::onRetry,
                        onRemoveCard = { newStatsViewModel.removeCard(cardType) },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = { newStatsViewModel.moveCardToTop(cardType) },
                        onMoveDown = { newStatsViewModel.moveCardDown(cardType) },
                        onMoveToBottom = { newStatsViewModel.moveCardToBottom(cardType) }
                    )
                    StatsCardType.MOST_VIEWED_POSTS_AND_PAGES -> MostViewedCard(
                        uiState = postsUiState,
                        cardType = cardType,
                        onShowAllClick = {
                            val detailData = mostViewedViewModel.getPostsDetailData()
                            MostViewedDetailActivity.start(
                                context = context,
                                cardType = detailData.cardType,
                                items = detailData.items,
                                totalViews = detailData.totalViews,
                                totalViewsChange = detailData.totalViewsChange,
                                totalViewsChangePercent = detailData.totalViewsChangePercent,
                                dateRange = detailData.dateRange
                            )
                        },
                        onRetry = mostViewedViewModel::onRetryPosts,
                        onRemoveCard = { newStatsViewModel.removeCard(cardType) },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = { newStatsViewModel.moveCardToTop(cardType) },
                        onMoveDown = { newStatsViewModel.moveCardDown(cardType) },
                        onMoveToBottom = { newStatsViewModel.moveCardToBottom(cardType) }
                    )
                    StatsCardType.MOST_VIEWED_REFERRERS -> MostViewedCard(
                        uiState = referrersUiState,
                        cardType = cardType,
                        onShowAllClick = {
                            val detailData = mostViewedViewModel.getReferrersDetailData()
                            MostViewedDetailActivity.start(
                                context = context,
                                cardType = detailData.cardType,
                                items = detailData.items,
                                totalViews = detailData.totalViews,
                                totalViewsChange = detailData.totalViewsChange,
                                totalViewsChangePercent = detailData.totalViewsChangePercent,
                                dateRange = detailData.dateRange
                            )
                        },
                        onRetry = mostViewedViewModel::onRetryReferrers,
                        onRemoveCard = { newStatsViewModel.removeCard(cardType) },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = { newStatsViewModel.moveCardToTop(cardType) },
                        onMoveDown = { newStatsViewModel.moveCardDown(cardType) },
                        onMoveToBottom = { newStatsViewModel.moveCardToBottom(cardType) }
                    )
                    StatsCardType.COUNTRIES -> CountriesCard(
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
                        onRetry = countriesViewModel::onRetry,
                        onRemoveCard = { newStatsViewModel.removeCard(cardType) },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = { newStatsViewModel.moveCardToTop(cardType) },
                        onMoveDown = { newStatsViewModel.moveCardDown(cardType) },
                        onMoveToBottom = { newStatsViewModel.moveCardToBottom(cardType) }
                    )
                    StatsCardType.AUTHORS -> AuthorsCard(
                        uiState = authorsUiState,
                        onShowAllClick = {
                            val detailData = authorsViewModel.getDetailData()
                            AuthorsDetailActivity.start(
                                context = context,
                                authors = detailData.authors,
                                totalViews = detailData.totalViews,
                                totalViewsChange = detailData.totalViewsChange,
                                totalViewsChangePercent = detailData.totalViewsChangePercent,
                                dateRange = detailData.dateRange
                            )
                        },
                        onRetry = authorsViewModel::onRetry,
                        onRemoveCard = { newStatsViewModel.removeCard(cardType) },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = { newStatsViewModel.moveCardToTop(cardType) },
                        onMoveDown = { newStatsViewModel.moveCardDown(cardType) },
                        onMoveToBottom = { newStatsViewModel.moveCardToBottom(cardType) }
                    )
                }
            }

            // Add Card Button
            AddCardButton(
                onClick = { showAddCardSheet = true },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun AddCardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.stats_add_card_title))
    }
}

@Suppress("LongParameterList")
private fun List<StatsCardType>.dispatchToVisibleCards(
    onTodaysStats: () -> Unit,
    onViewsStats: () -> Unit,
    onMostViewedPosts: () -> Unit,
    onMostViewedReferrers: () -> Unit,
    onCountries: () -> Unit,
    onAuthors: () -> Unit
) {
    if (StatsCardType.TODAYS_STATS in this) onTodaysStats()
    if (StatsCardType.VIEWS_STATS in this) onViewsStats()
    if (StatsCardType.MOST_VIEWED_POSTS_AND_PAGES in this) {
        onMostViewedPosts()
    }
    if (StatsCardType.MOST_VIEWED_REFERRERS in this) {
        onMostViewedReferrers()
    }
    if (StatsCardType.COUNTRIES in this) onCountries()
    if (StatsCardType.AUTHORS in this) onAuthors()
}

@Composable
private fun NoConnectionContent(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wifi_off_24px),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.no_connection_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_connection_error_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
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

@Composable
private fun StatsPeriod.getDisplayLabel(): String {
    return when (this) {
        is StatsPeriod.Custom -> {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
            "${startDate.format(formatter)} - ${endDate.format(formatter)}"
        }
        else -> stringResource(id = labelResId)
    }
}

@Preview
@Composable
fun NewStatsScreenPreview() {
    AppThemeM3 {
        NewStatsScreen(onBackPressed = {})
    }
}
