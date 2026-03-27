package org.wordpress.android.ui.newstats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
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
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.newstats.components.AddCardBottomSheet
import org.wordpress.android.ui.newstats.components.AddStatsCardBottomSheet
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.NoConnectionContent
import org.wordpress.android.ui.newstats.locations.LocationsCard
import org.wordpress.android.ui.newstats.locations.LocationsDetailActivity
import org.wordpress.android.ui.newstats.locations.LocationsViewModel
import org.wordpress.android.ui.newstats.mostviewed.MostViewedCard
import org.wordpress.android.ui.newstats.mostviewed.MostViewedCardUiState
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailActivity
import org.wordpress.android.ui.newstats.mostviewed.MostViewedViewModel
import org.wordpress.android.ui.newstats.todaysstats.TodaysStatsCard
import org.wordpress.android.ui.newstats.todaysstats.TodaysStatsViewModel
import org.wordpress.android.ui.newstats.authors.AuthorsCard
import org.wordpress.android.ui.newstats.authors.AuthorsCardUiState
import org.wordpress.android.ui.newstats.authors.AuthorsDetailActivity
import org.wordpress.android.ui.newstats.authors.AuthorsViewModel
import org.wordpress.android.ui.newstats.clicks.ClicksViewModel
import org.wordpress.android.ui.newstats.devices.DevicesCard
import org.wordpress.android.ui.newstats.devices.DevicesCardUiState
import org.wordpress.android.ui.newstats.devices.DevicesViewModel
import org.wordpress.android.ui.newstats.filedownloads.FileDownloadsViewModel
import org.wordpress.android.ui.newstats.locations.LocationsCardUiState
import org.wordpress.android.ui.newstats.searchterms.SearchTermsViewModel
import org.wordpress.android.ui.newstats.videoplays.VideoPlaysViewModel
import org.wordpress.android.ui.newstats.viewsstats.ViewsStatsCard
import org.wordpress.android.ui.newstats.viewsstats.ViewsStatsViewModel
import org.wordpress.android.ui.newstats.subscribers.SubscribersTabContent
import android.widget.Toast
import org.wordpress.android.ui.newstats.alltimestats.AllTimeStatsCard
import org.wordpress.android.ui.newstats.alltimestats.AllTimeStatsViewModel
import org.wordpress.android.ui.newstats.mostpopularday.MostPopularDayCard
import org.wordpress.android.ui.newstats.mostpopularday.MostPopularDayViewModel
import org.wordpress.android.ui.newstats.mostpopulartime.MostPopularTimeCard
import org.wordpress.android.ui.newstats.mostpopulartime.MostPopularTimeViewModel
import org.wordpress.android.ui.newstats.yearinreview.YearInReviewCard
import org.wordpress.android.ui.newstats.tagsandcategories.TagsAndCategoriesCard
import org.wordpress.android.ui.newstats.tagsandcategories.TagsAndCategoriesDetailActivity
import org.wordpress.android.ui.newstats.tagsandcategories.TagsAndCategoriesViewModel
import org.wordpress.android.ui.newstats.yearinreview.YearInReviewDetailActivity
import org.wordpress.android.ui.newstats.yearinreview.YearInReviewViewModel
import org.wordpress.android.ui.newstats.util.ProvideShimmerBrush
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.components.NewStatsIntroBottomSheet
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.ui.stats.refresh.StatsActivity
import org.wordpress.android.ui.stats.refresh.utils.StatsLaunchedFrom
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

