package org.wordpress.android.ui.newstats.todaysstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.HourlyViewsDataPoint
import org.wordpress.android.ui.newstats.repository.HourlyViewsResult
import org.wordpress.android.ui.newstats.repository.StatsCacheBucket
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TodayAggregatesResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val PREVIOUS_PERIOD_OFFSET_DAYS = 1

@HiltViewModel
class TodaysStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider,
    private val clock: Clock
) : ViewModel() {
    private val _uiState = MutableStateFlow<TodaysStatsCardUiState>(TodaysStatsCardUiState.Loading)
    val uiState: StateFlow<TodaysStatsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var isLoading = false
    private var isLoadedSuccessfully = false

    fun loadDataIfNeeded() {
        if (isLoadedSuccessfully || isLoading) return
        isLoading = true
        loadData()
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                loadDataInternal(site, forceRefresh = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            isLoading = false
            _uiState.value = TodaysStatsCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_no_site
                ),
                onRetry = ::loadData
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            isLoading = false
            _uiState.value = TodaysStatsCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_api
                ),
                onRetry = ::loadData
            )
            return
        }

        // Today's numbers already in memory repaint in the same frame, so the placeholder would
        // only flash a skeleton over data the card is about to render anyway.
        if (!statsRepository.isCached(StatsCacheBucket.TODAY_AGGREGATES, site.siteId)) {
            _uiState.value = TodaysStatsCardUiState.Loading
        }

        viewModelScope.launch {
            try {
                loadDataInternal(site)
                revalidateIfNeeded(site)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Refreshes numbers served from a previous visit to the stats screen, once. The card already
     * shows them, so this runs without a loading state and keeps them if it fails.
     */
    private suspend fun revalidateIfNeeded(site: SiteModel) {
        val needsRevalidation =
            statsRepository.needsRevalidation(StatsCacheBucket.TODAY_AGGREGATES, site.siteId)
        if (!needsRevalidation) return
        loadDataInternal(site, forceRefresh = true, applyErrors = false)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(
        site: SiteModel,
        forceRefresh: Boolean = false,
        applyErrors: Boolean = true
    ) {
        try {
            // Fetch all data in parallel for better performance
            val (todayStats, chartData) = coroutineScope {
                val todayStatsDeferred = async { fetchTodayStats(site, forceRefresh) }
                val chartDataDeferred = async { fetchChartData(site, forceRefresh) }
                todayStatsDeferred.await() to chartDataDeferred.await()
            }

            if (todayStats != null) {
                isLoadedSuccessfully = true
                _uiState.value = TodaysStatsCardUiState.Loaded(
                    views = todayStats.views,
                    visitors = todayStats.visitors,
                    likes = todayStats.likes,
                    comments = todayStats.comments,
                    chartData = chartData
                )
            } else if (applyErrors) {
                _uiState.value = TodaysStatsCardUiState.Error(
                    message = resourceProvider.getString(R.string.stats_error_api),
                    onRetry = ::loadData
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!applyErrors) return
            _uiState.value = TodaysStatsCardUiState.Error(
                message = e.message ?: resourceProvider.getString(R.string.stats_error_unknown),
                onRetry = ::loadData
            )
        }
    }

    private suspend fun fetchTodayStats(site: SiteModel, forceRefresh: Boolean): TodayStatsData? {
        val result = statsRepository.fetchTodayAggregates(site.siteId, forceRefresh)
        return when (result) {
            is TodayAggregatesResult.Success -> {
                TodayStatsData(
                    views = result.aggregates.views,
                    visitors = result.aggregates.visitors,
                    likes = result.aggregates.likes,
                    comments = result.aggregates.comments
                )
            }
            is TodayAggregatesResult.Error -> null
        }
    }

    private suspend fun fetchChartData(site: SiteModel, forceRefresh: Boolean): ChartData = coroutineScope {
        // Fetch both periods in parallel
        val currentPeriodDeferred = async { fetchHourlyData(site, 0, forceRefresh) }
        val previousPeriodDeferred = async {
            fetchHourlyData(site, PREVIOUS_PERIOD_OFFSET_DAYS, forceRefresh)
        }

        ChartData(
            currentPeriod = currentPeriodDeferred.await(),
            previousPeriod = previousPeriodDeferred.await()
        )
    }

    private suspend fun fetchHourlyData(
        site: SiteModel,
        offsetDays: Int,
        forceRefresh: Boolean
    ): List<ViewsDataPoint> {
        val result = statsRepository.fetchHourlyViews(
            siteId = site.siteId,
            offsetDays = offsetDays,
            forceRefresh = forceRefresh
        )

        return when (result) {
            is HourlyViewsResult.Success -> {
                val dataPoints = if (offsetDays == 0) {
                    trimFutureHours(result.dataPoints)
                } else {
                    result.dataPoints
                }
                dataPoints.map { dataPoint ->
                    ViewsDataPoint(
                        label = formatHourlyLabel(dataPoint.period),
                        views = dataPoint.views
                    )
                }
            }
            is HourlyViewsResult.Error -> emptyList()
        }
    }

    /**
     * Filters out data points for hours after the current hour
     * to avoid showing a misleading drop to zero in the sparkline.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun trimFutureHours(
        dataPoints: List<HourlyViewsDataPoint>
    ): List<HourlyViewsDataPoint> {
        val currentHour = LocalDateTime.now(clock).hour
        return dataPoints.filter { dataPoint ->
            try {
                val dateTime = LocalDateTime.parse(
                    dataPoint.period,
                    HOURLY_PERIOD_FORMAT
                )
                dateTime.hour <= currentHour
            } catch (e: Exception) {
                true
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun formatHourlyLabel(period: String): String {
        return try {
            val dateTime = LocalDateTime.parse(
                period, HOURLY_PERIOD_FORMAT
            )
            dateTime.format(HOURLY_LABEL_FORMAT).lowercase()
        } catch (e: Exception) {
            // Fallback: try parsing without seconds
            try {
                val dateTime = LocalDateTime.parse(
                    period, HOURLY_PERIOD_SHORT_FORMAT
                )
                dateTime.format(HOURLY_LABEL_FORMAT).lowercase()
            } catch (e2: Exception) {
                period
            }
        }
    }

    fun onRetry() {
        loadData()
    }

    private data class TodayStatsData(
        val views: Long,
        val visitors: Long,
        val likes: Long,
        val comments: Long
    )

    companion object {
        private val HOURLY_PERIOD_FORMAT =
            DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm:ss", Locale.US
            )
        private val HOURLY_PERIOD_SHORT_FORMAT =
            DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm", Locale.US
            )
        private val HOURLY_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("ha", Locale.US)
    }
}
