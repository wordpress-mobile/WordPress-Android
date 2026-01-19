package org.wordpress.android.ui.newstats.todaysstat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ResourceProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

private const val HOURLY_DATA_POINTS = 24
private const val PREVIOUS_PERIOD_OFFSET_DAYS = 1

@HiltViewModel
class TodaysStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val todayInsightsStore: TodayInsightsStore,
    private val visitsAndViewsStore: VisitsAndViewsStore,
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
        viewModelScope.launch {
            _isRefreshing.value = true
            loadDataInternal(forced = true)
            _isRefreshing.value = false
        }
    }

    fun loadData(forced: Boolean = false) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _uiState.value = TodaysStatsCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_no_site_selected),
                onRetry = { loadData(forced = true) }
            )
            return
        }

        _uiState.value = TodaysStatsCardUiState.Loading

        viewModelScope.launch {
            loadDataInternal(forced)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(forced: Boolean) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _uiState.value = TodaysStatsCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_no_site_selected),
                onRetry = { loadData(forced = true) }
            )
            return
        }

        try {
            val todayStats = fetchTodayStats(site, forced)
            val chartData = fetchChartData(site, forced)

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
                    onRetry = { loadData(forced = true) }
                )
            }
        } catch (e: Exception) {
            _uiState.value = TodaysStatsCardUiState.Error(
                message = e.message ?: resourceProvider.getString(R.string.stats_todays_stats_unknown_error),
                onRetry = { loadData(forced = true) }
            )
        }
    }

    private suspend fun fetchTodayStats(site: SiteModel, forced: Boolean): TodayStatsData? {
        val response = todayInsightsStore.fetchTodayInsights(site, forced)
        return if (response.isError) {
            null
        } else {
            response.model?.let { model ->
                TodayStatsData(
                    views = model.views,
                    visitors = model.visitors,
                    likes = model.likes,
                    comments = model.comments
                )
            }
        }
    }

    private suspend fun fetchChartData(site: SiteModel, forced: Boolean): ChartData {
        val currentPeriodData = fetchHourlyData(site, forced, offsetDays = 0)
        val previousPeriodData = fetchHourlyData(site, forced, offsetDays = PREVIOUS_PERIOD_OFFSET_DAYS)

        return ChartData(
            currentPeriod = currentPeriodData,
            previousPeriod = previousPeriodData
        )
    }

    private suspend fun fetchHourlyData(
        site: SiteModel,
        forced: Boolean,
        offsetDays: Int
    ): List<ViewsDataPoint> {
        val calendar = Calendar.getInstance()
        if (offsetDays > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -offsetDays)
        }

        val response = visitsAndViewsStore.fetchVisits(
            site = site,
            granularity = StatsGranularity.HOURS,
            limitMode = LimitMode.Top(HOURLY_DATA_POINTS),
            date = calendar.time,
            forced = forced
        )

        val model = response.model
        if (response.isError || model == null) {
            return emptyList()
        }

        return model.dates.map { periodData ->
            ViewsDataPoint(
                label = formatHourlyLabel(periodData.period),
                views = periodData.views
            )
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun formatHourlyLabel(period: String): String {
        return try {
            // API returns period in format "2024-01-16 14:00:00" for hourly data
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("ha", Locale.getDefault())
            val date = inputFormat.parse(period)
            date?.let { outputFormat.format(it).lowercase() } ?: period
        } catch (e: Exception) {
            // Fallback: try parsing just the hour if full format fails
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val outputFormat = SimpleDateFormat("ha", Locale.getDefault())
                val date = inputFormat.parse(period)
                date?.let { outputFormat.format(it).lowercase() } ?: period
            } catch (e2: Exception) {
                period
            }
        }
    }

    private fun onCardClicked() {
        // Navigation will be handled by the parent
    }

    fun onRetry() {
        loadData(forced = true)
    }

    private data class TodayStatsData(
        val views: Int,
        val visitors: Int,
        val likes: Int,
        val comments: Int
    )
}