@AndroidEntryPoint
class NewStatsActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var experimentalFeatures: ExperimentalFeatures

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shouldShowIntro =
            !appPrefsWrapper.getNewStatsIntroShown()
        setContent {
            AppThemeM3 {
                NewStatsScreen(
                    onBackPressed =
                        onBackPressedDispatcher::onBackPressed,
                    onSwitchToOldStats = ::switchToOldStats,
                    showIntroBottomSheet = shouldShowIntro,
                    onIntroDismissed = {
                        appPrefsWrapper
                            .setNewStatsIntroShown(true)
                    }
                )
            }
        }
    }

    private fun switchToOldStats() {
        analyticsTracker.track(Stat.STATS_NEW_STATS_DISABLED)
        experimentalFeatures.setEnabled(Feature.NEW_STATS, false)
        appPrefsWrapper.setNewStatsIntroShown(false)
        selectedSiteRepository.getSelectedSite()?.let { site ->
            StatsActivity.start(
                this,
                site,
                launchedFrom = StatsLaunchedFrom.STATS_TOGGLE
            )
            finish()
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

@Composable
private fun StatsOverflowMenu(
    onSwitchToOldStats: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(
                    R.string.more
                )
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            R.string.stats_switch_to_old_stats
                        )
                    )
                },
                onClick = {
                    expanded = false
                    onSwitchToOldStats()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewStatsScreen(
    onBackPressed: () -> Unit,
    onSwitchToOldStats: () -> Unit = {},
    showIntroBottomSheet: Boolean = false,
    onIntroDismissed: () -> Unit = {}
) {
    val viewsStatsViewModel: ViewsStatsViewModel = viewModel()
    val selectedPeriod by viewsStatsViewModel.selectedPeriod.collectAsState()

    val tabs = StatsTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var showPeriodMenu by remember { mutableStateOf(false) }
    var showIntro by remember { mutableStateOf(showIntroBottomSheet) }
    val introSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showIntro) {
        NewStatsIntroBottomSheet(
            sheetState = introSheetState,
            onDismiss = {
                showIntro = false
                onIntroDismissed()
            }
        )
    }
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
                    val currentTab = tabs[pagerState.currentPage]
                    if (currentTab == StatsTab.TRAFFIC) {
                        Box {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        showPeriodMenu = true
                                    }
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = selectedPeriod
                                        .getDisplayLabel(),
                                    style = MaterialTheme
                                        .typography.labelLarge,
                                    color = MaterialTheme
                                        .colorScheme.onSurface
                                )
                                Icon(
                                    imageVector =
                                        Icons.Default.DateRange,
                                    contentDescription =
                                        stringResource(
                                            R.string
                                                .stats_period_selector_content_description
                                        ),
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                )
                            }
                            StatsPeriodMenu(
                                expanded = showPeriodMenu,
                                selectedPeriod = selectedPeriod,
                                onDismiss = {
                                    showPeriodMenu = false
                                },
                                onPresetSelected = { period ->
                                    viewsStatsViewModel
                                        .onPeriodChanged(period)
                                    showPeriodMenu = false
                                },
                                onCustomSelected = {
                                    showPeriodMenu = false
                                    showDateRangePicker = true
                                }
                            )
                        }
                    }
                    StatsOverflowMenu(
                        onSwitchToOldStats = onSwitchToOldStats
                    )
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (tabs.size > 1) {
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
                                    text = stringResource(id = tab.titleResId)
                                )
                            }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = tabs.size > 1
            ) { page ->
                StatsTabContent(
                    tab = tabs[page],
                    viewsStatsViewModel = viewsStatsViewModel
                )
            }
        }
    }
}

