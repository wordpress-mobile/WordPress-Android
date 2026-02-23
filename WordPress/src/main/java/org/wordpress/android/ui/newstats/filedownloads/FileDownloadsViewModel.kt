package org.wordpress.android.ui.newstats.filedownloads

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
import org.wordpress.android.ui.newstats.repository.FileDownloadItemData
import org.wordpress.android.ui.newstats.repository.FileDownloadsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class FileDownloadsViewModel @Inject constructor(
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
    private var cachedTotalDownloads: Long = 0L
    private var cachedTotalDownloadsChange: Long = 0L
    private var cachedTotalDownloadsChangePercent: Double = 0.0

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
                fetchFileDownloads(site)
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
                fetchFileDownloads(site)
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
            cardType = StatsCardType.FILE_DOWNLOADS,
            items = allItems,
            totalViews = cachedTotalDownloads,
            totalViewsChange = cachedTotalDownloadsChange,
            totalViewsChangePercent =
                cachedTotalDownloadsChangePercent,
            dateRange = currentPeriod.toDateRangeString(
                resourceProvider
            )
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchFileDownloads(site: SiteModel) {
        val siteId = site.siteId

        try {
            when (val result = statsRepository.fetchFileDownloads(
                siteId, currentPeriod
            )) {
                is FileDownloadsResult.Success -> {
                    loadedPeriod = currentPeriod
                    cachedTotalDownloads = result.totalDownloads
                    cachedTotalDownloadsChange =
                        result.totalDownloadsChange
                    cachedTotalDownloadsChangePercent =
                        result.totalDownloadsChangePercent

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
                is FileDownloadsResult.Error -> {
                    _uiState.value = MostViewedCardUiState.Error(
                        resourceProvider.getString(
                            result.messageResId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching file downloads",
                e
            )
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(R.string.stats_error_unknown)
            )
        }
    }

    private fun FileDownloadItemData.toCardItem(
        id: Long
    ): MostViewedItem {
        val change = toChange()
        return MostViewedItem(
            id = id,
            title = name,
            views = downloads,
            change = change
        )
    }

    private fun FileDownloadItemData.toDetailItem(
        id: Long
    ): MostViewedDetailItem {
        val change = toChange()
        return MostViewedDetailItem(
            id = id,
            title = name,
            views = downloads,
            change = change
        )
    }

    private fun FileDownloadItemData.toChange(): MostViewedChange {
        return when {
            downloadsChange > 0 -> MostViewedChange.Positive(
                downloadsChange, abs(downloadsChangePercent)
            )
            downloadsChange < 0 -> MostViewedChange.Negative(
                abs(downloadsChange), abs(downloadsChangePercent)
            )
            else -> MostViewedChange.NoChange
        }
    }
}
