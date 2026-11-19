package org.wordpress.android.ui.newstats.viewsstats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.ChartInterval
import org.wordpress.android.ui.newstats.StatsCardsConfiguration
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.repository.BottomStatsAggregates
import org.wordpress.android.ui.newstats.repository.BottomStatsResult
import org.wordpress.android.ui.newstats.repository.PeriodAggregates
import org.wordpress.android.ui.newstats.repository.PeriodStatsResult
import org.wordpress.android.ui.newstats.repository.StatsCardsConfigurationRepository
import org.wordpress.android.ui.newstats.repository.StatsCacheBucket
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.ViewsDataPoint
import org.wordpress.android.ui.newstats.util.PresetRangeGranularity
import org.wordpress.android.ui.newstats.util.StatsDateSkeleton
import org.wordpress.android.ui.newstats.util.formatCustomDateRange
import org.wordpress.android.ui.newstats.util.formatDayRangeWithYear
import org.wordpress.android.ui.newstats.util.formatMonthRangeWithYear
import org.wordpress.android.ui.newstats.util.formatPresetRange
import org.wordpress.android.ui.newstats.util.formatYearRangeLabel
import org.wordpress.android.ui.newstats.util.statsDateFormatter
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

private const val PERCENTAGE_BASE = 100.0
private const val DAYS_THRESHOLD_FOR_MONTHLY_DISPLAY = 31
private val EMPTY_BOTTOM_AGGREGATES = BottomStatsAggregates(0, 0, 0, 0, 0)
private const val DATE_PREFIX_LENGTH = 10 // "YYYY-MM-DD".length
private const val YEAR_PREFIX_LENGTH = 4 // "YYYY".length

private val HOURLY_FORMAT_REGEX = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")
private val DAILY_FORMAT_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
private val MONTHLY_FORMAT_REGEX = Regex("""\d{4}-\d{2}""")
// A week bucket is the one period string that isn't an ISO date: "2013W07W22" is the week starting
// Monday 22 July 2013. See StatsRepository's WEEK_PERIOD_DATE_PATTERN.
private val WEEKLY_FORMAT_REGEX = Regex("""\d{4}W\d{2}W\d{2}""")
private val WEEKLY_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy'W'MM'W'dd", Locale.ROOT)
private const val DAYS_IN_WEEK = 7L

private const val KEY_CHART_TYPE = "chart_type"
private const val KEY_SELECTED_METRIC = "selected_metric"
private const val KEY_CHART_INTERVAL = "chart_interval"

