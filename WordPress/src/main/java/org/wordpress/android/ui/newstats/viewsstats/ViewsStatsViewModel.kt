package org.wordpress.android.ui.newstats.viewsstats

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
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.WeeklyAggregates
import org.wordpress.android.ui.newstats.repository.WeeklyStatsWithDailyDataResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

private const val CURRENT_WEEK = 0
private const val PREVIOUS_WEEK = 1
private const val PERCENTAGE_BASE = 100.0

@HiltViewModel
class ViewsStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<ViewsStatsCardUiState>(ViewsStatsCardUiState.Loading)
    val uiState: StateFlow<ViewsStatsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentChartType: ChartType = ChartType.LINE

    init {
        loadData()
    }

    fun onChartTypeChanged(chartType: ChartType) {
        currentChartType = chartType
        val currentState = _uiState.value
        if (currentState is ViewsStatsCardUiState.Loaded) {
            _uiState.value = currentState.copy(chartType = chartType)
        }
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
            _uiState.value = ViewsStatsCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_no_site_selected)
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            _uiState.value = ViewsStatsCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = ViewsStatsCardUiState.Loading

        viewModelScope.launch {
            loadDataInternal(site)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(site: SiteModel) {
        try {
            val (currentWeekResult, previousWeekResult) = fetchWeeklyData(site.siteId)
            val currentWeekStats = (currentWeekResult as? WeeklyStatsWithDailyDataResult.Success)?.aggregates
            val previousWeekStats = (previousWeekResult as? WeeklyStatsWithDailyDataResult.Success)?.aggregates

            if (currentWeekStats != null && previousWeekStats != null) {
                _uiState.value = buildLoadedState(
                    currentWeekResult as WeeklyStatsWithDailyDataResult.Success,
                    previousWeekResult as WeeklyStatsWithDailyDataResult.Success
                )
            } else {
                _uiState.value = ViewsStatsCardUiState.Error(
                    message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
                )
            }
        } catch (e: Exception) {
            _uiState.value = ViewsStatsCardUiState.Error(
                message = e.message ?: resourceProvider.getString(R.string.stats_todays_stats_unknown_error)
            )
        }
    }

    private suspend fun fetchWeeklyData(
        siteId: Long
    ): Pair<WeeklyStatsWithDailyDataResult, WeeklyStatsWithDailyDataResult> = coroutineScope {
        val currentWeekDeferred = async {
            statsRepository.fetchWeeklyStatsWithDailyData(siteId, CURRENT_WEEK)
        }
        val previousWeekDeferred = async {
            statsRepository.fetchWeeklyStatsWithDailyData(siteId, PREVIOUS_WEEK)
        }
        currentWeekDeferred.await() to previousWeekDeferred.await()
    }

    private fun buildLoadedState(
        currentWeekResult: WeeklyStatsWithDailyDataResult.Success,
        previousWeekResult: WeeklyStatsWithDailyDataResult.Success
    ): ViewsStatsCardUiState.Loaded {
        val currentWeekStats = currentWeekResult.aggregates
        val previousWeekStats = previousWeekResult.aggregates
        val currentWeekDailyViews = currentWeekResult.dailyDataPoints
            .map { DailyDataPoint(formatDayLabel(it.period), it.views) }
        val previousWeekDailyViews = previousWeekResult.dailyDataPoints
            .map { DailyDataPoint(formatDayLabel(it.period), it.views) }

        val weeklyAverage = if (currentWeekDailyViews.isNotEmpty()) {
            currentWeekStats.views / currentWeekDailyViews.size
        } else {
            0L
        }

        return ViewsStatsCardUiState.Loaded(
            currentWeekViews = currentWeekStats.views,
            previousWeekViews = previousWeekStats.views,
            viewsDifference = currentWeekStats.views - previousWeekStats.views,
            viewsPercentageChange = calculatePercentageChange(currentWeekStats.views, previousWeekStats.views),
            currentWeekDateRange = formatDateRange(currentWeekStats.startDate, currentWeekStats.endDate),
            previousWeekDateRange = formatDateRange(previousWeekStats.startDate, previousWeekStats.endDate),
            chartData = ViewsStatsChartData(currentWeek = currentWeekDailyViews, previousWeek = previousWeekDailyViews),
            weeklyAverage = weeklyAverage,
            bottomStats = buildBottomStats(currentWeekStats, previousWeekStats),
            chartType = currentChartType
        )
    }

    private fun buildBottomStats(
        currentWeek: WeeklyAggregates,
        previousWeek: WeeklyAggregates
    ): List<StatItem> {
        return listOf(
            StatItem(
                label = resourceProvider.getString(R.string.stats_views),
                value = currentWeek.views,
                change = calculateStatChange(currentWeek.views, previousWeek.views)
            ),
            StatItem(
                label = resourceProvider.getString(R.string.stats_visitors),
                value = currentWeek.visitors,
                change = calculateStatChange(currentWeek.visitors, previousWeek.visitors)
            ),
            StatItem(
                label = resourceProvider.getString(R.string.stats_likes),
                value = currentWeek.likes,
                change = calculateStatChange(currentWeek.likes, previousWeek.likes)
            ),
            StatItem(
                label = resourceProvider.getString(R.string.stats_comments),
                value = currentWeek.comments,
                change = calculateStatChange(currentWeek.comments, previousWeek.comments)
            ),
            StatItem(
                label = resourceProvider.getString(R.string.posts),
                value = currentWeek.posts,
                change = calculateStatChange(currentWeek.posts, previousWeek.posts)
            )
        )
    }

    private fun calculateStatChange(current: Long, previous: Long): StatChange {
        if (current == previous) return StatChange.NoChange

        val percentage = calculatePercentageChange(current, previous)
        return if (current > previous) {
            StatChange.Positive(percentage)
        } else {
            StatChange.Negative(abs(percentage))
        }
    }

    private fun calculatePercentageChange(current: Long, previous: Long): Double {
        if (previous == 0L) return if (current > 0) PERCENTAGE_BASE else 0.0
        return ((current - previous).toDouble() / previous) * PERCENTAGE_BASE
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun formatDayLabel(period: String): String {
        return try {
            val date = LocalDate.parse(period, DateTimeFormatter.ISO_LOCAL_DATE)
            val outputFormat = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            date.format(outputFormat)
        } catch (e: Exception) {
            period
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun formatDateRange(startDate: String, endDate: String): String {
        return try {
            val start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val dayFormat = DateTimeFormatter.ofPattern("d", Locale.getDefault())
            val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

            if (start.month == end.month) {
                "${start.format(dayFormat)}-${end.format(dayMonthFormat)}"
            } else {
                "${start.format(dayMonthFormat)} - ${end.format(dayMonthFormat)}"
            }
        } catch (e: Exception) {
            "$startDate - $endDate"
        }
    }

    fun onRetry() {
        loadData()
    }
}
