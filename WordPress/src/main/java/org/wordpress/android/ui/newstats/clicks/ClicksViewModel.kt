package org.wordpress.android.ui.newstats.clicks

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
import org.wordpress.android.ui.newstats.repository.ClickItemData
import org.wordpress.android.ui.newstats.repository.ClicksResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class ClicksViewModel @Inject constructor(
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
    private var cachedTotalClicks: Long = 0L
    private var cachedTotalClicksChange: Long = 0L
    private var cachedTotalClicksChangePercent: Double = 0.0

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
                fetchClicks(site)
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
                fetchClicks(site)
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
            cardType = StatsCardType.CLICKS,
            items = allItems,
            totalViews = cachedTotalClicks,
            totalViewsChange = cachedTotalClicksChange,
            totalViewsChangePercent = cachedTotalClicksChangePercent,
            dateRange = currentPeriod.toDateRangeString(
                resourceProvider
            )
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchClicks(site: SiteModel) {
        val siteId = site.siteId

        try {
            when (val result = statsRepository.fetchClicks(
                siteId, currentPeriod
            )) {
                is ClicksResult.Success -> {
                    loadedPeriod = currentPeriod
                    cachedTotalClicks = result.totalClicks
                    cachedTotalClicksChange =
                        result.totalClicksChange
                    cachedTotalClicksChangePercent =
                        result.totalClicksChangePercent

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
                is ClicksResult.Error -> {
                    _uiState.value = MostViewedCardUiState.Error(
                        resourceProvider.getString(
                            result.messageResId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Error fetching clicks", e)
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(R.string.stats_error_unknown)
            )
        }
    }

    private fun ClickItemData.toCardItem(id: Long): MostViewedItem {
        val change = toChange()
        return MostViewedItem(
            id = id,
            title = name,
            views = clicks,
            change = change
        )
    }

    private fun ClickItemData.toDetailItem(
        id: Long
    ): MostViewedDetailItem {
        val change = toChange()
        return MostViewedDetailItem(
            id = id,
            title = name,
            views = clicks,
            change = change
        )
    }

    private fun ClickItemData.toChange(): MostViewedChange {
        return when {
            clicksChange > 0 -> MostViewedChange.Positive(
                clicksChange, abs(clicksChangePercent)
            )
            clicksChange < 0 -> MostViewedChange.Negative(
                abs(clicksChange), abs(clicksChangePercent)
            )
            else -> MostViewedChange.NoChange
        }
    }
}
