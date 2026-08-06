package org.wordpress.android.ui.newstats.viewsstats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.repository.BottomStatsAggregates
import org.wordpress.android.ui.newstats.repository.BottomStatsResult
import org.wordpress.android.ui.newstats.repository.PeriodAggregates
import org.wordpress.android.ui.newstats.repository.PeriodStatsResult
import org.wordpress.android.ui.newstats.repository.StatsCardsConfigurationRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

private const val PERCENTAGE_BASE = 100.0
private const val DAYS_THRESHOLD_FOR_MONTHLY_DISPLAY = 31
private const val DATE_PREFIX_LENGTH = 10 // "YYYY-MM-DD".length

private val HOURLY_FORMAT_REGEX = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")
private val DAILY_FORMAT_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
private val MONTHLY_FORMAT_REGEX = Regex("""\d{4}-\d{2}""")

private const val KEY_CHART_TYPE = "chart_type"

@HiltViewModel
class ViewsStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider,
    private val savedStateHandle: SavedStateHandle,
    private val cardsConfigurationRepository: StatsCardsConfigurationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ViewsStatsCardUiState>(ViewsStatsCardUiState.Loading)
    val uiState: StateFlow<ViewsStatsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(restorePeriodFromSavedState())
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    private var currentChartType: ChartType = restoreChartTypeFromSavedState()
    private var currentPeriod: StatsPeriod = _selectedPeriod.value
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null

    private val _isPeriodInitialized = MutableStateFlow(false)
    val isPeriodInitialized: StateFlow<Boolean> = _isPeriodInitialized.asStateFlow()

    init {
        initializeWithPersistedPeriod()
    }

    /**
     * Initializes the ViewModel by restoring the period from persisted preferences asynchronously.
     * Data loading is deferred to [loadDataIfNeeded] which is called from the composable
     * only when the VIEWS_STATS card is visible.
     */
    private fun initializeWithPersistedPeriod() {
        viewModelScope.launch {
            // Try to restore from persisted preferences if SavedStateHandle didn't have a value
            val savedPeriodType = savedStateHandle.get<String>(KEY_PERIOD_TYPE)
            if (savedPeriodType == null) {
                val restoredPeriod = restorePeriodFromPreferences()
                if (restoredPeriod != null) {
                    currentPeriod = restoredPeriod
                    _selectedPeriod.value = restoredPeriod
                }
            }
            val savedChartType = savedStateHandle.get<String>(KEY_CHART_TYPE)
            if (savedChartType == null) {
                val restoredChartType = restoreChartTypeFromPreferences()
                if (restoredChartType != null) {
                    currentChartType = restoredChartType
                }
            }
            _isPeriodInitialized.value = true
        }
    }

    /**
     * Loads data only if it hasn't been loaded (or isn't currently loading)
     * for the current period. Called from the composable when the
     * VIEWS_STATS card is visible.
     */
    fun loadDataIfNeeded() {
        val targetPeriod = currentPeriod
        if (loadedPeriod == targetPeriod || loadingPeriod == targetPeriod) return
        loadingPeriod = targetPeriod
        loadData()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        if (period == currentPeriod) return
        currentPeriod = period
        _selectedPeriod.value = period
        savePeriod(period)
    }

    private fun savePeriod(period: StatsPeriod) {
        // Save to SavedStateHandle for immediate restoration
        savedStateHandle[KEY_PERIOD_TYPE] = period.toTypeString()
        if (period is StatsPeriod.Custom) {
            savedStateHandle[KEY_CUSTOM_START_DATE] = period.startDate.toEpochDay()
            savedStateHandle[KEY_CUSTOM_END_DATE] = period.endDate.toEpochDay()
        }

        // Persist to preferences for cross-session restoration
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId ?: return
        viewModelScope.launch {
            val config = cardsConfigurationRepository.getConfiguration(siteId)
            val periodType = period.toTypeString()
            val customStart = (period as? StatsPeriod.Custom)?.startDate?.toEpochDay()
            val customEnd = (period as? StatsPeriod.Custom)?.endDate?.toEpochDay()
            cardsConfigurationRepository.saveConfiguration(
                siteId,
                config.copy(
                    selectedPeriodType = periodType,
                    customPeriodStartDate = customStart,
                    customPeriodEndDate = customEnd
                )
            )
        }
    }

    /**
     * Restores period from SavedStateHandle only (fast, no disk I/O).
     * Used for property initialization.
     */
    private fun restorePeriodFromSavedState(): StatsPeriod {
        val savedPeriodType = savedStateHandle.get<String>(KEY_PERIOD_TYPE) ?: return StatsPeriod.Last7Days
        return StatsPeriod.fromTypeString(
            type = savedPeriodType,
            customStartEpochDay = savedStateHandle.get<Long>(KEY_CUSTOM_START_DATE),
            customEndEpochDay = savedStateHandle.get<Long>(KEY_CUSTOM_END_DATE)
        )
    }

    /**
     * Restores period from persisted preferences asynchronously.
     * Used for app restarts when SavedStateHandle doesn't have the value.
     */
    private suspend fun restorePeriodFromPreferences(): StatsPeriod? {
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId ?: return null
        val config = cardsConfigurationRepository.getConfiguration(siteId)
        return config.selectedPeriodType?.let { periodType ->
            StatsPeriod.fromTypeString(
                type = periodType,
                customStartEpochDay = config.customPeriodStartDate,
                customEndEpochDay = config.customPeriodEndDate
            )
        }
    }

    private fun restoreChartTypeFromSavedState(): ChartType {
        return ChartType.fromStorageKey(
            savedStateHandle.get<String>(KEY_CHART_TYPE)
        ) ?: ChartType.DEFAULT
    }

    private suspend fun restoreChartTypeFromPreferences(): ChartType? {
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId
            ?: return null
        val config = cardsConfigurationRepository.getConfiguration(siteId)
        return ChartType.fromStorageKey(config.selectedChartType)
    }

    private fun saveChartType(chartType: ChartType) {
        savedStateHandle[KEY_CHART_TYPE] = chartType.storageKey

        val siteId = selectedSiteRepository.getSelectedSite()?.siteId
            ?: return
        viewModelScope.launch {
            val config =
                cardsConfigurationRepository.getConfiguration(siteId)
            cardsConfigurationRepository.saveConfiguration(
                siteId,
                config.copy(
                    selectedChartType = chartType.storageKey
                )
            )
        }
    }

    fun onChartTypeChanged(chartType: ChartType) {
        currentChartType = chartType
        saveChartType(chartType)
        _uiState.update { current ->
            if (current is ViewsStatsCardUiState.Content && current.chart is ChartUiState.Loaded) {
                current.copy(chart = current.chart.copy(chartType = chartType))
            } else {
                current
            }
        }
    }

    @Suppress("ReturnCount")
    fun onBarTapped(index: Int) {
        val content = _uiState.value as? ViewsStatsCardUiState.Content ?: return
        if (content.isLoadingNewPeriod) return
        val chart = content.chart as? ChartUiState.Loaded ?: return
        val dataPoint = chart.chartData.currentPeriod.getOrNull(index) ?: return
        val rawPeriod = dataPoint.rawPeriod
        val newPeriod = drillDownPeriod(rawPeriod) ?: return
        _uiState.value = content.copy(isLoadingNewPeriod = true)
        onPeriodChanged(newPeriod)
        loadingPeriod = newPeriod
        loadData()
    }

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    private fun drillDownPeriod(rawPeriod: String): StatsPeriod? {
        val period = currentPeriod
        val isMonthlyGranularity = period is StatsPeriod.Last6Months ||
            period is StatsPeriod.Last12Months ||
            (period is StatsPeriod.Custom &&
                isCustomPeriodMonthly(period))

        // Hourly data — no smaller period available
        if (rawPeriod.matches(HOURLY_FORMAT_REGEX)) return null

        return try {
            when {
                isMonthlyGranularity -> drillDownMonth(rawPeriod)
                rawPeriod.matches(DAILY_FORMAT_REGEX) -> {
                    val date = LocalDate.parse(
                        rawPeriod,
                        DateTimeFormatter.ISO_LOCAL_DATE
                    )
                    StatsPeriod.Custom(date, date)
                }
                else -> null
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Failed to parse period for drill-down: $rawPeriod",
                e
            )
            null
        }
    }

    private fun drillDownMonth(rawPeriod: String): StatsPeriod {
        val firstDay = if (rawPeriod.matches(MONTHLY_FORMAT_REGEX)) {
            val parts = rawPeriod.split("-")
            LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
        } else {
            LocalDate.parse(
                rawPeriod.take(DATE_PREFIX_LENGTH),
                DateTimeFormatter.ISO_LOCAL_DATE
            ).withDayOfMonth(1)
        }
        val lastDay = firstDay.withDayOfMonth(
            firstDay.lengthOfMonth()
        )
        return StatsPeriod.Custom(firstDay, lastDay)
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                loadDataInternal(site)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            _uiState.value = ViewsStatsCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_no_site
                )
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            _uiState.value = ViewsStatsCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_api
                )
            )
            return
        }

        statsRepository.init(accessToken)
        val current = _uiState.value
        // While switching to a new period we keep the previous content on screen (dimmed, with a
        // spinner) instead of resetting to placeholders; otherwise show per-region placeholders.
        if (current !is ViewsStatsCardUiState.Content || !current.isLoadingNewPeriod) {
            _uiState.value = ViewsStatsCardUiState.Content(
                chart = ChartUiState.Loading,
                bottomStats = BottomStatsUiState.Loading
            )
        }

        viewModelScope.launch {
            loadDataInternal(site)
        }
    }

    /**
     * Loads the chart and the bottom-stats row concurrently and independently. Each updates only its
     * own region of the [ViewsStatsCardUiState.Content] state as it completes, so the chart can
     * appear before (or without) the bottom row and vice versa.
     */
    private suspend fun loadDataInternal(site: SiteModel) {
        val targetPeriod = currentPeriod
        try {
            val loaded = if (fillsBottomFromChart(targetPeriod)) {
                // Every non-hourly period uses a daily/monthly/yearly chart whose response already
                // carries all five bottom-row metrics per bucket, so the bottom row is filled from that
                // same fetch. This makes the card issue 2 network calls instead of 4.
                loadChart(site, fillBottomFromChart = true)
            } else {
                // Single-day periods fetch the bottom row from a dedicated day-level call (run in
                // parallel with the chart), the same way the web app does it: their chart is hourly and
                // an hourly response only populates `views`, so it can't fill the row.
                coroutineScope {
                    val chart = async { loadChart(site, fillBottomFromChart = false) }
                    val bottom = async { loadBottomStats(site) }
                    val chartLoaded = chart.await()
                    val bottomLoaded = bottom.await()
                    chartLoaded && bottomLoaded
                }
            }
            // Treat the period as loaded only when everything shown succeeded; otherwise a transient
            // failure would leave loadedPeriod set and make loadDataIfNeeded short-circuit forever.
            if (loaded) {
                loadedPeriod = targetPeriod
            }
        } finally {
            loadingPeriod = null
            clearLoadingNewPeriod()
        }
    }

    /**
     * Clears the card-level [ViewsStatsCardUiState.Content.isLoadingNewPeriod] dim flag once both the
     * chart and the (independently, and often slower) bottom-row calls have completed. Clearing it here
     * rather than when the chart alone finishes prevents the previous period's bottom-row totals from
     * being shown un-dimmed as if they belonged to the new period.
     */
    private fun clearLoadingNewPeriod() {
        _uiState.update { current ->
            if (current is ViewsStatsCardUiState.Content && current.isLoadingNewPeriod) {
                current.copy(isLoadingNewPeriod = false)
            } else {
                current
            }
        }
    }

    /**
     * Loads the chart for [currentPeriod]. When [fillBottomFromChart] is true the bottom row is filled
     * from the same response (its per-bucket data already carries all five metrics); when false the
     * bottom row is left untouched here because a dedicated call populates it (Today and Custom).
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadChart(site: SiteModel, fillBottomFromChart: Boolean): Boolean {
        var success = false
        val chartState = try {
            when (val result = statsRepository.fetchStatsForPeriod(site.siteId, currentPeriod)) {
                is PeriodStatsResult.Success -> {
                    success = true
                    if (fillBottomFromChart) {
                        updateBottom(BottomStatsUiState.Loaded(result.toBottomStatItems()))
                    }
                    buildChartLoaded(result)
                }
                is PeriodStatsResult.Error -> {
                    if (fillBottomFromChart) updateBottom(BottomStatsUiState.Hidden)
                    ChartUiState.Error
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Error loading views chart", e)
            if (fillBottomFromChart) updateBottom(BottomStatsUiState.Hidden)
            ChartUiState.Error
        }
        updateChart(chartState)
        return success
    }

    /**
     * Whether the bottom row can be filled from the chart's own response, which is true whenever the
     * chart isn't hourly: a non-hourly `stats/visits` response carries all five bottom-row metrics per
     * bucket, and the chart requests the same unit, quantity and windows the row needs.
     *
     * Only the single-day periods (Today, and a Custom range whose start and end are the same day)
     * render an hourly chart, and an hourly response populates `views` alone — so those fetch the row
     * from a dedicated day-level call instead.
     */
    private fun fillsBottomFromChart(period: StatsPeriod): Boolean = when (period) {
        is StatsPeriod.Last7Days,
        is StatsPeriod.Last30Days,
        is StatsPeriod.Last6Months,
        is StatsPeriod.Last12Months -> true
        // A multi-day Custom chart shares the bottom row's unit and quantity (both derive from the
        // repository's span-based rule, year-coarsening included), so its response already holds the
        // row's totals — including visitor uniques de-duplicated at the same granularity.
        is StatsPeriod.Custom -> period.startDate != period.endDate
        is StatsPeriod.Today -> false
    }

    /**
     * Maps the chart's period aggregates (which already carry all five metrics for the fixed periods)
     * into the bottom-row stat items, so no dedicated bottom-stats call is needed.
     */
    private fun PeriodStatsResult.Success.toBottomStatItems(): List<StatItem> =
        buildStatItems(currentAggregates.toBottomAggregates(), previousAggregates.toBottomAggregates())

    private fun PeriodAggregates.toBottomAggregates() = BottomStatsAggregates(
        views = views,
        visitors = visitors,
        likes = likes,
        comments = comments,
        posts = posts
    )

    /** Fetches the bottom row from a dedicated call. Used for Today and Custom (see [fillsBottomFromChart]). */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadBottomStats(site: SiteModel): Boolean {
        var success = false
        val bottomState = try {
            when (val result = statsRepository.fetchBottomStats(site.siteId, currentPeriod)) {
                is BottomStatsResult.Success -> {
                    success = true
                    BottomStatsUiState.Loaded(buildStatItems(result.current, result.previous))
                }
                is BottomStatsResult.Error -> BottomStatsUiState.Hidden
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Error loading bottom stats", e)
            BottomStatsUiState.Hidden
        }
        updateBottom(bottomState)
        return success
    }

    /**
     * Applies a chart-region update, preserving the current bottom-row state. The card-level
     * [ViewsStatsCardUiState.Content.isLoadingNewPeriod] dim flag is intentionally left untouched here;
     * it is cleared by [clearLoadingNewPeriod] only once both regions have finished loading.
     */
    private fun updateChart(chart: ChartUiState) {
        _uiState.update { current ->
            when (current) {
                is ViewsStatsCardUiState.Content -> current.copy(chart = chart)
                else -> ViewsStatsCardUiState.Content(chart = chart, bottomStats = BottomStatsUiState.Loading)
            }
        }
    }

    /** Applies a bottom-row update, preserving the current chart state. */
    private fun updateBottom(bottom: BottomStatsUiState) {
        _uiState.update { current ->
            when (current) {
                is ViewsStatsCardUiState.Content -> current.copy(bottomStats = bottom)
                else -> ViewsStatsCardUiState.Content(chart = ChartUiState.Loading, bottomStats = bottom)
            }
        }
    }

    private fun buildChartLoaded(result: PeriodStatsResult.Success): ChartUiState.Loaded {
        val currentStats = result.currentAggregates
        val previousStats = result.previousAggregates
        val currentDataPoints = result.currentPeriodData
            .map {
                ChartDataPoint(
                    formatDataPointLabel(it.period, result.unit),
                    it.views,
                    it.period
                )
            }
        val previousDataPoints = result.previousPeriodData
            .map {
                ChartDataPoint(
                    formatDataPointLabel(it.period, result.unit),
                    it.views,
                    it.period
                )
            }

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

        return ChartUiState.Loaded(
            currentPeriodViews = currentStats.views,
            previousPeriodViews = previousStats.views,
            viewsDifference = currentStats.views - previousStats.views,
            viewsPercentageChange = calculatePercentageChange(currentStats.views, previousStats.views),
            currentPeriodDateRange = formatDateRangeForPeriod(
                currentStats.startDate,
                currentStats.endDate,
                currentPeriod,
                result.unit
            ),
            previousPeriodDateRange = formatDateRangeForPeriod(
                previousStats.startDate,
                previousStats.endDate,
                currentPeriod,
                result.unit
            ),
            chartData = ViewsStatsChartData(currentPeriod = currentDataPoints, previousPeriod = previousDataPoints),
            periodAverage = average,
            chartType = currentChartType
        )
    }

    private fun buildStatItems(
        currentPeriod: BottomStatsAggregates,
        previousPeriod: BottomStatsAggregates
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

    /**
     * Formats one chart bucket's axis label. The output granularity comes from [unit] — the
     * granularity the buckets were actually requested at — not from the [period] string, which is a
     * full ISO date for DAY, MONTH and YEAR buckets alike and so cannot be told apart on its own.
     * The string's shape still decides how to *parse* it, since the API returns "yyyy-MM" for some
     * month buckets and "yyyy-MM-dd" for others.
     */
    private fun formatDataPointLabel(period: String, unit: StatsUnit): String = when {
        period.matches(HOURLY_FORMAT_REGEX) -> formatHourlyLabel(period)
        else -> parsePeriodDate(period)
            ?.format(DateTimeFormatter.ofPattern(labelPatternFor(unit), Locale.getDefault()))
            ?: period
    }

    private fun labelPatternFor(unit: StatsUnit): String = when (unit) {
        StatsUnit.YEAR -> "yyyy"
        StatsUnit.MONTH -> "MMM"
        else -> "MMM d"
    }

    /** Parses a bucket's period string, tolerating both the "yyyy-MM" and "yyyy-MM-dd" shapes. */
    private fun parsePeriodDate(period: String): LocalDate? = when {
        period.matches(DAILY_FORMAT_REGEX) -> LocalDate.parse(period, DateTimeFormatter.ISO_LOCAL_DATE)
        period.matches(MONTHLY_FORMAT_REGEX) -> {
            val parts = period.split("-")
            LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
        }
        else -> null
    }

    private fun formatHourlyLabel(period: String): String {
        val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        return LocalDateTime.parse(period, inputFormat).format(outputFormat)
    }

    private fun isCustomPeriodMonthly(custom: StatsPeriod.Custom): Boolean {
        val daysBetween = ChronoUnit.DAYS.between(custom.startDate, custom.endDate) + 1
        return daysBetween > DAYS_THRESHOLD_FOR_MONTHLY_DISPLAY
    }

    /**
     * Formats the legend's date range. A YEAR-unit window is decided by [unit] rather than by
     * [period], because a Custom range long enough to be charted in years would otherwise fall into
     * the month branch and render a span like "Jan - Jun" for 2024→2026.
     */
    private fun formatDateRangeForPeriod(
        startDate: String,
        endDate: String,
        period: StatsPeriod,
        unit: StatsUnit
    ): String {
        if (unit == StatsUnit.YEAR) return formatYearRange(startDate, endDate)
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

    private fun formatSingleDayRange(date: String): String {
        val parsedDate = parsePeriodDate(date) ?: return date
        return parsedDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }

    @Suppress("ReturnCount")
    private fun formatYearRange(startDate: String, endDate: String): String {
        val start = parsePeriodDate(startDate) ?: return "$startDate - $endDate"
        val end = parsePeriodDate(endDate) ?: return "$startDate - $endDate"

        return if (start.year == end.year) "${start.year}" else "${start.year} - ${end.year}"
    }

    @Suppress("ReturnCount")
    private fun formatMonthRange(startDate: String, endDate: String): String {
        val start = parsePeriodDate(startDate) ?: return "$startDate - $endDate"
        val end = parsePeriodDate(endDate) ?: return "$startDate - $endDate"
        val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

        return if (start.month == end.month && start.year == end.year) {
            start.format(monthFormat)
        } else {
            "${start.format(monthFormat)} - ${end.format(monthFormat)}"
        }
    }

    @Suppress("ReturnCount")
    private fun formatDayRange(startDate: String, endDate: String): String {
        val start = parsePeriodDate(startDate) ?: return "$startDate - $endDate"
        val end = parsePeriodDate(endDate) ?: return "$startDate - $endDate"
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

    companion object {
        /**
         * Keys used to restore the selected period. They double as Intent extras: Hilt seeds the
         * [SavedStateHandle] from the launching Intent, so an entry point can open New Stats on a
         * specific period without persisting it as the user's preference.
         */
        const val KEY_PERIOD_TYPE = "period_type"
        const val KEY_CUSTOM_START_DATE = "custom_start_date"
        const val KEY_CUSTOM_END_DATE = "custom_end_date"
    }
}
