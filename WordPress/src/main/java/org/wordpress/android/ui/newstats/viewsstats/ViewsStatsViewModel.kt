package org.wordpress.android.ui.newstats.viewsstats

import androidx.lifecycle.SavedStateHandle
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
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.PeriodStatsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.PeriodAggregates
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

private const val PERCENTAGE_BASE = 100.0
private const val DAYS_THRESHOLD_FOR_MONTHLY_DISPLAY = 30

private val HOURLY_FORMAT_REGEX = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")
private val DAILY_FORMAT_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
private val MONTHLY_FORMAT_REGEX = Regex("""\d{4}-\d{2}""")

private const val KEY_PERIOD_TYPE = "period_type"
private const val KEY_CUSTOM_START_DATE = "custom_start_date"
private const val KEY_CUSTOM_END_DATE = "custom_end_date"

private const val PERIOD_TODAY = "today"
private const val PERIOD_LAST_7_DAYS = "last_7_days"
private const val PERIOD_LAST_30_DAYS = "last_30_days"
private const val PERIOD_LAST_6_MONTHS = "last_6_months"
private const val PERIOD_LAST_12_MONTHS = "last_12_months"
private const val PERIOD_CUSTOM = "custom"

@HiltViewModel
class ViewsStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow<ViewsStatsCardUiState>(ViewsStatsCardUiState.Loading)
    val uiState: StateFlow<ViewsStatsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(restorePeriod())
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    private var currentChartType: ChartType = ChartType.LINE
    private var currentPeriod: StatsPeriod = _selectedPeriod.value

    init {
        loadData()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        if (period == currentPeriod) return
        currentPeriod = period
        _selectedPeriod.value = period
        savePeriod(period)
        loadData()
    }

    private fun savePeriod(period: StatsPeriod) {
        when (period) {
            is StatsPeriod.Today -> savedStateHandle[KEY_PERIOD_TYPE] = PERIOD_TODAY
            is StatsPeriod.Last7Days -> savedStateHandle[KEY_PERIOD_TYPE] = PERIOD_LAST_7_DAYS
            is StatsPeriod.Last30Days -> savedStateHandle[KEY_PERIOD_TYPE] = PERIOD_LAST_30_DAYS
            is StatsPeriod.Last6Months -> savedStateHandle[KEY_PERIOD_TYPE] = PERIOD_LAST_6_MONTHS
            is StatsPeriod.Last12Months -> savedStateHandle[KEY_PERIOD_TYPE] = PERIOD_LAST_12_MONTHS
            is StatsPeriod.Custom -> {
                savedStateHandle[KEY_PERIOD_TYPE] = PERIOD_CUSTOM
                savedStateHandle[KEY_CUSTOM_START_DATE] = period.startDate.toEpochDay()
                savedStateHandle[KEY_CUSTOM_END_DATE] = period.endDate.toEpochDay()
            }
        }
    }

    private fun restorePeriod(): StatsPeriod {
        return when (savedStateHandle.get<String>(KEY_PERIOD_TYPE)) {
            PERIOD_TODAY -> StatsPeriod.Today
            PERIOD_LAST_7_DAYS -> StatsPeriod.Last7Days
            PERIOD_LAST_30_DAYS -> StatsPeriod.Last30Days
            PERIOD_LAST_6_MONTHS -> StatsPeriod.Last6Months
            PERIOD_LAST_12_MONTHS -> StatsPeriod.Last12Months
            PERIOD_CUSTOM -> {
                val startEpochDay = savedStateHandle.get<Long>(KEY_CUSTOM_START_DATE)
                val endEpochDay = savedStateHandle.get<Long>(KEY_CUSTOM_END_DATE)
                if (startEpochDay != null && endEpochDay != null) {
                    StatsPeriod.Custom(
                        LocalDate.ofEpochDay(startEpochDay),
                        LocalDate.ofEpochDay(endEpochDay)
                    )
                } else {
                    StatsPeriod.Last7Days
                }
            }
            else -> StatsPeriod.Last7Days
        }
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
            val result = statsRepository.fetchStatsForPeriod(site.siteId, currentPeriod)

            when (result) {
                is PeriodStatsResult.Success -> {
                    _uiState.value = buildLoadedState(result)
                }
                is PeriodStatsResult.Error -> {
                    _uiState.value = ViewsStatsCardUiState.Error(
                        message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = ViewsStatsCardUiState.Error(
                message = e.message ?: resourceProvider.getString(R.string.stats_todays_stats_unknown_error)
            )
        }
    }

    private fun buildLoadedState(result: PeriodStatsResult.Success): ViewsStatsCardUiState.Loaded {
        val currentStats = result.currentAggregates
        val previousStats = result.previousAggregates
        val currentDataPoints = result.currentPeriodData
            .map { ChartDataPoint(formatDataPointLabel(it.period, currentPeriod), it.views) }
        val previousDataPoints = result.previousPeriodData
            .map { ChartDataPoint(formatDataPointLabel(it.period, currentPeriod), it.views) }

        val average = if (currentDataPoints.isNotEmpty()) {
            currentStats.views / currentDataPoints.size
        } else {
            if (currentStats.views > 0) {
                AppLog.w(
                    AppLog.T.STATS,
                    "Data inconsistency: no data points but views=${currentStats.views}"
                )
            }
            0L
        }

        return ViewsStatsCardUiState.Loaded(
            currentPeriodViews = currentStats.views,
            previousPeriodViews = previousStats.views,
            viewsDifference = currentStats.views - previousStats.views,
            viewsPercentageChange = calculatePercentageChange(currentStats.views, previousStats.views),
            currentPeriodDateRange = formatDateRangeForPeriod(
                currentStats.startDate,
                currentStats.endDate,
                currentPeriod
            ),
            previousPeriodDateRange = formatDateRangeForPeriod(
                previousStats.startDate,
                previousStats.endDate,
                currentPeriod
            ),
            chartData = ViewsStatsChartData(currentPeriod = currentDataPoints, previousPeriod = previousDataPoints),
            periodAverage = average,
            bottomStats = buildBottomStats(currentStats, previousStats),
            chartType = currentChartType
        )
    }

    private fun buildBottomStats(
        currentPeriod: PeriodAggregates,
        previousPeriod: PeriodAggregates
    ): List<StatItem> {
        return listOf(
            StatItem(
                label = resourceProvider.getString(R.string.stats_views),
                value = currentPeriod.views,
                change = calculateStatChange(currentPeriod.views, previousPeriod.views)
            ),
            StatItem(
                label = resourceProvider.getString(R.string.stats_visitors),
                value = currentPeriod.visitors,
                change = calculateStatChange(currentPeriod.visitors, previousPeriod.visitors)
            ),
            StatItem(
                label = resourceProvider.getString(R.string.stats_likes),
                value = currentPeriod.likes,
                change = calculateStatChange(currentPeriod.likes, previousPeriod.likes)
            ),
            StatItem(
                label = resourceProvider.getString(R.string.stats_comments),
                value = currentPeriod.comments,
                change = calculateStatChange(currentPeriod.comments, previousPeriod.comments)
            ),
            StatItem(
                label = resourceProvider.getString(R.string.posts),
                value = currentPeriod.posts,
                change = calculateStatChange(currentPeriod.posts, previousPeriod.posts)
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

    private fun formatDataPointLabel(period: String, statsPeriod: StatsPeriod): String {
        val isMonthlyPeriod = statsPeriod is StatsPeriod.Last6Months ||
            statsPeriod is StatsPeriod.Last12Months ||
            (statsPeriod is StatsPeriod.Custom && isCustomPeriodMonthly(statsPeriod))

        return when {
            period.matches(HOURLY_FORMAT_REGEX) -> formatHourlyLabel(period)
            period.matches(DAILY_FORMAT_REGEX) -> formatDailyLabel(period, isMonthlyPeriod)
            period.matches(MONTHLY_FORMAT_REGEX) -> formatMonthlyLabel(period)
            else -> period
        }
    }

    private fun formatHourlyLabel(period: String): String {
        val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        return LocalDateTime.parse(period, inputFormat).format(outputFormat)
    }

    private fun formatDailyLabel(period: String, showMonthOnly: Boolean): String {
        val date = LocalDate.parse(period, DateTimeFormatter.ISO_LOCAL_DATE)
        val pattern = if (showMonthOnly) "MMM" else "MMM d"
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    private fun formatMonthlyLabel(period: String): String {
        val parts = period.split("-")
        val date = LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
        return date.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
    }

    private fun isCustomPeriodMonthly(custom: StatsPeriod.Custom): Boolean {
        val daysBetween = ChronoUnit.DAYS.between(custom.startDate, custom.endDate) + 1
        return daysBetween > DAYS_THRESHOLD_FOR_MONTHLY_DISPLAY
    }

    private fun formatDateRangeForPeriod(startDate: String, endDate: String, period: StatsPeriod): String {
        return when (period) {
            is StatsPeriod.Today -> formatSingleDayRange(endDate)
            is StatsPeriod.Last6Months, is StatsPeriod.Last12Months -> formatMonthRange(startDate, endDate)
            is StatsPeriod.Custom -> {
                if (isCustomPeriodMonthly(period)) formatMonthRange(startDate, endDate)
                else formatDayRange(startDate, endDate)
            }
            else -> formatDayRange(startDate, endDate)
        }
    }

    private fun parseDate(dateString: String): LocalDate? {
        return if (dateString.matches(DAILY_FORMAT_REGEX)) {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            null
        }
    }

    private fun formatSingleDayRange(date: String): String {
        val parsedDate = parseDate(date) ?: return date
        return parsedDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }

    @Suppress("ReturnCount")
    private fun formatMonthRange(startDate: String, endDate: String): String {
        val start = parseDate(startDate) ?: return "$startDate - $endDate"
        val end = parseDate(endDate) ?: return "$startDate - $endDate"
        val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

        return if (start.month == end.month && start.year == end.year) {
            start.format(monthFormat)
        } else {
            "${start.format(monthFormat)} - ${end.format(monthFormat)}"
        }
    }

    @Suppress("ReturnCount")
    private fun formatDayRange(startDate: String, endDate: String): String {
        val start = parseDate(startDate) ?: return "$startDate - $endDate"
        val end = parseDate(endDate) ?: return "$startDate - $endDate"
        val dayFormat = DateTimeFormatter.ofPattern("d", Locale.getDefault())
        val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

        return if (start.month == end.month) {
            "${start.format(dayFormat)}-${end.format(dayMonthFormat)}"
        } else {
            "${start.format(dayMonthFormat)} - ${end.format(dayMonthFormat)}"
        }
    }

    fun onRetry() {
        loadData()
    }
}