@Composable
private fun StatsTabContent(
    tab: StatsTab,
    viewsStatsViewModel: ViewsStatsViewModel
) {
    when (tab) {
        StatsTab.TRAFFIC -> TrafficTabContent(
            viewsStatsViewModel = viewsStatsViewModel
        )
        StatsTab.INSIGHTS -> InsightsTabContent()
        StatsTab.SUBSCRIBERS -> SubscribersTabContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
private fun TrafficTabContent(
    viewsStatsViewModel: ViewsStatsViewModel,
    todaysStatsViewModel: TodaysStatsViewModel = viewModel(),
    mostViewedViewModel: MostViewedViewModel = viewModel(),
    locationsViewModel: LocationsViewModel = viewModel(),
    authorsViewModel: AuthorsViewModel = viewModel(),
    clicksViewModel: ClicksViewModel = viewModel(),
    searchTermsViewModel: SearchTermsViewModel = viewModel(),
    videoPlaysViewModel: VideoPlaysViewModel = viewModel(),
    fileDownloadsViewModel: FileDownloadsViewModel = viewModel(),
    devicesViewModel: DevicesViewModel = viewModel(),
    newStatsViewModel: NewStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val todaysStatsUiState by todaysStatsViewModel.uiState.collectAsState()
    val viewsStatsUiState by viewsStatsViewModel.uiState.collectAsState()
    val postsUiState by mostViewedViewModel.postsUiState.collectAsState()
    val referrersUiState by mostViewedViewModel.referrersUiState.collectAsState()
    val locationsUiState by locationsViewModel.uiState.collectAsState()
    val selectedLocationType by locationsViewModel.selectedLocationType.collectAsState()
    val authorsUiState by authorsViewModel.uiState.collectAsState()
    val clicksUiState by clicksViewModel.uiState.collectAsState()
    val searchTermsUiState by searchTermsViewModel.uiState.collectAsState()
    val videoPlaysUiState by videoPlaysViewModel.uiState.collectAsState()
    val fileDownloadsUiState by fileDownloadsViewModel.uiState.collectAsState()
    val devicesUiState by devicesViewModel.uiState.collectAsState()
    val selectedDeviceType by devicesViewModel.selectedDeviceType.collectAsState()
    val selectedPeriod by viewsStatsViewModel.selectedPeriod.collectAsState()
    val isTodaysStatsRefreshing by todaysStatsViewModel.isRefreshing.collectAsState()
    val isViewsStatsRefreshing by viewsStatsViewModel.isRefreshing.collectAsState()
    val isMostViewedPostsRefreshing by mostViewedViewModel
        .isPostsRefreshing.collectAsState()
    val isMostViewedReferrersRefreshing by mostViewedViewModel
        .isReferrersRefreshing.collectAsState()
    val isLocationsRefreshing by locationsViewModel.isRefreshing.collectAsState()
    val isAuthorsRefreshing by authorsViewModel.isRefreshing.collectAsState()
    val isClicksRefreshing by clicksViewModel.isRefreshing.collectAsState()
    val isSearchTermsRefreshing by searchTermsViewModel
        .isRefreshing.collectAsState()
    val isVideoPlaysRefreshing by videoPlaysViewModel
        .isRefreshing.collectAsState()
    val isFileDownloadsRefreshing by fileDownloadsViewModel
        .isRefreshing.collectAsState()
    val isDevicesRefreshing by devicesViewModel.isRefreshing.collectAsState()
    val isRefreshing = listOf(
        isTodaysStatsRefreshing, isViewsStatsRefreshing,
        isMostViewedPostsRefreshing, isMostViewedReferrersRefreshing,
        isLocationsRefreshing, isAuthorsRefreshing,
        isClicksRefreshing, isSearchTermsRefreshing,
        isVideoPlaysRefreshing, isFileDownloadsRefreshing,
        isDevicesRefreshing
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
            onLocations = {
                locationsViewModel.onPeriodChanged(selectedPeriod)
            },
            onAuthors = {
                authorsViewModel.onPeriodChanged(selectedPeriod)
            },
            onClicks = {
                clicksViewModel.onPeriodChanged(selectedPeriod)
            },
            onSearchTerms = {
                searchTermsViewModel.onPeriodChanged(selectedPeriod)
            },
            onVideoPlays = {
                videoPlaysViewModel.onPeriodChanged(selectedPeriod)
            },
            onFileDownloads = {
                fileDownloadsViewModel.onPeriodChanged(selectedPeriod)
            },
            onDevices = {
                devicesViewModel.onPeriodChanged(selectedPeriod)
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
            onLocations = { locationsViewModel.loadData() },
            onAuthors = { authorsViewModel.loadData() },
            onClicks = { clicksViewModel.loadData() },
            onSearchTerms = { searchTermsViewModel.loadData() },
            onVideoPlays = { videoPlaysViewModel.loadData() },
            onFileDownloads = { fileDownloadsViewModel.loadData() },
            onDevices = { devicesViewModel.loadData() }
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
                onLocations = { locationsViewModel.refresh() },
                onAuthors = { authorsViewModel.refresh() },
                onClicks = { clicksViewModel.refresh() },
                onSearchTerms = { searchTermsViewModel.refresh() },
                onVideoPlays = { videoPlaysViewModel.refresh() },
                onFileDownloads = {
                    fileDownloadsViewModel.refresh()
                },
                onDevices = { devicesViewModel.refresh() }
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
                        onBarTapped = viewsStatsViewModel::onBarTapped,
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
                    StatsCardType.LOCATIONS -> LocationsCard(
                        uiState = locationsUiState,
                        selectedLocationType = selectedLocationType,
                        onLocationTypeChanged = locationsViewModel::onLocationTypeChanged,
                        onShowAllClick = {
                            val detailData = locationsViewModel.getDetailData()
                            LocationsDetailActivity.start(
                                context = context,
                                detailData = detailData
                            )
                        },
                        onRetry = locationsViewModel::onRetry,
                        onRemoveCard = { newStatsViewModel.removeCard(cardType) },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = { newStatsViewModel.moveCardToTop(cardType) },
                        onMoveDown = { newStatsViewModel.moveCardDown(cardType) },
                        onMoveToBottom = { newStatsViewModel.moveCardToBottom(cardType) },
                        onOpenWpAdmin = buildOpenWpAdminAction(
                            isAuthError = (locationsUiState as?
                                LocationsCardUiState.Error)
                                ?.isAuthError == true,
                            getAdminUrl = locationsViewModel::getAdminUrl,
                            context = context
                        )
                    )
                    StatsCardType.DEVICES -> DevicesCard(
                        uiState = devicesUiState,
                        selectedDeviceType = selectedDeviceType,
                        onDeviceTypeChanged =
                            devicesViewModel::onDeviceTypeChanged,
                        onRetry = devicesViewModel::onRetry,
                        onRemoveCard = {
                            newStatsViewModel.removeCard(cardType)
                        },
                        cardPosition = cardPosition,
                        onMoveUp = {
                            newStatsViewModel.moveCardUp(cardType)
                        },
                        onMoveToTop = {
                            newStatsViewModel.moveCardToTop(cardType)
                        },
                        onMoveDown = {
                            newStatsViewModel.moveCardDown(cardType)
                        },
                        onMoveToBottom = {
                            newStatsViewModel.moveCardToBottom(
                                cardType
                            )
                        },
                        onOpenWpAdmin = buildOpenWpAdminAction(
                            isAuthError = (devicesUiState as?
                                DevicesCardUiState.Error)
                                ?.isAuthError == true,
                            getAdminUrl =
                                devicesViewModel::getAdminUrl,
                            context = context
                        )
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
                        onMoveToBottom = { newStatsViewModel.moveCardToBottom(cardType) },
                        onOpenWpAdmin = buildOpenWpAdminAction(
                            isAuthError = (authorsUiState as?
                                AuthorsCardUiState.Error)
                                ?.isAuthError == true,
                            getAdminUrl = authorsViewModel::getAdminUrl,
                            context = context
                        )
                    )
                    StatsCardType.CLICKS -> MostViewedCard(
                        uiState = clicksUiState,
                        cardType = cardType,
                        onShowAllClick = {
                            val detailData = clicksViewModel.getDetailData()
                            MostViewedDetailActivity.start(
                                context = context,
                                cardType = detailData.cardType,
                                items = detailData.items,
                                totalViews = detailData.totalViews,
                                totalViewsChange = detailData.totalViewsChange,
                                totalViewsChangePercent =
                                    detailData.totalViewsChangePercent,
                                dateRange = detailData.dateRange,
                                valueHeaderResId = R.string.stats_clicks_label
                            )
                        },
                        onRetry = clicksViewModel::onRetry,
                        onRemoveCard = {
                            newStatsViewModel.removeCard(cardType)
                        },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = {
                            newStatsViewModel.moveCardToTop(cardType)
                        },
                        onMoveDown = {
                            newStatsViewModel.moveCardDown(cardType)
                        },
                        onMoveToBottom = {
                            newStatsViewModel.moveCardToBottom(cardType)
                        },
                        onOpenWpAdmin = buildOpenWpAdminAction(
                            isAuthError = (clicksUiState as?
                                MostViewedCardUiState.Error)
                                ?.isAuthError == true,
                            getAdminUrl = clicksViewModel::getAdminUrl,
                            context = context
                        )
                    )
                    StatsCardType.SEARCH_TERMS -> MostViewedCard(
                        uiState = searchTermsUiState,
                        cardType = cardType,
                        onShowAllClick = {
                            val detailData =
                                searchTermsViewModel.getDetailData()
                            MostViewedDetailActivity.start(
                                context = context,
                                cardType = detailData.cardType,
                                items = detailData.items,
                                totalViews = detailData.totalViews,
                                totalViewsChange = detailData.totalViewsChange,
                                totalViewsChangePercent =
                                    detailData.totalViewsChangePercent,
                                dateRange = detailData.dateRange
                            )
                        },
                        onRetry = searchTermsViewModel::onRetry,
                        onRemoveCard = {
                            newStatsViewModel.removeCard(cardType)
                        },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = {
                            newStatsViewModel.moveCardToTop(cardType)
                        },
                        onMoveDown = {
                            newStatsViewModel.moveCardDown(cardType)
                        },
                        onMoveToBottom = {
                            newStatsViewModel.moveCardToBottom(cardType)
                        },
                        onOpenWpAdmin = buildOpenWpAdminAction(
                            isAuthError = (searchTermsUiState as?
                                MostViewedCardUiState.Error)
                                ?.isAuthError == true,
                            getAdminUrl =
                                searchTermsViewModel::getAdminUrl,
                            context = context
                        )
                    )
                    StatsCardType.VIDEO_PLAYS -> MostViewedCard(
                        uiState = videoPlaysUiState,
                        cardType = cardType,
                        onShowAllClick = {
                            val detailData =
                                videoPlaysViewModel.getDetailData()
                            MostViewedDetailActivity.start(
                                context = context,
                                cardType = detailData.cardType,
                                items = detailData.items,
                                totalViews = detailData.totalViews,
                                totalViewsChange = detailData.totalViewsChange,
                                totalViewsChangePercent =
                                    detailData.totalViewsChangePercent,
                                dateRange = detailData.dateRange
                            )
                        },
                        onRetry = videoPlaysViewModel::onRetry,
                        onRemoveCard = {
                            newStatsViewModel.removeCard(cardType)
                        },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = {
                            newStatsViewModel.moveCardToTop(cardType)
                        },
                        onMoveDown = {
                            newStatsViewModel.moveCardDown(cardType)
                        },
                        onMoveToBottom = {
                            newStatsViewModel.moveCardToBottom(cardType)
                        },
                        onOpenWpAdmin = buildOpenWpAdminAction(
                            isAuthError = (videoPlaysUiState as?
                                MostViewedCardUiState.Error)
                                ?.isAuthError == true,
                            getAdminUrl =
                                videoPlaysViewModel::getAdminUrl,
                            context = context
                        )
                    )
                    StatsCardType.FILE_DOWNLOADS -> MostViewedCard(
                        uiState = fileDownloadsUiState,
                        cardType = cardType,
                        onShowAllClick = {
                            val detailData =
                                fileDownloadsViewModel.getDetailData()
                            MostViewedDetailActivity.start(
                                context = context,
                                cardType = detailData.cardType,
                                items = detailData.items,
                                totalViews = detailData.totalViews,
                                totalViewsChange = detailData.totalViewsChange,
                                totalViewsChangePercent =
                                    detailData.totalViewsChangePercent,
                                dateRange = detailData.dateRange,
                                valueHeaderResId =
                                    R.string.stats_file_downloads_value_label
                            )
                        },
                        onRetry = fileDownloadsViewModel::onRetry,
                        onRemoveCard = {
                            newStatsViewModel.removeCard(cardType)
                        },
                        cardPosition = cardPosition,
                        onMoveUp = { newStatsViewModel.moveCardUp(cardType) },
                        onMoveToTop = {
                            newStatsViewModel.moveCardToTop(cardType)
                        },
                        onMoveDown = {
                            newStatsViewModel.moveCardDown(cardType)
                        },
                        onMoveToBottom = {
                            newStatsViewModel.moveCardToBottom(cardType)
                        },
                        onOpenWpAdmin = buildOpenWpAdminAction(
                            isAuthError = (fileDownloadsUiState as?
                                MostViewedCardUiState.Error)
                                ?.isAuthError == true,
                            getAdminUrl =
                                fileDownloadsViewModel::getAdminUrl,
                            context = context
                        )
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
    onLocations: () -> Unit,
    onAuthors: () -> Unit,
    onClicks: () -> Unit,
    onSearchTerms: () -> Unit,
    onVideoPlays: () -> Unit,
    onFileDownloads: () -> Unit,
    onDevices: () -> Unit
) {
    if (StatsCardType.TODAYS_STATS in this) onTodaysStats()
    if (StatsCardType.VIEWS_STATS in this) onViewsStats()
    if (StatsCardType.MOST_VIEWED_POSTS_AND_PAGES in this) {
        onMostViewedPosts()
    }
    if (StatsCardType.MOST_VIEWED_REFERRERS in this) {
        onMostViewedReferrers()
    }
    if (StatsCardType.LOCATIONS in this) onLocations()
    if (StatsCardType.AUTHORS in this) onAuthors()
    if (StatsCardType.CLICKS in this) onClicks()
    if (StatsCardType.SEARCH_TERMS in this) onSearchTerms()
    if (StatsCardType.VIDEO_PLAYS in this) onVideoPlays()
    if (StatsCardType.FILE_DOWNLOADS in this) onFileDownloads()
    if (StatsCardType.DEVICES in this) onDevices()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "LongParameterList")
private fun InsightsTabContent(
    yearInReviewViewModel: YearInReviewViewModel = viewModel(),
    allTimeStatsViewModel: AllTimeStatsViewModel = viewModel(),
    mostPopularDayViewModel: MostPopularDayViewModel = viewModel(),
    mostPopularTimeViewModel: MostPopularTimeViewModel = viewModel(),
    tagsAndCategoriesViewModel: TagsAndCategoriesViewModel = viewModel(),
    insightsViewModel: InsightsViewModel = viewModel()
) {
    val context = LocalContext.current
    val yearInReviewUiState by yearInReviewViewModel.uiState.collectAsState()
    val allTimeStatsUiState by allTimeStatsViewModel.uiState.collectAsState()
    val mostPopularDayUiState by mostPopularDayViewModel.uiState.collectAsState()
    val mostPopularTimeUiState by mostPopularTimeViewModel.uiState.collectAsState()
    val tagsAndCategoriesUiState by tagsAndCategoriesViewModel.uiState.collectAsState()
    val isRefreshing by insightsViewModel.isDataRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    val visibleCards by insightsViewModel.visibleCards.collectAsState()
    val hiddenCards by insightsViewModel.hiddenCards.collectAsState()
    val isNetworkAvailable by insightsViewModel.isNetworkAvailable.collectAsState()
    val cardsToLoad by insightsViewModel.cardsToLoad.collectAsState()
    var showAddCardSheet by remember { mutableStateOf(false) }
    val addCardSheetState = rememberModalBottomSheetState()

    LaunchedEffect(cardsToLoad) {
        insightsViewModel.loadDataIfNeeded()
        if (InsightsCardType.TAGS_AND_CATEGORIES in cardsToLoad) {
            tagsAndCategoriesViewModel.loadData()
        }
    }

    val onRetryData = remember { { insightsViewModel.fetchData() } }

    LaunchedEffect(Unit) {
        insightsViewModel.summaryResult.collect { result ->
            allTimeStatsViewModel.handleResult(result)
            mostPopularDayViewModel.handleResult(result)
        }
    }

    LaunchedEffect(Unit) {
        insightsViewModel.insightsResult.collect { result ->
            yearInReviewViewModel.handleResult(result)
            mostPopularTimeViewModel.handleResult(result)
        }
    }

    if (showAddCardSheet) {
        AddCardBottomSheet(
            sheetState = addCardSheetState,
            availableCards = hiddenCards,
            getDisplayNameResId = {
                it.displayNameResId
            },
            onDismiss = { showAddCardSheet = false },
            onCardSelected = { cardType ->
                insightsViewModel.addCard(cardType)
            }
        )
    }

    var showNoConnectionScreen by remember {
        mutableStateOf(!isNetworkAvailable)
    }

    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable && showNoConnectionScreen) {
            showNoConnectionScreen = false
            insightsViewModel.fetchData()
        } else if (!isNetworkAvailable &&
            !showNoConnectionScreen
        ) {
            showNoConnectionScreen = true
        }
    }

    if (showNoConnectionScreen) {
        NoConnectionContent(
            onRetry = {
                val isAvailable =
                    insightsViewModel.checkNetworkStatus()
                if (isAvailable) {
                    showNoConnectionScreen = false
                    insightsViewModel.fetchData()
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
            insightsViewModel.checkNetworkStatus()
            insightsViewModel.refreshData()
            if (InsightsCardType.TAGS_AND_CATEGORIES
                in visibleCards
            ) {
                tagsAndCategoriesViewModel.refresh()
            }
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
        val cardPositions = remember(visibleCards) {
            visibleCards.mapIndexed { index, _ ->
                CardPosition(index = index, totalCards = visibleCards.size)
            }
        }

        ProvideShimmerBrush {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (visibleCards.isEmpty()) {
                    item {
                        val emptyStateMessage = stringResource(
                            R.string.stats_no_cards_message
                        )
                        Text(
                            text = emptyStateMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                                .semantics {
                                    contentDescription = emptyStateMessage
                                },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                itemsIndexed(visibleCards) { index, cardType ->
                    val pos = cardPositions.getOrNull(index)
                    when (cardType) {
                        InsightsCardType.ALL_TIME_STATS -> AllTimeStatsCard(
                            uiState = allTimeStatsUiState,
                            onRemoveCard = { insightsViewModel.removeCard(cardType) },
                            onRetry = { allTimeStatsViewModel.showLoading(); onRetryData() },
                            cardPosition = pos,
                            onMoveUp = { insightsViewModel.moveCardUp(cardType) },
                            onMoveToTop = { insightsViewModel.moveCardToTop(cardType) },
                            onMoveDown = { insightsViewModel.moveCardDown(cardType) },
                            onMoveToBottom = { insightsViewModel.moveCardToBottom(cardType) }
                        )
                        InsightsCardType.MOST_POPULAR_DAY -> MostPopularDayCard(
                            uiState = mostPopularDayUiState,
                            onRemoveCard = { insightsViewModel.removeCard(cardType) },
                            onRetry = { mostPopularDayViewModel.showLoading(); onRetryData() },
                            cardPosition = pos,
                            onMoveUp = { insightsViewModel.moveCardUp(cardType) },
                            onMoveToTop = { insightsViewModel.moveCardToTop(cardType) },
                            onMoveDown = { insightsViewModel.moveCardDown(cardType) },
                            onMoveToBottom = { insightsViewModel.moveCardToBottom(cardType) }
                        )
                        InsightsCardType.MOST_POPULAR_TIME -> MostPopularTimeCard(
                            uiState = mostPopularTimeUiState,
                            onRemoveCard = { insightsViewModel.removeCard(cardType) },
                            onRetry = { mostPopularTimeViewModel.showLoading(); onRetryData() },
                            cardPosition = pos,
                            onMoveUp = { insightsViewModel.moveCardUp(cardType) },
                            onMoveToTop = { insightsViewModel.moveCardToTop(cardType) },
                            onMoveDown = { insightsViewModel.moveCardDown(cardType) },
                            onMoveToBottom = { insightsViewModel.moveCardToBottom(cardType) }
                        )
                        InsightsCardType.YEAR_IN_REVIEW -> YearInReviewCard(
                            uiState = yearInReviewUiState,
                            onRemoveCard = { insightsViewModel.removeCard(cardType) },
                            onShowAllClick = { YearInReviewDetailActivity.start(context) },
                            onRetry = { yearInReviewViewModel.showLoading(); onRetryData() },
                            cardPosition = pos,
                            onMoveUp = { insightsViewModel.moveCardUp(cardType) },
                            onMoveToTop = { insightsViewModel.moveCardToTop(cardType) },
                            onMoveDown = { insightsViewModel.moveCardDown(cardType) },
                            onMoveToBottom = { insightsViewModel.moveCardToBottom(cardType) }
                        )
                        InsightsCardType.TAGS_AND_CATEGORIES -> TagsAndCategoriesCard(
                            uiState = tagsAndCategoriesUiState,
                            onShowAllClick = { TagsAndCategoriesDetailActivity.start(context) },
                            onRemoveCard = { insightsViewModel.removeCard(cardType) },
                            onRetry = { tagsAndCategoriesViewModel.refresh() },
                            cardPosition = pos,
                            onMoveUp = { insightsViewModel.moveCardUp(cardType) },
                            onMoveToTop = { insightsViewModel.moveCardToTop(cardType) },
                            onMoveDown = { insightsViewModel.moveCardDown(cardType) },
                            onMoveToBottom = { insightsViewModel.moveCardToBottom(cardType) }
                        )
                    }
                }

                item {
                    AddCardButton(
                        onClick = { showAddCardSheet = true },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
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

private fun buildOpenWpAdminAction(
    isAuthError: Boolean,
    getAdminUrl: () -> String?,
    context: Context
): (() -> Unit)? = if (isAuthError) {
    {
        val url = getAdminUrl()
        if (url != null) {
            ActivityLauncher.openUrlExternal(context, url)
        } else {
            AppLog.w(
                AppLog.T.STATS,
                "Admin URL is null, cannot open WP Admin"
            )
            Toast.makeText(
                context,
                R.string.stats_error_admin_url_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
} else {
    null
}

@Preview
@Composable
fun NewStatsScreenPreview() {
    AppThemeM3 {
        NewStatsScreen(onBackPressed = {})
    }
}
