package org.wordpress.android.ui.newstats.todaysstat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

private const val CHART_DATA_POINTS = 7
private const val PREVIOUS_PERIOD_OFFSET_DAYS = 7

@HiltViewModel
class TodaysStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val todayInsightsStore: TodayInsightsStore,
    private val visitsAndViewsStore: VisitsAndViewsStore
) : ViewModel() {
    private val _uiState = MutableStateFlow<TodaysStatsCardUiState>(TodaysStatsCardUiState.Loading)
    val uiState: StateFlow<TodaysStatsCardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(forced: Boolean = false) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _uiState.value = TodaysStatsCardUiState.Error(
                message = "No site selected",
                onRetry = { loadData(forced = true) }
            )
            return
        }

        _uiState.value = TodaysStatsCardUiState.Loading

        viewModelScope.launch {
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
                        message = "Failed to load stats",
                        onRetry = { loadData(forced = true) }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TodaysStatsCardUiState.Error(
                    message = e.message ?: "Unknown error",
                    onRetry = { loadData(forced = true) }
                )
            }
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
        val currentPeriodData = fetchPeriodData(site, forced, offsetDays = 0)
        val previousPeriodData = fetchPeriodData(site, forced, offsetDays = PREVIOUS_PERIOD_OFFSET_DAYS)

        return ChartData(
            currentPeriod = currentPeriodData,
            previousPeriod = previousPeriodData
        )
    }

    private suspend fun fetchPeriodData(
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
            granularity = StatsGranularity.DAYS,
            limitMode = LimitMode.Top(CHART_DATA_POINTS),
            date = calendar.time,
            forced = forced
        )

        val model = response.model
        if (response.isError || model == null) {
            return emptyList()
        }

        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())

        return model.dates.map { periodData ->
            ViewsDataPoint(
                label = formatPeriodLabel(periodData.period, dateFormat),
                views = periodData.views
            )
        }
    }

    private fun formatPeriodLabel(period: String, dateFormat: SimpleDateFormat): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(period)
            date?.let { dateFormat.format(it) } ?: period
        } catch (e: Exception) {
            period
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