@HiltViewModel
class ViewsStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider,
    private val savedStateHandle: SavedStateHandle,
    private val cardsConfigurationRepository: StatsCardsConfigurationRepository,
    private val newStatsTracker: NewStatsTracker
) : ViewModel() {
    private val _uiState = MutableStateFlow<ViewsStatsCardUiState>(ViewsStatsCardUiState.Loading)
    val uiState: StateFlow<ViewsStatsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(restorePeriodFromSavedState())
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    // The narrower period of the currently soft-selected bar, or null when no bar is selected (or the
    // selected bar is hourly, which has no representable sub-period). It never drives this card's own
    // chart — the chart stays bound to [selectedPeriod] — only [effectivePeriod].
    private val _selectedBarPeriod = MutableStateFlow<StatsPeriod?>(null)

    /**
     * The period the rest of the screen (all non-Views cards and the top range label) should reflect:
     * the soft-selected bar's period when a drillable bar is selected, otherwise the committed
     * [selectedPeriod]. Kept separate from [selectedPeriod] so a bar tap updates everything *except*
     * the Views chart, which stays frozen with the bar highlighted.
     */
    val effectivePeriod: StateFlow<StatsPeriod> =
        combine(_selectedPeriod, _selectedBarPeriod) { base, bar -> bar ?: base }
            .stateIn(viewModelScope, SharingStarted.Eagerly, _selectedPeriod.value)

    /**
     * The dates [effectivePeriod] resolves to, shown beneath a preset's name in the Traffic
     * selector so the label names the window the cards were loaded for. A preset's name on its own
     * ("Last 7 days") never changes, which is what made a still-selected period look like stats
     * that had stopped updating when a user refreshed them (CMM-2540).
     *
     * Null for a Custom range — including a soft-selected bar, which is one — whose label is
     * already made of those dates; repeating them would only double the line.
     *
     * Recomputed on every [_uiState] emission as well as on a period change, and the window is read
     * off the clock each time. The [_uiState] leg is what keeps the dates right after a same-period
     * pull-to-refresh, which moves neither [_selectedPeriod] nor [effectivePeriod] and so would
     * otherwise leave the label untouched.
     *
     * The cost of reading the clock there is that emissions which do *no* fetch — [onMetricSelected],
     * [onChartTypeChanged], [onBarTapped] — also relabel. A session left open across local midnight
     * therefore shows today's window above data loaded yesterday until the next load. Accepted
     * rather than fixed: it takes a session spanning midnight plus a tap on one of those controls,
     * and dates a day ahead of the chart read as a prompt to refresh rather than as stalled stats.
     * Deriving the label from the loaded result instead would remove the gap entirely.
     */
    val effectivePeriodDates: StateFlow<String?> =
        combine(effectivePeriod, _uiState) { period, _ -> formatPeriodDates(period) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, formatPeriodDates(_selectedPeriod.value))

    private fun formatPeriodDates(period: StatsPeriod): String? {
        if (period is StatsPeriod.Custom) return null
        val (start, end) = statsRepository.periodWindow(period)
        return formatPresetRange(start, end, granularity = period.bucketGranularity())
    }

    private var currentChartType: ChartType = restoreChartTypeFromSavedState()

    // The user's persisted metric preference. It isn't necessarily what the chart plots: on single-day
    // periods only views has an hourly series, so the chart falls back to it (see [chartedMetric]) while
    // this preference is kept untouched, so the chart returns to it on the next multi-day period.
    private var currentSelectedMetric: StatsMetric = restoreMetricFromSavedState()

    // Whether [currentSelectedMetric] was picked while on [currentPeriod], as opposed to carried in from
    // an earlier one. Only a carried-in preference is overridden by the views fallback, so a deliberate
    // tap on a metric this period can't chart still gets that metric's own empty state. Reset on every
    // period change: the fallback has to be re-evaluated for the period the user just moved to.
    private var metricPickedOnCurrentPeriod = false

    // The last successful chart result for the current period, cached so a metric switch can re-plot
    // the chart from data already in memory, without a new network call.
    private var lastChartResult: PeriodStatsResult.Success? = null

    // The committed period's bottom-row state, tracked so a soft bar selection can overlay the bar's
    // own totals and then restore the whole-period row when the selection is cleared.
    private var wholePeriodBottom: BottomStatsUiState = BottomStatsUiState.Loading

    // The request [wholePeriodBottom] holds the row for. It outlives a period change, and a single-day
    // period loads its row from a separate call that can land well after the chart, so without this
    // an hourly selection made in between would borrow the *previous* period's totals for the metrics
    // an hourly response carries no series for — see [wholePeriodRowForCurrentPeriod].
    private var wholePeriodBottomRequest: ChartRequest? = null

    private var currentPeriod: StatsPeriod = _selectedPeriod.value

    // The bucket width the chart is drawn at. [ChartInterval.AUTOMATIC] leaves it to the window, which
    // is what the chart has always done; anything else is the user overriding that derivation.
    private var currentInterval: ChartInterval = restoreIntervalFromSavedState()

    // The period the user last picked themselves, kept while they page back and forth. Paging turns a
    // preset into a concrete range, so by the time forward navigation lands back on the present edge
    // the picked label is gone — several presets can describe that same window and the wrong one would
    // win. Only an explicit pick (selector, custom range, bar drill-down) redefines it. Persisted with
    // the period so paging that spans a restart still resolves against the pick.
    private var periodOrigin: StatsPeriod = restoreOriginFromSavedState() ?: _selectedPeriod.value

    private var loadingRequest: ChartRequest? = null
    private var loadedRequest: ChartRequest? = null
    private var loadJob: Job? = null

    private val _isPeriodInitialized = MutableStateFlow(false)
    val isPeriodInitialized: StateFlow<Boolean> = _isPeriodInitialized.asStateFlow()

    private val _canNavigateBackward = MutableStateFlow(statsRepository.canNavigateBackward(currentPeriod))
    val canNavigateBackward: StateFlow<Boolean> = _canNavigateBackward.asStateFlow()

    private val _canNavigateForward = MutableStateFlow(statsRepository.canNavigateForward(currentPeriod))
    val canNavigateForward: StateFlow<Boolean> = _canNavigateForward.asStateFlow()

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
                val restored = restorePeriodFromPreferences()
                if (restored != null) {
                    currentPeriod = restored.period
                    periodOrigin = restored.origin
                    _selectedPeriod.value = restored.period
                    updateNavigationState()
                }
            }
            val savedChartType = savedStateHandle.get<String>(KEY_CHART_TYPE)
            if (savedChartType == null) {
                val restoredChartType = restoreChartTypeFromPreferences()
                if (restoredChartType != null) {
                    currentChartType = restoredChartType
                }
            }
            val savedMetric = savedStateHandle.get<String>(KEY_SELECTED_METRIC)
            if (savedMetric == null) {
                val restoredMetric = restoreMetricFromPreferences()
                if (restoredMetric != null) {
                    currentSelectedMetric = restoredMetric
                }
            }
            val savedInterval = savedStateHandle.get<String>(KEY_CHART_INTERVAL)
            if (savedInterval == null) {
                val restoredInterval = restoreIntervalFromPreferences()
                if (restoredInterval != null) {
                    currentInterval = restoredInterval
                }
            }
            // The period and the interval are stored separately, so a restored pairing can be one the
            // user never saw. Dropped in memory only: a launch is not a pick, and leaving what's
            // stored alone means the interval is still there if they go back to a period that fits it.
            if (!intervalFitsPeriod(currentInterval, currentPeriod)) {
                currentInterval = ChartInterval.DEFAULT
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
        val target = currentRequest()
        if (loadedRequest == target || loadingRequest == target) return
        loadingRequest = target
        loadData()
    }

    /**
     * A preset picked from the period selector. Separate from [onPeriodChanged] so only a pick made
     * in the menu is reported - paging and bar drill-downs reach the same range through their own
     * entry points, and each has its own event.
     *
     * Reports the tap even when [period] is already the committed range: that tap is how the user
     * returns the screen to the whole period after soft-selecting a bar, so it is a real choice
     * rather than a no-op.
     */
    fun onPresetSelected(period: StatsPeriod) {
        newStatsTracker.trackDateRangePresetSelected(period)
        onPeriodChanged(period)
    }

    /** A range picked in the date-range picker dialog. */
    fun onCustomRangeSelected(startDate: LocalDate, endDate: LocalDate) {
        newStatsTracker.trackCustomDateRangeSelected(startDate, endDate)
        onPeriodChanged(StatsPeriod.Custom(startDate, endDate))
    }

    /**
     * Commits [period] as the screen's range. An explicit pick — the selector, a custom range, a bar
     * drill-down — also becomes the origin future navigation resolves ties against; paging passes
     * [keepOrigin] so the ranges it produces don't overwrite the preset the user chose.
     */
    fun onPeriodChanged(period: StatsPeriod, keepOrigin: Boolean = false) {
        val hasSoftSelection = _selectedBarPeriod.value != null ||
            (_uiState.value as? ViewsStatsCardUiState.Content)?.selectedBar != null
        if (period == currentPeriod) {
            // Re-selecting the committed range while a bar is soft-selected returns the whole screen
            // to the full period: clearing the selection reverts effectivePeriod (reloading the other
            // cards) and restores this card's whole-period header/bottom. The range label showed the
            // bar's date, so this is the way back. A plain no-op when nothing is selected.
            if (hasSoftSelection) clearSoftSelection()
            return
        }
        // A real period change supersedes any soft bar selection; clear it first (restoring the
        // whole-period header/bottom) so no stale overlay survives into the reload.
        clearSoftSelection()
        currentPeriod = period
        // The chosen interval carries over to the new period when that period can still draw it, and
        // resets when it can't — a Years chart means nothing on a seven-day window. One rule for
        // presets, custom ranges, bar drill-downs and paging alike.
        if (!intervalFitsPeriod(currentInterval, period)) {
            currentInterval = ChartInterval.DEFAULT
            saveInterval(ChartInterval.DEFAULT)
        }
        // The pick belonged to the period being left, so it no longer suppresses the fallback here.
        metricPickedOnCurrentPeriod = false
        if (!keepOrigin) periodOrigin = period
        // Drop the previous period's cached chart result so a metric switch mid-load can't re-plot from
        // stale data or evaluate availability against the wrong period; it is repopulated on next load.
        lastChartResult = null
        _selectedPeriod.value = period
        updateNavigationState()
        savePeriod(period, periodOrigin)
    }

    /**
     * Pages the whole screen to the range immediately before the current one (see
     * [StatsRepository.previousPeriod]). Guarded by the same floor the button's enabled state uses,
     * so a stray tap on a disabled control is a no-op.
     */
    fun onNavigatePrevious() {
        if (!statsRepository.canNavigateBackward(currentPeriod)) return
        // Reported before the move, so the event names the range being left.
        newStatsTracker.trackDateNavigationButtonTapped(
            NewStatsTracker.NavigationDirection.PREVIOUS,
            currentPeriod
        )
        navigateTo(statsRepository.previousPeriod(currentPeriod, origin = periodOrigin))
    }

    /**
     * Pages the whole screen to the range immediately after the current one (see
     * [StatsRepository.nextPeriod]). Guarded so a tap while at the present edge is a no-op.
     */
    fun onNavigateNext() {
        if (!statsRepository.canNavigateForward(currentPeriod)) return
        newStatsTracker.trackDateNavigationButtonTapped(
            NewStatsTracker.NavigationDirection.NEXT,
            currentPeriod
        )
        navigateTo(statsRepository.nextPeriod(currentPeriod, origin = periodOrigin))
    }

    /**
     * Applies a navigated period: dims the current content (if any) while the new range loads, then
     * changes the period — which updates [selectedPeriod] so every other card reloads too — and
     * reloads this card, mirroring [onBarTapped].
     */
    private fun navigateTo(newPeriod: StatsPeriod) {
        if (newPeriod == currentPeriod) return
        (_uiState.value as? ViewsStatsCardUiState.Content)?.let { content ->
            _uiState.value = content.copy(isLoadingNewPeriod = true)
        }
        onPeriodChanged(newPeriod, keepOrigin = true)
        loadingRequest = currentRequest()
        loadData()
    }

    private fun updateNavigationState() {
        _canNavigateBackward.value = statsRepository.canNavigateBackward(currentPeriod)
        _canNavigateForward.value = statsRepository.canNavigateForward(currentPeriod)
    }

    /**
     * Persists [period] together with [origin], the pick it was navigated from. Both are needed: the
     * range alone can't say which preset the user chose, so restoring without the origin would drop
     * paging back onto the tie-break heuristic and could resume on a different window (CMM-2415).
     */
    private fun savePeriod(period: StatsPeriod, origin: StatsPeriod) {
        // Save to SavedStateHandle for immediate restoration
        savedStateHandle[KEY_PERIOD_TYPE] = period.toTypeString()
        if (period is StatsPeriod.Custom) {
            savedStateHandle[KEY_CUSTOM_START_DATE] = period.startDate.toEpochDay()
            savedStateHandle[KEY_CUSTOM_END_DATE] = period.endDate.toEpochDay()
        }
        savedStateHandle[KEY_ORIGIN_PERIOD_TYPE] = origin.toTypeString()
        if (origin is StatsPeriod.Custom) {
            savedStateHandle[KEY_ORIGIN_CUSTOM_START_DATE] = origin.startDate.toEpochDay()
            savedStateHandle[KEY_ORIGIN_CUSTOM_END_DATE] = origin.endDate.toEpochDay()
        }

        // Persist to preferences for cross-session restoration
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId ?: return
        viewModelScope.launch {
            editConfiguration(siteId) { config ->
                config.copy(
                    selectedPeriodType = period.toTypeString(),
                    customPeriodStartDate = (period as? StatsPeriod.Custom)?.startDate?.toEpochDay(),
                    customPeriodEndDate = (period as? StatsPeriod.Custom)?.endDate?.toEpochDay(),
                    originPeriodType = origin.toTypeString(),
                    originCustomStartDate = (origin as? StatsPeriod.Custom)?.startDate?.toEpochDay(),
                    originCustomEndDate = (origin as? StatsPeriod.Custom)?.endDate?.toEpochDay()
                )
            }
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
     * Restores the origin from SavedStateHandle only (fast, no disk I/O), or null when none was saved
     * — an entry point that seeds only the period keys, or a process that died before the first pick.
     * Used for property initialization.
     */
    private fun restoreOriginFromSavedState(): StatsPeriod? {
        val savedOriginType = savedStateHandle.get<String>(KEY_ORIGIN_PERIOD_TYPE) ?: return null
        return StatsPeriod.fromTypeString(
            type = savedOriginType,
            customStartEpochDay = savedStateHandle.get<Long>(KEY_ORIGIN_CUSTOM_START_DATE),
            customEndEpochDay = savedStateHandle.get<Long>(KEY_ORIGIN_CUSTOM_END_DATE)
        )
    }

    /**
     * Restores the period and its origin from persisted preferences asynchronously.
     * Used for app restarts when SavedStateHandle doesn't have the value.
     */
    private suspend fun restorePeriodFromPreferences(): RestoredPeriod? {
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId ?: return null
        val config = cardsConfigurationRepository.getConfiguration(siteId)
        return config.selectedPeriodType?.let { periodType ->
            val period = StatsPeriod.fromTypeString(
                type = periodType,
                customStartEpochDay = config.customPeriodStartDate,
                customEndEpochDay = config.customPeriodEndDate
            )
            // Configurations written before the origin was persisted have none; the stored period is
            // the only stand-in available, and using it reproduces the previous restore behaviour.
            val origin = config.originPeriodType?.let { originType ->
                StatsPeriod.fromTypeString(
                    type = originType,
                    customStartEpochDay = config.originCustomStartDate,
                    customEndEpochDay = config.originCustomEndDate
                )
            } ?: period
            RestoredPeriod(period, origin)
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
            editConfiguration(siteId) { config -> config.copy(selectedChartType = chartType.storageKey) }
        }
    }

    fun onChartTypeChanged(chartType: ChartType) {
        // Re-picking the type already on screen changes nothing visible, so it isn't reported.
        if (chartType != currentChartType) {
            newStatsTracker.trackChartTypeChanged(from = currentChartType, to = chartType)
        }
        currentChartType = chartType
        saveChartType(chartType)
        // The bar highlight is BAR-only and the header overlay is tied to a selection, so a chart-type
        // switch clears the soft selection and restores the full-period header before re-typing it.
        clearSoftSelection()
        _uiState.update { current ->
            if (current is ViewsStatsCardUiState.Content && current.chart is ChartUiState.Loaded) {
                current.copy(chart = current.chart.copy(chartType = chartType))
            } else {
                current
            }
        }
    }

    private fun restoreMetricFromSavedState(): StatsMetric {
        return StatsMetric.fromStorageKey(
            savedStateHandle.get<String>(KEY_SELECTED_METRIC)
        ) ?: StatsMetric.DEFAULT
    }

    private suspend fun restoreMetricFromPreferences(): StatsMetric? {
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId
            ?: return null
        val config = cardsConfigurationRepository.getConfiguration(siteId)
        return StatsMetric.fromStorageKey(config.selectedMetric)
    }

    private fun saveMetric(metric: StatsMetric) {
        savedStateHandle[KEY_SELECTED_METRIC] = metric.storageKey

        val siteId = selectedSiteRepository.getSelectedSite()?.siteId
            ?: return
        viewModelScope.launch {
            editConfiguration(siteId) { config -> config.copy(selectedMetric = metric.storageKey) }
        }
    }

    /**
     * What the interval menu shows: the units this period can draw, and the one the bars are
     * actually at — which under [ChartInterval.AUTOMATIC] is whatever the window resolved to, and
     * is what the menu ticks either way.
     */
    private fun intervalUiState() = ChartIntervalUiState(
        offered = statsRepository.offeredUnits(currentPeriod),
        effectiveUnit = statsRepository.chartUnitFor(currentPeriod, currentInterval)
    )

    /**
     * Whether [interval] still means something on [period]. [ChartInterval.AUTOMATIC] always does:
     * it is the absence of a choice, and the window derives a unit for every period.
     */
    private fun intervalFitsPeriod(interval: ChartInterval, period: StatsPeriod): Boolean =
        interval == ChartInterval.AUTOMATIC || interval.unit in statsRepository.offeredUnits(period)

    private fun restoreIntervalFromSavedState(): ChartInterval =
        ChartInterval.fromStorageKey(savedStateHandle.get<String>(KEY_CHART_INTERVAL)) ?: ChartInterval.DEFAULT

    private suspend fun restoreIntervalFromPreferences(): ChartInterval? {
        val siteId = selectedSiteRepository.getSelectedSite()?.siteId
            ?: return null
        val config = cardsConfigurationRepository.getConfiguration(siteId)
        return ChartInterval.fromStorageKey(config.selectedChartInterval)
    }

    private fun saveInterval(interval: ChartInterval) {
        savedStateHandle[KEY_CHART_INTERVAL] = interval.storageKey

        val siteId = selectedSiteRepository.getSelectedSite()?.siteId
            ?: return
        viewModelScope.launch {
            editConfiguration(siteId) { config -> config.copy(selectedChartInterval = interval.storageKey) }
        }
    }

    /**
     * Reads the stored configuration for [siteId], applies [edit] to it and writes it back, with no
     * other edit of the blob interleaving.
     *
     * Every setting here lives in one blob, so persisting any of them is a read-modify-write of the
     * whole thing: two running at once and the one that writes first is silently reverted. A single
     * period change fires two — the period, plus the interval when the new period can't draw it —
     * which is enough to lose the period the user just picked. The serialising lives in the
     * repository, which is the singleton that owns the blob, so a card reorder issued from another
     * view model is ordered against these writes too.
     */
    private suspend fun editConfiguration(
        siteId: Long,
        edit: (StatsCardsConfiguration) -> StatsCardsConfiguration
    ) = cardsConfigurationRepository.update(siteId, edit)

    /**
     * Pins the width of one chart bar to [unit].
     *
     * Picking the width the chart is already at changes nothing — including under
     * [ChartInterval.AUTOMATIC], where the pick is dropped rather than frozen into an override, so
     * the chart goes on deriving its width as the user moves between periods. That is why the test
     * is on the resolved unit and not on the stored interval.
     *
     * Unlike a metric change, a real switch can't be answered from what's already in memory: the
     * buckets themselves change, so the chart is refetched — dimmed, the way paging to another
     * period is, rather than flashing placeholders over data that is about to be replaced.
     */
    fun onChartIntervalSelected(unit: StatsUnit) {
        val interval = ChartInterval.of(unit)
        // Reported on every pick, a re-pick included, which is what iOS reports for this event. The
        // neighbouring chart-type event deliberately does the opposite; don't align them.
        newStatsTracker.trackChartIntervalChanged(from = currentInterval, to = interval)
        if (statsRepository.chartUnitFor(currentPeriod, currentInterval) == unit) return
        currentInterval = interval
        saveInterval(interval)
        // Bar index N addresses a different bucket once the chart is re-bucketed, so no selection can
        // survive the switch.
        clearSoftSelection()
        // The cached result is plotted at the old bucket width, so it can't stand in for the new one —
        // not even for the metric switch that would otherwise re-plot from it.
        lastChartResult = null
        (_uiState.value as? ViewsStatsCardUiState.Content)?.let { content ->
            // The menu's checkmark and the unit it names have to move with the tap, not when the
            // refetch lands, or the card looks inert for the length of a network call.
            _uiState.value = content.copy(isLoadingNewPeriod = true, interval = intervalUiState())
        }
        loadingRequest = currentRequest()
        loadData()
    }

    /**
     * Selects a metric to chart and list. Re-plots the chart from the cached period result (no network
     * call) and persists the choice. An explicit pick is always honoured, even on a single-day (hourly)
     * period where only [StatsMetric.VIEWS] has a series: there the chart region shows the "no hourly
     * data" state so the tap has a visible answer instead of appearing inert. That differs from merely
     * *arriving* on such a period, which falls back to views — see [chartedMetric].
     *
     * Tapping the metric already on screen does nothing and persists nothing, so the stored preference
     * only ever changes through a tap that visibly changes the card.
     */
    fun onMetricSelected(metric: StatsMetric) {
        // Tapping the metric the card already shows is a no-op, whether it is showing because the user
        // chose it or because the views fallback stands in for something else. Comparing against the
        // *stored preference* instead would let a tap on the highlighted Views tab through while the
        // fallback is up: the rebuilt state is identical, so nothing would change on screen, but the
        // user's real preference would be silently overwritten with views.
        if (metric == chartedMetric()) return
        newStatsTracker.trackChartMetricSelected(metric)
        currentSelectedMetric = metric
        metricPickedOnCurrentPeriod = true
        saveMetric(metric)
        val cached = lastChartResult
        _uiState.update { current ->
            if (current !is ViewsStatsCardUiState.Content) return@update current
            // Re-plot from the cached result when the chart is showing plotted content; otherwise keep
            // the current chart region (loading/error) but still reflect the new selection so the title,
            // header dot and icons update — the tap always has a visible effect, matching what was
            // persisted.
            val chart = if (current.chart.isPlotted() && cached != null) {
                buildChartState(cached)
            } else {
                current.chart
            }
            // Keep a soft bar selection across metric switches, re-overlaying the header for the new
            // metric. If the new metric has no chart for this period (Unavailable), there's no bar to
            // highlight, so drop the selection (and its arrow row) instead.
            val selectedBar = current.selectedBar
            if (selectedBar != null && chart is ChartUiState.Loaded) {
                current.copy(
                    chart = buildSelectedBarHeader(chart, selectedBar.index),
                    selectedMetric = metric
                )
            } else {
                if (selectedBar != null) _selectedBarPeriod.value = null
                current.copy(
                    chart = chart,
                    selectedMetric = metric,
                    bottomStats = if (selectedBar != null) restoredWholePeriodBottom() else current.bottomStats,
                    selectedBar = null
                )
            }
        }
    }

    /** Whether the chart region currently shows plotted content (as opposed to loading/error). */
    private fun ChartUiState.isPlotted(): Boolean =
        this is ChartUiState.Loaded || this is ChartUiState.Unavailable

    /**
     * The metric the chart plots and the card labels itself with: the user's [currentSelectedMetric],
     * except when a preference is *carried into* a single-day (hourly) period that has no series for it
     * — there it falls back to [StatsMetric.VIEWS] rather than dropping the user on an empty state they
     * never asked for (CMM-2472). The fallback is transient: the stored preference is untouched, so the
     * chart returns to it as soon as the period spans more than a day.
     *
     * A pick made *while on* the period is not overridden ([metricPickedOnCurrentPeriod]); that tap gets
     * the metric's own [ChartUiState.Unavailable] state, which explains why there is no chart.
     */
    private fun chartedMetric(): StatsMetric =
        if (metricPickedOnCurrentPeriod || chartAvailableFor(currentSelectedMetric)) {
            currentSelectedMetric
        } else {
            StatsMetric.VIEWS
        }

    /**
     * Whether [metric] has a chartable series at the width the chart is currently drawn. An hourly
     * response only carries a views series, so only [StatsMetric.VIEWS] is chartable there; every
     * metric is chartable at any coarser width.
     */
    private fun chartAvailableFor(metric: StatsMetric): Boolean =
        metric == StatsMetric.VIEWS || fillsBottomFromChart(currentRequest())

    private fun PeriodAggregates.valueFor(metric: StatsMetric): Long = when (metric) {
        StatsMetric.VIEWS -> views
        StatsMetric.VISITORS -> visitors
        StatsMetric.LIKES -> likes
        StatsMetric.COMMENTS -> comments
        StatsMetric.POSTS -> posts
    }

    private fun ViewsDataPoint.valueFor(metric: StatsMetric): Long = when (metric) {
        StatsMetric.VIEWS -> views
        StatsMetric.VISITORS -> visitors
        StatsMetric.LIKES -> likes
        StatsMetric.COMMENTS -> comments
        StatsMetric.POSTS -> posts
    }

    /**
     * Soft-selects the tapped bar: the chart stays frozen with the bar highlighted, this card's header
     * reflects only that bar, and — when the bar maps to a narrower period — [effectivePeriod] shifts
     * so the other cards and the top range label follow it. Tapping the already-selected bar clears
     * the selection; tapping another moves it. Committing to a full drill-down is deferred to
     * [onDrillIntoSelectedBar] (the arrow affordance). No network call happens here.
     */
    @Suppress("ReturnCount")
    fun onBarTapped(index: Int) {
        val content = _uiState.value as? ViewsStatsCardUiState.Content ?: return
        if (content.isLoadingNewPeriod) return
        val loaded = content.chart as? ChartUiState.Loaded ?: return
        val dataPoint = loaded.chartData.currentPeriod.getOrNull(index) ?: return
        // A bucket that hasn't happened yet has nothing to show or drill into: it only holds a slot on
        // the axis for the previous period's comparison bar.
        if (dataPoint.isUpcoming) return

        // Tapping the selected bar again reverts to the whole period.
        if (content.selectedBar?.index == index) {
            clearSoftSelection()
            return
        }

        // Only a bar becoming selected is reported; the branch above that clears a selection is the
        // same tap undone, and would otherwise double every bar interaction.
        lastChartResult?.let { result ->
            newStatsTracker.trackChartBarSelected(
                metric = chartedMetric(),
                unit = result.unit,
                value = dataPoint.value
            )
        }

        // drillDownPeriod is null for hourly buckets: no sub-period to broadcast and no arrow, but the
        // bar is still selectable for the header + highlight.
        val barPeriod = drillDownPeriod(dataPoint.rawPeriod)
        _selectedBarPeriod.value = barPeriod
        _uiState.value = content.copy(
            chart = buildSelectedBarHeader(loaded, index),
            bottomStats = buildSelectedBarBottom(index) ?: content.bottomStats,
            selectedBar = SelectedBar(
                index = index,
                label = selectionLabelOf(dataPoint),
                canDrillDown = barPeriod != null
            )
        )
    }

    /**
     * Commits the current soft selection to a real drill-down (the arrow affordance): pages the whole
     * screen into the selected bar's period, the same path [onNavigatePrevious]/[onNavigateNext] use.
     * No-op when nothing is selected or the selection is an hourly bar (no narrower period).
     */
    @Suppress("ReturnCount")
    fun onDrillIntoSelectedBar() {
        val content = _uiState.value as? ViewsStatsCardUiState.Content ?: return
        if (content.isLoadingNewPeriod) return
        if (content.selectedBar == null) return
        val newPeriod = _selectedBarPeriod.value ?: return
        _uiState.value = content.copy(isLoadingNewPeriod = true, selectedBar = null)
        // onPeriodChanged also resets _selectedBarPeriod, collapsing effectivePeriod back onto the new
        // committed period so cards already showing that period simply keep it.
        onPeriodChanged(newPeriod)
        loadingRequest = currentRequest()
        loadData()
    }

    /** Clears the soft bar selection and restores the full-period header from the cached result. */
    private fun clearSoftSelection() {
        val content = _uiState.value as? ViewsStatsCardUiState.Content
        if (_selectedBarPeriod.value == null && content?.selectedBar == null) return
        _selectedBarPeriod.value = null
        val cached = lastChartResult
        _uiState.update { current ->
            if (current !is ViewsStatsCardUiState.Content) return@update current
            val restoredChart = if (current.chart is ChartUiState.Loaded && cached != null) {
                buildChartState(cached)
            } else {
                current.chart
            }
            current.copy(chart = restoredChart, bottomStats = restoredWholePeriodBottom(), selectedBar = null)
        }
    }

    /**
     * Overlays the header totals of [loaded] with the values of the bar at [index], leaving the chart
     * series, average line and type untouched so the bars don't move. The current bucket's value and
     * the index-aligned previous bucket's value already carry the currently selected metric, so the
     * overlay is metric-correct with no recomputation.
     */
    private fun buildSelectedBarHeader(loaded: ChartUiState.Loaded, index: Int): ChartUiState.Loaded {
        val current = loaded.chartData.currentPeriod.getOrNull(index) ?: return loaded
        // An empty raw period is the slot a bucket with no counterpart is paired against (see
        // StatsRepository's alignedPreviousBuckets), not a bucket with a name this can read.
        val previous = loaded.chartData.previousPeriod.getOrNull(index)
            ?.takeIf { it.rawPeriod.isNotEmpty() }
        val currentValue = current.value
        val previousValue = previous?.value ?: 0L
        return loaded.copy(
            currentPeriodTotal = currentValue,
            previousPeriodTotal = previousValue,
            difference = currentValue - previousValue,
            percentageChange = calculatePercentageChange(currentValue, previousValue),
            currentPeriodDateRange = selectionLabelOf(current),
            previousPeriodDateRange = previous?.let { selectionLabelOf(it) } ?: loaded.previousPeriodDateRange
        )
    }

    /**
     * [point]'s name as the header and the selected-bar label write it, falling back to the axis
     * label when no chart result is held to read the bucket unit off.
     */
    private fun selectionLabelOf(point: ChartDataPoint): String {
        val unit = lastChartResult?.unit ?: return point.label
        return formatSelectionLabel(point.rawPeriod, unit)
    }

    /**
     * Builds the bottom-row items for the bar at [index] from the cached per-bucket data, so the row
     * reflects the selected bar with no network call. Returns null when there's no cached result or the
     * index is out of range (the caller keeps the existing row).
     *
     * Non-hourly buckets carry all five metrics. An hourly response carries only the views series, so
     * the metrics it has no data for fall back to the whole day's own totals (matching iOS) rather than
     * reading a misleading zero for the selected hour — see [hasPerBucketSeries].
     */
    @Suppress("ReturnCount")
    private fun buildSelectedBarBottom(index: Int): BottomStatsUiState? {
        val result = lastChartResult ?: return null
        val current = result.currentPeriodData.getOrNull(index) ?: return null
        val previous = result.previousPeriodData.getOrNull(index)?.toBottomAggregates()
            ?: EMPTY_BOTTOM_AGGREGATES
        val barItems = buildStatItems(current.toBottomAggregates(), previous)
        if (barItems.all { hasPerBucketSeries(result, it.metric) }) return BottomStatsUiState.Loaded(barItems)
        // What the row borrows is the *window's* total, which only stands in for a bucket while the
        // window is the single day an hourly response was fetched for. A chosen Hours interval can
        // chart a window of several days, where a whole week's visitors sitting under one hour's bar
        // would read as that hour's own count; there, the bucket keeps the zero the API gave it.
        if (!result.coversSingleDay()) return BottomStatsUiState.Loaded(barItems)
        // Something has to be borrowed from the day's own row, so what happens next depends on whether
        // that row has landed for *this* period.
        val wholePeriodItems = when (val dayRow = wholePeriodRowForCurrentPeriod()) {
            is BottomStatsUiState.Loaded -> dayRow.stats
            // It landed and failed: there is nothing to borrow and never will be, so drop the row the
            // same way the unselected card does instead of leaving its placeholder shimmering.
            is BottomStatsUiState.Hidden -> return BottomStatsUiState.Hidden
            // Still in flight, or belonging to the period before this one. Keep whatever the row shows
            // rather than another period's totals or a zero that reads as "none this hour"; the caller
            // reapplies this once the day totals arrive.
            else -> return null
        }
        return BottomStatsUiState.Loaded(
            barItems.map { item ->
                if (hasPerBucketSeries(result, item.metric)) {
                    item
                } else {
                    // The whole item, not just its value: a day-level total must be shown with the
                    // day's own change, never with one computed from the selected bucket.
                    wholePeriodItems.firstOrNull { it.metric == item.metric } ?: item
                }
            }
        )
    }

    /**
     * The committed period's bottom-row state, but only when it belongs to the period now on screen —
     * null while it still belongs to the one before. [wholePeriodBottom] survives a period change, so
     * without the guard a single-day period could dress its hourly selection in the row the period
     * before it left behind.
     */
    private fun wholePeriodRowForCurrentPeriod(): BottomStatsUiState? =
        wholePeriodBottom.takeIf { wholePeriodBottomRequest == currentRequest() }

    /**
     * The row to show when a soft bar selection is dropped: the committed period's own row, or the
     * loading placeholder while [wholePeriodBottom] still belongs to the period before this one. A
     * single-day period fetches its row separately and can be cleared before that call lands, and
     * showing the previous period's totals there is worse than showing the placeholder they replace.
     */
    private fun restoredWholePeriodBottom(): BottomStatsUiState =
        wholePeriodRowForCurrentPeriod() ?: BottomStatsUiState.Loading

    /**
     * Whether [metric] has a per-bucket series of its own in [result], so a selected bucket can show
     * its own value for it.
     *
     * Only the hourly (single-day) response is ever missing one: it populates views alone, leaving the
     * other four metrics at zero in every bucket. Reading that as "no series" rather than hard-coding
     * which metrics are hourly keeps the row correct if the API starts returning more of them — a
     * metric that really is zero all day has an equally zero day total, so the fallback shows the same
     * number either way.
     */
    private fun hasPerBucketSeries(result: PeriodStatsResult.Success, metric: StatsMetric): Boolean =
        result.unit != StatsUnit.HOUR || result.currentPeriodData.any { it.valueFor(metric) > 0L }

    /** Whether this result's window is a single day, so the window's totals are a day's totals. */
    private fun PeriodStatsResult.Success.coversSingleDay(): Boolean =
        currentAggregates.startDate == currentAggregates.endDate

    private fun ViewsDataPoint.toBottomAggregates() = BottomStatsAggregates(
        views = views,
        visitors = visitors,
        likes = likes,
        comments = comments,
        posts = posts
    )

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    private fun drillDownPeriod(rawPeriod: String): StatsPeriod? {
        // The chart's actual bucket unit is the source of truth for granularity: a Custom range is
        // charted in days, months or years depending on its span, and the rawPeriod string alone can't
        // tell a day bucket from a month or a year one -- the API returns a full ISO date for all
        // three. (A week bucket is the exception, being the only self-describing key.) Falling back to
        // day-level drill when no chart is loaded is the safe default.
        val chartUnit = lastChartResult?.unit

        // Hourly data — no smaller period available
        if (rawPeriod.matches(HOURLY_FORMAT_REGEX)) return null

        return try {
            when {
                chartUnit == StatsUnit.YEAR -> drillDownYear(rawPeriod)
                chartUnit == StatsUnit.MONTH -> drillDownMonth(rawPeriod)
                chartUnit == StatsUnit.WEEK -> drillDownWeek(rawPeriod)
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

    /**
     * The whole calendar year a YEAR bucket covers, so tapping the 2024 bar selects 2024 and not its
     * January. The current year is clamped to today by [StatsRepository.calendarYearWindow], the same
     * way the year-to-date presets end on today rather than on 31 December.
     *
     * Only the year is read off [rawPeriod]: a year bucket arrives as a full ISO date ("2024-01-01"),
     * but its month and day carry no meaning, so the first four characters are all that is needed.
     */
    private fun drillDownYear(rawPeriod: String): StatsPeriod {
        val year = rawPeriod.take(YEAR_PREFIX_LENGTH).toInt()
        val (firstDay, lastDay) = statsRepository.calendarYearWindow(year)
        return clampToChartWindow(firstDay, lastDay)
    }

    /** The Monday-to-Sunday week a WEEK bucket covers, clamped to the window on screen. */
    private fun drillDownWeek(rawPeriod: String): StatsPeriod {
        val monday = parseWeekPeriod(rawPeriod)
        return clampToChartWindow(monday, monday.plusDays(DAYS_IN_WEEK - 1))
    }

    /**
     * [start]..[end] narrowed to the window the chart is drawing.
     *
     * A coarse bucket can reach outside that window at either end — the window's first and last
     * buckets are partial whenever it doesn't begin and end on a bucket boundary, which a chosen
     * interval makes the normal case rather than the exception. Handing the screen the whole bucket
     * would put every list card on a range wider than, and partly outside, the one the chart shows,
     * which is how CMM-2489 happened. The trailing clamp also keeps a drill-down off days that
     * haven't happened: the last bar of Last 12 Months used to drill into the whole current month.
     */
    private fun clampToChartWindow(start: LocalDate, end: LocalDate): StatsPeriod {
        val (windowStart, windowEnd) = chartWindow()
        return StatsPeriod.Custom(maxOf(start, windowStart), minOf(end, windowEnd))
    }

    /**
     * The window the chart on screen was loaded for, falling back to the committed period's own
     * window when no chart result is held.
     *
     * Read off the loaded result rather than recomputed from [currentPeriod], because every rolling
     * preset's window is relative to today: re-reading the clock on a tap made after midnight would
     * clamp a drill-down against a window a day away from the one the bars were drawn for, and the
     * last bar — the one most likely to be tapped — would clamp to a day it doesn't cover.
     */
    private fun chartWindow(): Pair<LocalDate, LocalDate> {
        val aggregates = lastChartResult?.currentAggregates
        val start = aggregates?.startDate?.let { parsePeriodDate(it) }
        val end = aggregates?.endDate?.let { parsePeriodDate(it) }
        return if (start != null && end != null) start to end else statsRepository.periodWindow(currentPeriod)
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
        return clampToChartWindow(firstDay, lastDay)
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        // A refresh reloads the committed period, so drop any soft bar selection first: otherwise the
        // rebuilt whole-period header/bottom would sit alongside a still-highlighted bar and an
        // effectivePeriod stuck on the drilled sub-period, drifting the cards out of sync.
        clearSoftSelection()
        // The units a period offers are measured against its window, and every rolling preset's
        // window moves with the clock, so a refresh can be the first load of a period the chosen
        // interval no longer fits — a screen left open overnight on "Today" with Hours is charting
        // two days by the time it is pulled. Dropped in memory only, as at startup: the clock moving
        // is not a pick, so what's stored stays there for the next period that can draw it.
        if (!intervalFitsPeriod(currentInterval, currentPeriod)) {
            currentInterval = ChartInterval.DEFAULT
        }
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
            loadingRequest = null
            _uiState.value = ViewsStatsCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_no_site
                )
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingRequest = null
            _uiState.value = ViewsStatsCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_api
                )
            )
            return
        }

        val targetRequest = currentRequest()
        val current = _uiState.value
        // While switching to a new period we keep the previous content on screen (dimmed, with a
        // spinner) instead of resetting to placeholders; otherwise show per-region placeholders.
        if (current !is ViewsStatsCardUiState.Content || !current.isLoadingNewPeriod) {
            // A fresh (re)load supersedes any soft bar selection (e.g. loadVisibleCards on network
            // regain, or a no-connection Retry). Clear its effective period in lockstep with dropping
            // selectedBar below, so effectivePeriod can't stay stuck on the bar's sub-period.
            _selectedBarPeriod.value = null
            if (isCached(site.siteId, targetRequest)) {
                // A period already in memory repaints in the same frame, so resetting to
                // placeholders would only flash a skeleton over data the card is about to render.
                // The bar selection still has to go: updateChart keeps the overlay while one is set,
                // so leaving it would freeze the card on the previous period's chart.
                _uiState.update { state ->
                    if (state is ViewsStatsCardUiState.Content) state.copy(selectedBar = null) else state
                }
            } else {
                _uiState.value = ViewsStatsCardUiState.Content(
                    chart = ChartUiState.Loading,
                    bottomStats = BottomStatsUiState.Loading,
                    selectedMetric = chartedMetric(),
                    interval = intervalUiState()
                )
            }
        }

        // Rapid paging ("back five days quickly") fires overlapping loads. Cancel the in-flight one so
        // only the latest request survives — otherwise a stale older coroutine can clear the
        // dim/loadingRequest mid-flight (flicker) or write a stale loadedRequest (a redundant refetch).
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadDataInternal(site)
            revalidateIfNeeded(site, targetRequest)
        }
    }

    private fun currentRequest() = ChartRequest(currentPeriod, currentInterval)

    /**
     * Whether everything this card shows for [request] is already in memory: the chart, plus the
     * bottom row for the single-day periods that fetch it separately.
     */
    private fun isCached(siteId: Long, request: ChartRequest): Boolean =
        statsRepository.isChartCached(siteId, request.period, request.interval) &&
            (fillsBottomFromChart(request) ||
                statsRepository.isCached(StatsCacheBucket.BOTTOM_STATS, siteId, request.period))

    /**
     * Refreshes a period served from a previous visit to the stats screen, once. The card already
     * shows those numbers, so this reloads without the placeholders or the dim, and a failure leaves
     * what's on screen alone instead of replacing it with an error.
     */
    private suspend fun revalidateIfNeeded(site: SiteModel, request: ChartRequest) {
        if (isStale(request)) return
        val needsRevalidation =
            statsRepository.chartNeedsRevalidation(site.siteId, request.period, request.interval) ||
                (!fillsBottomFromChart(request) && statsRepository.needsRevalidation(
                    StatsCacheBucket.BOTTOM_STATS, site.siteId, request.period
                ))
        if (!needsRevalidation) return
        loadDataInternal(site, forceRefresh = true, applyErrors = false)
    }

    /**
     * Loads the chart and the bottom-stats row concurrently and independently. Each updates only its
     * own region of the [ViewsStatsCardUiState.Content] state as it completes, so the chart can
     * appear before (or without) the bottom row and vice versa.
     */
    private suspend fun loadDataInternal(
        site: SiteModel,
        forceRefresh: Boolean = false,
        applyErrors: Boolean = true
    ) {
        val targetRequest = currentRequest()
        try {
            val loaded = if (fillsBottomFromChart(targetRequest)) {
                // Every non-hourly period uses a daily/monthly/yearly chart whose response already
                // carries all five bottom-row metrics per bucket, so the bottom row is filled from that
                // same fetch. This makes the card issue 2 network calls instead of 4.
                loadChart(site, targetRequest, true, forceRefresh, applyErrors)
            } else {
                // Single-day periods fetch the bottom row from a dedicated day-level call (run in
                // parallel with the chart), the same way the web app does it: their chart is hourly and
                // an hourly response only populates `views`, so it can't fill the row.
                coroutineScope {
                    val chart = async {
                        loadChart(site, targetRequest, false, forceRefresh, applyErrors)
                    }
                    val bottom = async {
                        loadBottomStats(site, targetRequest, forceRefresh, applyErrors)
                    }
                    val chartLoaded = chart.await()
                    val bottomLoaded = bottom.await()
                    chartLoaded && bottomLoaded
                }
            }
            // Treat the request as loaded only when everything shown succeeded; otherwise a transient
            // failure would leave loadedRequest set and make loadDataIfNeeded short-circuit forever.
            if (loaded) {
                loadedRequest = targetRequest
            }
        } finally {
            // Only clear the flags if this load still owns them. A load superseded by rapid paging is
            // cancelled and its finally runs after the newer load has already set loadingRequest to the
            // newer request, so clearing unconditionally here would wipe the newer load's dim.
            if (loadingRequest == targetRequest) {
                loadingRequest = null
                clearLoadingNewPeriod()
            }
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
     * Loads the chart for [request]. When [fillBottomFromChart] is true the bottom row is filled
     * from the same response (its per-bucket data already carries all five metrics); when false the
     * bottom row is left untouched here because a dedicated call populates it (Today and Custom).
     */
    @Suppress("TooGenericExceptionCaught", "LongParameterList", "ReturnCount")
    private suspend fun loadChart(
        site: SiteModel,
        request: ChartRequest,
        fillBottomFromChart: Boolean,
        forceRefresh: Boolean = false,
        applyErrors: Boolean = true
    ): Boolean {
        // Fetch first and apply afterwards, so the staleness check sits between the two and every write
        // below it is known to belong to the request still on screen.
        val result = try {
            statsRepository.fetchStatsForPeriod(
                siteId = site.siteId,
                period = request.period,
                interval = request.interval,
                forceRefresh = forceRefresh
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Error loading views chart", e)
            null
        }
        if (isStale(request)) return false
        val chartState = when (result) {
            is PeriodStatsResult.Success -> {
                lastChartResult = result
                if (fillBottomFromChart) {
                    updateBottom(BottomStatsUiState.Loaded(result.toBottomStatItems()), request)
                }
                buildChartState(result)
            }
            // An API error or a thrown call: both show the error state and drop the row rather than
            // leaving the previous period's numbers under a new range. A background revalidation
            // instead leaves the cached numbers it was refreshing on screen.
            else -> {
                if (!applyErrors) return false
                if (fillBottomFromChart) updateBottom(BottomStatsUiState.Hidden, request)
                ChartUiState.Error
            }
        }
        updateChart(chartState)
        return result is PeriodStatsResult.Success
    }

    /**
     * Whether a load started for [request] has been overtaken and must not write to the card.
     * [loadData] cancels the load it supersedes, but [refresh] launches outside that job and so
     * survives a period or interval change: without this its response would repaint the chart with
     * the previous range's data, at the previous bucket width, and, worse, stamp that data as the new
     * request's in [updateBottom] — which is exactly the confusion [wholePeriodBottomRequest] exists
     * to prevent.
     */
    private fun isStale(request: ChartRequest): Boolean = request != currentRequest()

    /**
     * Whether the bottom row can be filled from the chart's own response, which is true whenever the
     * chart isn't hourly: a non-hourly `stats/visits` response carries all five bottom-row metrics per
     * bucket, and the chart requests the same unit, quantity and windows the row needs. An hourly
     * response populates `views` alone, so an hourly chart fetches the row from a dedicated day-level
     * call instead.
     *
     * Asks the repository what the chart will actually request rather than pattern-matching the
     * period: once the interval can be chosen, "single day" and "hourly" come apart in both
     * directions. Days on Today is a day-bucketed chart that carries the whole row, and Hours on a
     * fortnight is an hourly one that carries none of it.
     */
    private fun fillsBottomFromChart(request: ChartRequest): Boolean =
        statsRepository.chartUnitFor(request.period, request.interval) != StatsUnit.HOUR

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
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private suspend fun loadBottomStats(
        site: SiteModel,
        request: ChartRequest,
        forceRefresh: Boolean = false,
        applyErrors: Boolean = true
    ): Boolean {
        var success = false
        val bottomState = try {
            when (val result = statsRepository.fetchBottomStats(site.siteId, request.period, forceRefresh)) {
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
        if (isStale(request)) return false
        if (!success && !applyErrors) return false
        updateBottom(bottomState, request)
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
                // Mirror updateBottom: while a bar is soft-selected the visible chart holds the bar's
                // overlaid header. A late reload (a background refresh landing after the user tapped a
                // bar) must not drop it while the highlight/date/arrow/effectivePeriod still point at
                // the bar; lastChartResult already holds the fresh data for the eventual clear.
                is ViewsStatsCardUiState.Content ->
                    current.copy(
                        chart = if (current.selectedBar != null) current.chart else chart,
                        selectedMetric = chartedMetric(),
                        interval = intervalUiState()
                    )
                else -> ViewsStatsCardUiState.Content(
                    chart = chart,
                    bottomStats = BottomStatsUiState.Loading,
                    selectedMetric = chartedMetric(),
                    interval = intervalUiState()
                )
            }
        }
    }

    /**
     * Applies a bottom-row update, preserving the current chart state. [request] is the request the
     * load that produced [bottom] was started for — taken from the load rather than read from
     * [currentRequest] here, so the row can never be stamped with a request it doesn't describe.
     */
    private fun updateBottom(bottom: BottomStatsUiState, request: ChartRequest) {
        // This is always the committed period's row; remember it, and which request it belongs to, so a
        // soft bar selection can restore it after overlaying the selected bar's own totals.
        wholePeriodBottom = bottom
        wholePeriodBottomRequest = request
        _uiState.update { current ->
            when (current) {
                // When a bar is soft-selected the visible row holds that bar's overlaid totals. A
                // late-arriving whole-period bottom load (single-day periods fetch it separately, and it
                // can land after the chart) must not clobber the overlay and desync it from the header;
                // it's kept in wholePeriodBottom above and restored on clear. The overlay is rebuilt
                // rather than kept as-is, because an hourly selection borrows the day's totals for the
                // metrics its own response has no series for — those arrive with this very load.
                is ViewsStatsCardUiState.Content ->
                    current.copy(
                        bottomStats = current.selectedBar
                            ?.let { buildSelectedBarBottom(it.index) ?: current.bottomStats }
                            ?: bottom,
                        selectedMetric = chartedMetric(),
                        interval = intervalUiState()
                    )
                else -> ViewsStatsCardUiState.Content(
                    chart = ChartUiState.Loading,
                    bottomStats = bottom,
                    selectedMetric = chartedMetric(),
                    interval = intervalUiState()
                )
            }
        }
    }

    /**
     * Builds the chart-region state for [result] and the current selection: the plotted chart when the
     * charted metric has a series for this period, or [ChartUiState.Unavailable] when it doesn't (a
     * non-views metric explicitly picked on a single-day/hourly period) — see [chartedMetric].
     */
    private fun buildChartState(result: PeriodStatsResult.Success): ChartUiState =
        if (chartAvailableFor(chartedMetric())) buildChartLoaded(result) else ChartUiState.Unavailable

    private fun buildChartLoaded(result: PeriodStatsResult.Success): ChartUiState.Loaded {
        val metric = chartedMetric()
        val currentStats = result.currentAggregates
        val previousStats = result.previousAggregates
        val currentValue = currentStats.valueFor(metric)
        val previousValue = previousStats.valueFor(metric)
        val currentDataPoints = result.currentPeriodData
            .map {
                ChartDataPoint(
                    formatDataPointLabel(it.period, result.unit),
                    it.valueFor(metric),
                    it.period,
                    it.isUpcoming
                )
            }
        val previousDataPoints = result.previousPeriodData
            .map {
                ChartDataPoint(
                    formatDataPointLabel(it.period, result.unit),
                    it.valueFor(metric),
                    it.period
                )
            }

        // The average is per elapsed bucket: the placeholders an unfinished calendar period is padded
        // with carry no data, so averaging over them would drag the line down as the period fills up.
        val elapsedPointCount = currentDataPoints.count { !it.isUpcoming }
        val average = if (elapsedPointCount > 0) {
            currentValue / elapsedPointCount
        } else {
            if (currentValue > 0) {
                AppLog.w(
                    AppLog.T.STATS,
                    "Data inconsistency: no data points but value=$currentValue"
                )
            }
            0L
        }

        return ChartUiState.Loaded(
            currentPeriodTotal = currentValue,
            previousPeriodTotal = previousValue,
            difference = currentValue - previousValue,
            percentageChange = calculatePercentageChange(currentValue, previousValue),
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
                metric = StatsMetric.VIEWS,
                value = currentPeriod.views,
                change = calculateStatChange(currentPeriod.views, previousPeriod.views)
            ),
            StatItem(
                metric = StatsMetric.VISITORS,
                value = currentPeriod.visitors,
                change = calculateStatChange(currentPeriod.visitors, previousPeriod.visitors)
            ),
            StatItem(
                metric = StatsMetric.LIKES,
                value = currentPeriod.likes,
                change = calculateStatChange(currentPeriod.likes, previousPeriod.likes)
            ),
            StatItem(
                metric = StatsMetric.COMMENTS,
                value = currentPeriod.comments,
                change = calculateStatChange(currentPeriod.comments, previousPeriod.comments)
            ),
            StatItem(
                metric = StatsMetric.POSTS,
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
     * month buckets, "yyyy-MM-dd" for others and a W-separated form for week buckets.
     */
    private fun formatDataPointLabel(period: String, unit: StatsUnit): String = when {
        period.matches(HOURLY_FORMAT_REGEX) -> formatHourlyLabel(period)
        else -> parsePeriodDate(period)?.let { date ->
            when (unit) {
                // A bare four-digit year reads the same everywhere; the localized form ("2026年")
                // would only cost axis width.
                StatsUnit.YEAR -> date.year.toString()
                StatsUnit.MONTH -> date.format(statsDateFormatter(StatsDateSkeleton.MONTH))
                // A week bucket is named by the Monday it starts on, which is what iOS's axis does
                // too. Spelling out the whole range ("Nov 9 - 15") costs more axis width than the
                // labels have, and the selected-bar label above the chart already shows the range.
                else -> date.format(statsDateFormatter(StatsDateSkeleton.DAY_MONTH))
            }
        } ?: period
    }

    /**
     * Parses a bucket's period string into the day its bucket starts on, tolerating every shape the
     * API uses: "yyyy-MM-dd", "yyyy-MM" for some month buckets, and the W-separated form a week
     * bucket arrives in ([WEEKLY_FORMAT_REGEX]).
     */
    private fun parsePeriodDate(period: String): LocalDate? = when {
        period.matches(DAILY_FORMAT_REGEX) -> LocalDate.parse(period, DateTimeFormatter.ISO_LOCAL_DATE)
        period.matches(WEEKLY_FORMAT_REGEX) -> parseWeekPeriod(period)
        period.matches(MONTHLY_FORMAT_REGEX) -> {
            val parts = period.split("-")
            LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
        }
        else -> null
    }

    /**
     * The Monday a week bucket starts on. The API's key already names a day inside the week, but
     * normalising to the Monday keeps the label and the drilled range independent of which day that
     * happens to be.
     */
    private fun parseWeekPeriod(period: String): LocalDate =
        LocalDate.parse(period, WEEKLY_PERIOD_FORMATTER)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun formatHourlyLabel(period: String): String {
        val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        return LocalDateTime.parse(period, inputFormat).format(outputFormat)
    }

    /**
     * How a bucket is named where there is room to spell it out: the selected-bar label above the
     * chart, and the legend line the selection takes over from the window's date range.
     *
     * Two units are abbreviated on the axis because the axis cannot fit them, and both become
     * ambiguous once they stand alone. A week bucket is labelled by the Monday it starts on, which
     * reads as a single day; here it is widened to the Monday-to-Sunday span it covers. An hourly
     * bucket is labelled by its time alone, which was unambiguous while the chart only ever covered
     * one day — a chosen Hours interval can now span several, so the day is named alongside it.
     * Every other unit already names itself, and is left exactly as the axis writes it.
     */
    private fun formatSelectionLabel(period: String, unit: StatsUnit): String = when {
        period.matches(HOURLY_FORMAT_REGEX) -> formatHourlyLabelWithDate(period)
        unit == StatsUnit.WEEK -> parsePeriodDate(period)
            ?.let { formatCustomDateRange(it, it.plusDays(DAYS_IN_WEEK - 1)) }
            ?: formatDataPointLabel(period, unit)
        else -> formatDataPointLabel(period, unit)
    }

    private fun formatHourlyLabelWithDate(period: String): String {
        val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = statsDateFormatter(StatsDateSkeleton.DAY_MONTH_HOUR_MINUTE)
        return LocalDateTime.parse(period, inputFormat).format(outputFormat)
    }

    private fun isCustomPeriodMonthly(custom: StatsPeriod.Custom): Boolean {
        val daysBetween = ChronoUnit.DAYS.between(custom.startDate, custom.endDate) + 1
        return daysBetween > DAYS_THRESHOLD_FOR_MONTHLY_DISPLAY
    }

    /**
     * Formats the legend's date range.
     *
     * This names the *window*, not a bucket, so it follows [period] and not the granularity the
     * chart happens to be drawing. A Custom range is the one case the period can't resolve on its
     * own — its endpoints are arbitrary days whatever its span — so there [unit] stands in for how
     * wide the range is, and a range long enough to be charted in years is named by its years
     * rather than rendering a span like "Jan - Jun" for 2024→2026.
     */
    private fun formatDateRangeForPeriod(
        startDate: String,
        endDate: String,
        period: StatsPeriod,
        unit: StatsUnit
    ): String = when (period) {
        is StatsPeriod.Today -> formatSingleDayRange(endDate)
        is StatsPeriod.Custom -> when {
            unit == StatsUnit.YEAR -> formatYearRange(startDate, endDate)
            isCustomPeriodMonthly(period) -> formatMonthRange(startDate, endDate)
            else -> formatDayRange(startDate, endDate)
        }
        else -> when (period.bucketGranularity()) {
            PresetRangeGranularity.YEAR -> formatYearRange(startDate, endDate)
            PresetRangeGranularity.MONTH -> formatMonthRange(startDate, endDate)
            PresetRangeGranularity.DAY -> formatDayRange(startDate, endDate)
        }
    }

    /**
     * The buckets a preset period is charted — and so named — in. Last 12 Months is twelve month
     * buckets by definition, and This Year takes its granularity from the whole calendar year, so it
     * is twelve of them on 5 January just as in December. Last 3 Years is whole year buckets, one
     * bar per year. Everything else is named by the day.
     */
    private fun StatsPeriod.bucketGranularity(): PresetRangeGranularity = when (this) {
        is StatsPeriod.Last3Years -> PresetRangeGranularity.YEAR
        is StatsPeriod.Last12Months,
        is StatsPeriod.ThisYear -> PresetRangeGranularity.MONTH
        else -> PresetRangeGranularity.DAY
    }

    private fun formatSingleDayRange(date: String): String {
        val parsedDate = parsePeriodDate(date) ?: return date
        return parsedDate.format(statsDateFormatter(StatsDateSkeleton.DAY_MONTH_YEAR))
    }

    @Suppress("ReturnCount")
    private fun formatYearRange(startDate: String, endDate: String): String {
        val start = parsePeriodDate(startDate) ?: return "$startDate - $endDate"
        val end = parsePeriodDate(endDate) ?: return "$startDate - $endDate"

        return formatYearRangeLabel(start, end)
    }

    @Suppress("ReturnCount")
    private fun formatMonthRange(startDate: String, endDate: String): String {
        val start = parsePeriodDate(startDate) ?: return "$startDate - $endDate"
        val end = parsePeriodDate(endDate) ?: return "$startDate - $endDate"
        return formatMonthRangeWithYear(start, end)
    }

    @Suppress("ReturnCount")
    private fun formatDayRange(startDate: String, endDate: String): String {
        val start = parsePeriodDate(startDate) ?: return "$startDate - $endDate"
        val end = parsePeriodDate(endDate) ?: return "$startDate - $endDate"
        return formatDayRangeWithYear(start, end)
    }

    fun onRetry() {
        loadData()
    }

    /**
     * What the card is loading, or has loaded. A period change and an interval change each invalidate
     * everything on screen, so both belong to the identity a response is checked against: the chart
     * is re-bucketed, and so is the bottom row whenever it comes from the chart's own response.
     */
    private data class ChartRequest(val period: StatsPeriod, val interval: ChartInterval)

    /**
     * A restored period paired with the pick it was navigated from, so both survive a restart
     * together. See [periodOrigin].
     */
    private data class RestoredPeriod(val period: StatsPeriod, val origin: StatsPeriod)

    companion object {
        /**
         * Keys used to restore the selected period. They double as Intent extras: Hilt seeds the
         * [SavedStateHandle] from the launching Intent, so an entry point can open New Stats on a
         * specific period without persisting it as the user's preference.
         */
        const val KEY_PERIOD_TYPE = "period_type"
        const val KEY_CUSTOM_START_DATE = "custom_start_date"
        const val KEY_CUSTOM_END_DATE = "custom_end_date"

        /**
         * Keys used to restore [periodOrigin]. Not meant as Intent extras — an entry point that seeds
         * only the period keys gets that period as its own origin, which is what an explicit pick
         * would have done.
         */
        private const val KEY_ORIGIN_PERIOD_TYPE = "origin_period_type"
        private const val KEY_ORIGIN_CUSTOM_START_DATE = "origin_custom_start_date"
        private const val KEY_ORIGIN_CUSTOM_END_DATE = "origin_custom_end_date"
    }
}
