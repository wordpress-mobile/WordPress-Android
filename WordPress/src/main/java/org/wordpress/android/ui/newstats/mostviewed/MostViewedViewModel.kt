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

    private val _isPostsRefreshing = MutableStateFlow(false)
    val isPostsRefreshing: StateFlow<Boolean> = _isPostsRefreshing.asStateFlow()

    private val _isReferrersRefreshing = MutableStateFlow(false)
    val isReferrersRefreshing: StateFlow<Boolean> =
        _isReferrersRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var postsLoadingPeriod: StatsPeriod? = null
    private var postsLoadedPeriod: StatsPeriod? = null
    private var referrersLoadingPeriod: StatsPeriod? = null
    private var referrersLoadedPeriod: StatsPeriod? = null

    // Cache for detail data - separate for each data source
    private var postsAllItems: List<MostViewedDetailItem> = emptyList()
    private var postsCachedTotalViews: Long = 0L
    private var postsCachedTotalViewsChange: Long = 0L
    private var postsCachedTotalViewsChangePercent: Double = 0.0

    private var referrersAllItems: List<MostViewedDetailItem> = emptyList()
    private var referrersCachedTotalViews: Long = 0L
    private var referrersCachedTotalViewsChange: Long = 0L
    private var referrersCachedTotalViewsChangePercent: Double = 0.0

    fun onPeriodChanged(period: StatsPeriod) {
        currentPeriod = period
        onPeriodChangedPosts(period)
        onPeriodChangedReferrers(period)
    }

    fun onPeriodChangedPosts(period: StatsPeriod) {
        if (postsLoadedPeriod == period ||
            postsLoadingPeriod == period
        ) return
        postsLoadingPeriod = period
        currentPeriod = period
        loadPosts()
    }

    fun onPeriodChangedReferrers(period: StatsPeriod) {
        if (referrersLoadedPeriod == period ||
            referrersLoadingPeriod == period
        ) return
        referrersLoadingPeriod = period
        currentPeriod = period
        loadReferrers()
    }

    fun refresh() {
        refreshPosts()
        refreshReferrers()
    }

    fun refreshPosts() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        statsRepository.init(accessToken)
        viewModelScope.launch {
            _isPostsRefreshing.value = true
            loadDataForSourceInternal(
                site.siteId,
                MostViewedDataSource.POSTS_AND_PAGES
            )
            _isPostsRefreshing.value = false
        }
    }

    fun refreshReferrers() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        statsRepository.init(accessToken)
        viewModelScope.launch {
            _isReferrersRefreshing.value = true
            loadDataForSourceInternal(
                site.siteId,
                MostViewedDataSource.REFERRERS
            )
            _isReferrersRefreshing.value = false
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
        loadPosts()
        loadReferrers()
    }

    fun loadPosts() {
        loadDataForSource(MostViewedDataSource.POSTS_AND_PAGES)
    }

    fun loadReferrers() {
        loadDataForSource(MostViewedDataSource.REFERRERS)
    }

    private fun loadDataForSource(dataSource: MostViewedDataSource) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            clearLoadingPeriod(dataSource)
            val errorState = MostViewedCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_todays_stats_no_site_selected
                )
            )
            setUiState(dataSource, errorState)
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            clearLoadingPeriod(dataSource)
            val errorState = MostViewedCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_todays_stats_failed_to_load
                )
            )
            setUiState(dataSource, errorState)
            return
        }

        statsRepository.init(accessToken)
        setUiState(dataSource, MostViewedCardUiState.Loading)

        viewModelScope.launch {
            try {
                loadDataForSourceInternal(site.siteId, dataSource)
                if (!hasErrorStateForSource(dataSource)) {
                    setLoadedPeriod(dataSource, currentPeriod)
                }
            } finally {
                clearLoadingPeriod(dataSource)
            }
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

    private fun hasErrorStateForSource(
        dataSource: MostViewedDataSource
    ): Boolean {
        return when (dataSource) {
            MostViewedDataSource.POSTS_AND_PAGES ->
                _postsUiState.value is MostViewedCardUiState.Error
            MostViewedDataSource.REFERRERS ->
                _referrersUiState.value is MostViewedCardUiState.Error
        }
    }

    private fun setUiState(
        dataSource: MostViewedDataSource,
        state: MostViewedCardUiState
    ) {
        when (dataSource) {
            MostViewedDataSource.POSTS_AND_PAGES ->
                _postsUiState.value = state
            MostViewedDataSource.REFERRERS ->
                _referrersUiState.value = state
        }
    }

    private fun setErrorState(
        dataSource: MostViewedDataSource,
        message: String
    ) {
        setUiState(
            dataSource,
            MostViewedCardUiState.Error(message = message)
        )
    }

    private fun clearLoadingPeriod(dataSource: MostViewedDataSource) {
        when (dataSource) {
            MostViewedDataSource.POSTS_AND_PAGES ->
                postsLoadingPeriod = null
            MostViewedDataSource.REFERRERS ->
                referrersLoadingPeriod = null
        }
    }

    private fun setLoadedPeriod(
        dataSource: MostViewedDataSource,
        period: StatsPeriod
    ) {
        when (dataSource) {
            MostViewedDataSource.POSTS_AND_PAGES ->
                postsLoadedPeriod = period
            MostViewedDataSource.REFERRERS ->
                referrersLoadedPeriod = period
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
