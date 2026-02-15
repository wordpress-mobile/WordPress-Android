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
import org.wordpress.android.ui.newstats.repository.HourlyViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TodayAggregatesResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private const val PREVIOUS_PERIOD_OFFSET_DAYS = 1

@HiltViewModel
class TodaysStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<TodaysStatsCardUiState>(TodaysStatsCardUiState.Loading)
    val uiState: StateFlow<TodaysStatsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            loadDataInternal(site)
            _isRefreshing.value = false
        }
    }

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _uiState.value = TodaysStatsCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_no_site_selected),
                onRetry = ::loadData
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            _uiState.value = TodaysStatsCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load),
                onRetry = ::loadData
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = TodaysStatsCardUiState.Loading

        viewModelScope.launch {
            loadDataInternal(site)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(site: SiteModel) {
        try {
            // Fetch all data in parallel for better performance
            val (todayStats, chartData) = coroutineScope {
                val todayStatsDeferred = async { fetchTodayStats(site) }
                val chartDataDeferred = async { fetchChartData(site) }
                todayStatsDeferred.await() to chartDataDeferred.await()
            }

            if (todayStats != null) {
                _uiState.value = TodaysStatsCardUiState.Loaded(
                    views = todayStats.views,
                    visitors = todayStats.visitors,
                    likes = todayStats.likes,
                    comments = todayStats.comments,
                    chartData = chartData,
                    onCardClick = { onCardClicked() }
                )
            } else {
                _uiState.value = TodaysStatsCardUiState.Error(
                    message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load),
                    onRetry = ::loadData
                )
            }
        } catch (e: Exception) {
            _uiState.value = TodaysStatsCardUiState.Error(
                message = e.message ?: resourceProvider.getString(R.string.stats_todays_stats_unknown_error),
                onRetry = ::loadData
            )
        }
    }

    private suspend fun fetchTodayStats(site: SiteModel): TodayStatsData? {
        val result = statsRepository.fetchTodayAggregates(site.siteId)
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

    private suspend fun fetchChartData(site: SiteModel): ChartData = coroutineScope {
        // Fetch both periods in parallel
        val currentPeriodDeferred = async { fetchHourlyData(site, offsetDays = 0) }
        val previousPeriodDeferred = async {
            fetchHourlyData(site, offsetDays = PREVIOUS_PERIOD_OFFSET_DAYS)
        }

        ChartData(
            currentPeriod = currentPeriodDeferred.await(),
            previousPeriod = previousPeriodDeferred.await()
        )
    }

    private suspend fun fetchHourlyData(
        site: SiteModel,
        offsetDays: Int
    ): List<ViewsDataPoint> {
        val result = statsRepository.fetchHourlyViews(
            siteId = site.siteId,
            offsetDays = offsetDays
        )

        return when (result) {
            is HourlyViewsResult.Success -> {
                result.dataPoints.map { dataPoint ->
                    ViewsDataPoint(
                        label = formatHourlyLabel(dataPoint.period),
                        views = dataPoint.views
                    )
                }
            }
            is HourlyViewsResult.Error -> emptyList()
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun formatHourlyLabel(period: String): String {
        return try {
            // API returns period in format "2024-01-16 14:00:00" for hourly data
            val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = DateTimeFormatter.ofPattern("ha", Locale.getDefault())
            val dateTime = LocalDateTime.parse(period, inputFormat)
            dateTime.format(outputFormat).lowercase()
        } catch (e: Exception) {
            // Fallback: try parsing just the hour if full format fails
            try {
                val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
                val outputFormat = DateTimeFormatter.ofPattern("ha", Locale.getDefault())
                val dateTime = LocalDateTime.parse(period, inputFormat)
                dateTime.format(outputFormat).lowercase()
            } catch (e2: Exception) {
                period
            }
        }
    }

    private fun onCardClicked() {
        // Navigation will be handled by the parent
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
}
