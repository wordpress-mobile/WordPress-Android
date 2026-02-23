package org.wordpress.android.ui.newstats.videoplays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.mostviewed.MostViewedCardUiState
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailData
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDetailItem
import org.wordpress.android.ui.newstats.mostviewed.MostViewedItem
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.VideoPlayItemData
import org.wordpress.android.ui.newstats.repository.VideoPlaysResult
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class VideoPlaysViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<MostViewedCardUiState>(MostViewedCardUiState.Loading)
    val uiState: StateFlow<MostViewedCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null

    private var allItems: List<MostViewedDetailItem> = emptyList()
    private var cachedTotalViews: Long = 0L
    private var cachedTotalViewsChange: Long = 0L
    private var cachedTotalViewsChangePercent: Double = 0.0

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(R.string.stats_error_no_site)
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(R.string.stats_error_api)
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = MostViewedCardUiState.Loading

        viewModelScope.launch {
            try {
                fetchVideoPlays(site)
            } finally {
                loadingPeriod = null
            }
        }
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        statsRepository.init(accessToken)
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                fetchVideoPlays(site)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onRetry() {
        loadData()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        if (loadedPeriod == period || loadingPeriod == period) return
        loadingPeriod = period
        currentPeriod = period
        loadData()
    }

    fun getDetailData(): MostViewedDetailData {
        return MostViewedDetailData(
            cardType = StatsCardType.VIDEO_PLAYS,
            items = allItems,
            totalViews = cachedTotalViews,
            totalViewsChange = cachedTotalViewsChange,
            totalViewsChangePercent = cachedTotalViewsChangePercent,
            dateRange = currentPeriod.toDateRangeString(
                resourceProvider
            )
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchVideoPlays(site: SiteModel) {
        val siteId = site.siteId

        try {
            when (val result = statsRepository.fetchVideoPlays(
                siteId, currentPeriod
            )) {
                is VideoPlaysResult.Success -> {
                    loadedPeriod = currentPeriod
                    cachedTotalViews = result.totalViews
                    cachedTotalViewsChange =
                        result.totalViewsChange
                    cachedTotalViewsChangePercent =
                        result.totalViewsChangePercent

                    if (result.items.isEmpty()) {
                        allItems = emptyList()
                        _uiState.value = MostViewedCardUiState.Loaded(
                            items = emptyList(),
                            maxViewsForBar = 0
                        )
                    } else {
                        val detailItems = result.items.mapIndexed {
                            index, item ->
                            item.toDetailItem(index.toLong())
                        }
                        allItems = detailItems

                        val cardItems = result.items
                            .take(CARD_MAX_ITEMS)
                            .mapIndexed { index, item ->
                                item.toCardItem(index.toLong())
                            }
                        val maxForBar =
                            cardItems.firstOrNull()?.views ?: 0L

                        _uiState.value = MostViewedCardUiState.Loaded(
                            items = cardItems,
                            maxViewsForBar = maxForBar
                        )
                    }
                }
                is VideoPlaysResult.Error -> {
                    _uiState.value = MostViewedCardUiState.Error(
                        resourceProvider.getString(
                            result.messageResId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS, "Error fetching video plays", e
            )
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(R.string.stats_error_unknown)
            )
        }
    }

    private fun VideoPlayItemData.toCardItem(
        id: Long
    ): MostViewedItem {
        val change = toChange()
        return MostViewedItem(
            id = id,
            title = title,
            views = views,
            change = change
        )
    }

    private fun VideoPlayItemData.toDetailItem(
        id: Long
    ): MostViewedDetailItem {
        val change = toChange()
        return MostViewedDetailItem(
            id = id,
            title = title,
            views = views,
            change = change
        )
    }

    private fun VideoPlayItemData.toChange(): MostViewedChange {
        return when {
            viewsChange > 0 -> MostViewedChange.Positive(
                viewsChange, abs(viewsChangePercent)
            )
            viewsChange < 0 -> MostViewedChange.Negative(
                abs(viewsChange), abs(viewsChangePercent)
            )
            else -> MostViewedChange.NoChange
        }
    }
}
