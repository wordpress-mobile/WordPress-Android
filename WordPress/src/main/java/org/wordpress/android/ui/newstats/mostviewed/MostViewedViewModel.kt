package org.wordpress.android.ui.newstats.mostviewed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.MostViewedItemData
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.util.toDateRangeString
import kotlin.math.abs
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class MostViewedViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    // Separate state flows for each data source
    private val _postsUiState = MutableStateFlow<MostViewedCardUiState>(MostViewedCardUiState.Loading)
    val postsUiState: StateFlow<MostViewedCardUiState> = _postsUiState.asStateFlow()

    private val _referrersUiState = MutableStateFlow<MostViewedCardUiState>(MostViewedCardUiState.Loading)
    val referrersUiState: StateFlow<MostViewedCardUiState> = _referrersUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days

    // Cache for detail data - separate for each data source
    private var postsAllItems: List<MostViewedDetailItem> = emptyList()
    private var postsCachedTotalViews: Long = 0L
    private var postsCachedTotalViewsChange: Long = 0L
    private var postsCachedTotalViewsChangePercent: Double = 0.0

    private var referrersAllItems: List<MostViewedDetailItem> = emptyList()
    private var referrersCachedTotalViews: Long = 0L
    private var referrersCachedTotalViewsChange: Long = 0L
    private var referrersCachedTotalViewsChangePercent: Double = 0.0

    init {
        loadData()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        if (period == currentPeriod) return
        currentPeriod = period
        loadData()
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        statsRepository.init(accessToken)
        viewModelScope.launch {
            _isRefreshing.value = true
            loadDataInternal(site.siteId)
            _isRefreshing.value = false
        }
    }

    fun onRetryPosts() {
        loadDataForSource(MostViewedDataSource.POSTS_AND_PAGES)
    }

    fun onRetryReferrers() {
        loadDataForSource(MostViewedDataSource.REFERRERS)
    }

    fun getPostsDetailData(): MostViewedDetailData {
        return MostViewedDetailData(
            cardType = StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            items = postsAllItems,
            totalViews = postsCachedTotalViews,
            totalViewsChange = postsCachedTotalViewsChange,
            totalViewsChangePercent = postsCachedTotalViewsChangePercent,
            dateRange = currentPeriod.toDateRangeString(resourceProvider)
        )
    }

    fun getReferrersDetailData(): MostViewedDetailData {
        return MostViewedDetailData(
            cardType = StatsCardType.MOST_VIEWED_REFERRERS,
            items = referrersAllItems,
            totalViews = referrersCachedTotalViews,
            totalViewsChange = referrersCachedTotalViewsChange,
            totalViewsChangePercent = referrersCachedTotalViewsChangePercent,
            dateRange = currentPeriod.toDateRangeString(resourceProvider)
        )
    }

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            val errorState = MostViewedCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_no_site_selected)
            )
            _postsUiState.value = errorState
            _referrersUiState.value = errorState
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            val errorState = MostViewedCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
            )
            _postsUiState.value = errorState
            _referrersUiState.value = errorState
            return
        }

        statsRepository.init(accessToken)
        _postsUiState.value = MostViewedCardUiState.Loading
        _referrersUiState.value = MostViewedCardUiState.Loading

        viewModelScope.launch {
            loadDataInternal(site.siteId)
        }
    }

    private fun loadDataForSource(dataSource: MostViewedDataSource) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            val errorState = MostViewedCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_no_site_selected)
            )
            when (dataSource) {
                MostViewedDataSource.POSTS_AND_PAGES -> _postsUiState.value = errorState
                MostViewedDataSource.REFERRERS -> _referrersUiState.value = errorState
            }
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            val errorState = MostViewedCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
            )
            when (dataSource) {
                MostViewedDataSource.POSTS_AND_PAGES -> _postsUiState.value = errorState
                MostViewedDataSource.REFERRERS -> _referrersUiState.value = errorState
            }
            return
        }

        statsRepository.init(accessToken)
        when (dataSource) {
            MostViewedDataSource.POSTS_AND_PAGES -> _postsUiState.value = MostViewedCardUiState.Loading
            MostViewedDataSource.REFERRERS -> _referrersUiState.value = MostViewedCardUiState.Loading
        }

        viewModelScope.launch {
            loadDataForSourceInternal(site.siteId, dataSource)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(siteId: Long) {
        // Load both data sources in parallel
        viewModelScope.launch {
            loadDataForSourceInternal(siteId, MostViewedDataSource.POSTS_AND_PAGES)
        }
        viewModelScope.launch {
            loadDataForSourceInternal(siteId, MostViewedDataSource.REFERRERS)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataForSourceInternal(siteId: Long, dataSource: MostViewedDataSource) {
        try {
            val result = statsRepository.fetchMostViewed(siteId, currentPeriod, dataSource)
            when (result) {
                is MostViewedResult.Success -> handleSuccessResult(result, dataSource)
                is MostViewedResult.Error -> setErrorState(
                    dataSource,
                    resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
                )
            }
        } catch (e: Exception) {
            setErrorState(
                dataSource,
                e.message ?: resourceProvider.getString(R.string.stats_todays_stats_unknown_error)
            )
        }
    }

    private fun handleSuccessResult(result: MostViewedResult.Success, dataSource: MostViewedDataSource) {
        val allItems = result.items.map { it.toDetailItem() }
        val cardItems = allItems.take(CARD_MAX_ITEMS)
        val loadedState = MostViewedCardUiState.Loaded(
            items = cardItems.mapIndexed { index, item ->
                MostViewedItem(
                    id = item.id,
                    title = item.title,
                    views = item.views,
                    change = item.change,
                    isHighlighted = index == 0
                )
            },
            maxViewsForBar = cardItems.firstOrNull()?.views ?: 1L
        )
        updateCacheAndState(dataSource, allItems, result, loadedState)
    }

    private fun updateCacheAndState(
        dataSource: MostViewedDataSource,
        allItems: List<MostViewedDetailItem>,
        result: MostViewedResult.Success,
        loadedState: MostViewedCardUiState.Loaded
    ) {
        when (dataSource) {
            MostViewedDataSource.POSTS_AND_PAGES -> {
                postsAllItems = allItems
                postsCachedTotalViews = result.totalViews
                postsCachedTotalViewsChange = result.totalViewsChange
                postsCachedTotalViewsChangePercent = result.totalViewsChangePercent
                _postsUiState.value = loadedState
            }
            MostViewedDataSource.REFERRERS -> {
                referrersAllItems = allItems
                referrersCachedTotalViews = result.totalViews
                referrersCachedTotalViewsChange = result.totalViewsChange
                referrersCachedTotalViewsChangePercent = result.totalViewsChangePercent
                _referrersUiState.value = loadedState
            }
        }
    }

    private fun setErrorState(dataSource: MostViewedDataSource, message: String) {
        val errorState = MostViewedCardUiState.Error(message = message)
        when (dataSource) {
            MostViewedDataSource.POSTS_AND_PAGES -> _postsUiState.value = errorState
            MostViewedDataSource.REFERRERS -> _referrersUiState.value = errorState
        }
    }

    companion object {
        private const val CARD_MAX_ITEMS = 10
    }
}

data class MostViewedDetailData(
    val cardType: StatsCardType,
    val items: List<MostViewedDetailItem>,
    val totalViews: Long,
    val totalViewsChange: Long,
    val totalViewsChangePercent: Double,
    val dateRange: String
)

private fun MostViewedItemData.toDetailItem(): MostViewedDetailItem {
    val change = when {
        viewsChange > 0 -> MostViewedChange.Positive(viewsChange, abs(viewsChangePercent))
        viewsChange < 0 -> MostViewedChange.Negative(abs(viewsChange), abs(viewsChangePercent))
        else -> MostViewedChange.NoChange
    }
    return MostViewedDetailItem(id = id, title = title, views = views, change = change)
}
