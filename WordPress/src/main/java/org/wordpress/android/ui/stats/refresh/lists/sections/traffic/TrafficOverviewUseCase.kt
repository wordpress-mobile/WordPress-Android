package org.wordpress.android.ui.stats.refresh.lists.sections.traffic

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.traffic.TrafficOverviewUseCase.UiState
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.trackWithGranularity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

const val OVERVIEW_ITEMS_TO_LOAD = 15

@Suppress("LongParameterList")
class TrafficOverviewUseCase(
    private val statsGranularity: StatsGranularity,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val trafficOverviewMapper: TrafficOverviewMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val statsWidgetUpdaters: WidgetUpdater.StatsWidgetUpdaters,
    private val resourceProvider: ResourceProvider,
    private val statsUtils: StatsUtils
) : BaseStatsUseCase<TrafficOverviewUseCase.TrafficOverviewUiModel, UiState>(
    StatsStore.TimeStatsType.OVERVIEW,
    mainDispatcher,
    backgroundDispatcher,
    UiState(),
    fetchParams = listOf(UseCaseParam.SelectedDateParam(statsGranularity))
) {
    // The granularity of the chart is one level lower than the current one
    private val lowerGranularity = when (statsGranularity) {
        StatsGranularity.WEEKS -> StatsGranularity.DAYS
        StatsGranularity.MONTHS -> StatsGranularity.WEEKS
        StatsGranularity.YEARS -> StatsGranularity.MONTHS
        else -> statsGranularity
    }

    private val itemsToLoad = when (lowerGranularity) {
        StatsGranularity.DAYS -> 7
        StatsGranularity.WEEKS -> 6
        StatsGranularity.MONTHS -> 12
        else -> OVERVIEW_ITEMS_TO_LOAD
    }

    override fun buildLoadingItem(): List<BlockListItem> =
        listOf(
            BlockListItem.ValueItem(
                value = "0",
                unit = R.string.stats_views,
                isFirst = true,
                contentDescription = resourceProvider.getString(R.string.stats_loading_card)
            )
        )

    override suspend fun loadCachedData(): TrafficOverviewUiModel? {
        statsWidgetUpdaters.updateViewsWidget(statsSiteProvider.siteModel.siteId)
        statsWidgetUpdaters.updateWeekViewsWidget(statsSiteProvider.siteModel.siteId)
        val cachedData = visitsAndViewsStore.getVisits(
            statsSiteProvider.siteModel,
            statsGranularity,
            LimitMode.All
        )

        // Get lower granularity model for chart values
        val lowerGranularityCachedData = if (statsGranularity != StatsGranularity.DAYS) {
            val selectedDate = getLastDate(cachedData)
            selectedDate?.let {
                visitsAndViewsStore.getVisits(
                    statsSiteProvider.siteModel,
                    lowerGranularity,
                    LimitMode.Top(OVERVIEW_ITEMS_TO_LOAD),
                    it,
                    false
                )
            }
        } else {
            null
        }

        return if (cachedData != null &&
            (statsGranularity == StatsGranularity.DAYS || lowerGranularityCachedData != null)
        ) {
            TrafficOverviewUiModel(cachedData, lowerGranularityCachedData)
        } else {
            null
        }
    }

    private fun getLastDate(model: VisitsAndViewsModel?): Date? {
        selectedDateProvider.getSelectedDate(statsGranularity)?.let { return it }
        val lastDateString = model?.dates?.lastOrNull()?.period
        return lastDateString?.let { statsDateFormatter.parseStatsDate(statsGranularity, it) }
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<TrafficOverviewUiModel> {
        val response = fetchVisit(statsGranularity, OVERVIEW_ITEMS_TO_LOAD, forced)
        val model = response.model

        // Fetch lower granularity model for chart values
        val lowerGranularityResponse = if (statsGranularity != StatsGranularity.DAYS) {
            val selectedDate = getLastDate(model)
            selectedDate?.let { fetchVisit(lowerGranularity, itemsToLoad, forced, it) }
        } else {
            null
        }
        val lowerGranularityModel = lowerGranularityResponse?.model

        val error = getErrorMessage(response) ?: getErrorMessage(lowerGranularityResponse)

        return when {
            error != null -> {
                selectedDateProvider.onDateLoadingFailed(statsGranularity)
                State.Error(error)
            }

            statsGranularity == StatsGranularity.DAYS && model != null && model.dates.isNotEmpty() -> {
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
                State.Data(TrafficOverviewUiModel(model))
            }

            model != null &&
                    model.dates.isNotEmpty() &&
                    lowerGranularityModel != null &&
                    lowerGranularityModel.dates.isNotEmpty() -> {
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
                State.Data(TrafficOverviewUiModel(model, lowerGranularityModel))
            }

            else -> {
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
                State.Empty()
            }
        }
    }

    private fun getErrorMessage(response: StatsStore.OnStatsFetched<VisitsAndViewsModel>?) =
        response?.error?.message ?: response?.error?.type?.name

    private suspend fun fetchVisit(
        granularity: StatsGranularity,
        quantity: Int,
        forced: Boolean,
        date: Date? = null
    ) = date?.let {
        visitsAndViewsStore.fetchVisits(
            statsSiteProvider.siteModel,
            granularity,
            LimitMode.Top(quantity),
            date,
            forced,
            false
        )
    } ?: visitsAndViewsStore.fetchVisits(
        statsSiteProvider.siteModel,
        granularity,
        LimitMode.Top(quantity),
        forced
    ).apply {
        error?.let { return@apply }
    }

    override fun buildUiModel(
        domainModel: TrafficOverviewUiModel,
        uiState: UiState
    ): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isNotEmpty()) {
            val dateFromProvider = selectedDateProvider.getSelectedDate(statsGranularity)
            val availableDates = domainModel.dates.map {
                statsDateFormatter.parseStatsDate(statsGranularity, it.period)
            }
            val selectedDate = dateFromProvider ?: availableDates.last()
            val index = availableDates.indexOf(selectedDate)

            selectedDateProvider.selectDate(selectedDate, availableDates, statsGranularity)
            val selectedItem = domainModel.dates.getOrNull(index) ?: domainModel.dates.last()

            if (statsGranularity == StatsGranularity.DAYS) {
                buildTodayCard(selectedItem, items)
            } else {
                val lowerGranularityDates = domainModel.lowerGranularityDates.map {
                    statsDateFormatter.parseStatsDate(lowerGranularity, it.period)
                }
                val barCount = getBarCount(lowerGranularityDates, selectedDate)
                buildGranularChart(
                    domainModel.lowerGranularityDates.takeLast(barCount),
                    uiState,
                    items,
                    selectedItem
                )
            }
        } else {
            selectedDateProvider.onDateLoadingFailed(statsGranularity)
            AppLog.e(AppLog.T.STATS, "There is no data to be shown in the overview block")
        }
        return items
    }

    private fun getBarCount(dates: List<Date>, selectedDate: Date) = if (statsGranularity == StatsGranularity.MONTHS) {
        val selectedCalendar = Calendar.getInstance().apply { time = selectedDate }
        val selectedYear = selectedCalendar.get(Calendar.YEAR)
        val selectedMonth = selectedCalendar.get(Calendar.MONTH)

        // Count the weeks that are in the selected month.
        dates.count {
            val weekEndCalendar = Calendar.getInstance().apply { time = it }
            val weekEndYear = weekEndCalendar.get(Calendar.YEAR)
            val weekEndMonth = weekEndCalendar.get(Calendar.MONTH)

            val weekStartCalendar = Calendar.getInstance().apply {
                time = it
                add(Calendar.DAY_OF_YEAR, @Suppress("MagicNumber") -6)
            }
            val weekStartYear = weekStartCalendar.get(Calendar.YEAR)
            val weekStartMonth = weekStartCalendar.get(Calendar.MONTH)

            (weekEndYear == selectedYear && weekEndMonth == selectedMonth) ||
                    (weekStartYear == selectedYear && weekStartMonth == selectedMonth)
        }
    } else {
        itemsToLoad
    }

    private fun buildTodayCard(
        selectedItem: VisitsAndViewsModel.PeriodData?,
        items: MutableList<BlockListItem>
    ) {
        val views = selectedItem?.views ?: 0
        val visitors = selectedItem?.visitors ?: 0
        val likes = selectedItem?.likes ?: 0
        val comments = selectedItem?.comments ?: 0

        items.add(BlockListItem.Title(R.string.stats_timeframe_today))
        items.add(
            BlockListItem.QuickScanItem(
                BlockListItem.QuickScanItem.Column(
                    R.string.stats_views,
                    statsUtils.toFormattedString(views)
                ),
                BlockListItem.QuickScanItem.Column(
                    R.string.stats_visitors,
                    statsUtils.toFormattedString(visitors)
                )
            )
        )

        items.add(
            BlockListItem.QuickScanItem(
                BlockListItem.QuickScanItem.Column(
                    R.string.stats_likes,
                    statsUtils.toFormattedString(likes)
                ),
                BlockListItem.QuickScanItem.Column(
                    R.string.stats_comments,
                    statsUtils.toFormattedString(comments)
                )
            )
        )
    }

    private fun buildGranularChart(
        dates: List<VisitsAndViewsModel.PeriodData>,
        uiState: UiState,
        items: MutableList<BlockListItem>,
        selectedItem: VisitsAndViewsModel.PeriodData
    ) {
        items.addAll(
            trafficOverviewMapper.buildChart(dates, lowerGranularity, this::onBarChartDrawn, uiState.selectedPosition)
        )
        items.add(
            trafficOverviewMapper.buildColumns(
                selectedItem,
                this::onColumnSelected,
                uiState.selectedPosition
            )
        )
    }

    @Suppress("MagicNumber")
    private fun onColumnSelected(position: Int) {
        val event = when (position) {
            0 -> AnalyticsTracker.Stat.STATS_OVERVIEW_TYPE_TAPPED_VIEWS
            1 -> AnalyticsTracker.Stat.STATS_OVERVIEW_TYPE_TAPPED_VISITORS
            2 -> AnalyticsTracker.Stat.STATS_OVERVIEW_TYPE_TAPPED_LIKES
            3 -> AnalyticsTracker.Stat.STATS_OVERVIEW_TYPE_TAPPED_COMMENTS
            else -> null
        }
        event?.let { analyticsTracker.trackWithGranularity(it, statsGranularity) }
        updateUiState { it.copy(selectedPosition = position) }
    }

    private fun onBarChartDrawn(visibleBarCount: Int) {
        updateUiState { it.copy(visibleBarCount = visibleBarCount) }
    }

    data class UiState(val selectedPosition: Int = 0, val visibleBarCount: Int? = null)

    data class TrafficOverviewUiModel(
        val period: String,
        val dates: List<VisitsAndViewsModel.PeriodData>,
        val lowerGranularityDates: List<VisitsAndViewsModel.PeriodData>
    ) {
        constructor(model: VisitsAndViewsModel, lowerGranularityModel: VisitsAndViewsModel? = null) : this(
            model.period,
            model.dates,
            lowerGranularityModel?.dates ?: listOf()
        )
    }

    class TrafficOverviewUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter,
        private val trafficOverviewMapper: TrafficOverviewMapper,
        private val visitsAndViewsStore: VisitsAndViewsStore,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val statsWidgetUpdaters: WidgetUpdater.StatsWidgetUpdaters,
        private val resourceProvider: ResourceProvider,
        private val statsUtils: StatsUtils
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
            TrafficOverviewUseCase(
                granularity,
                visitsAndViewsStore,
                selectedDateProvider,
                statsSiteProvider,
                statsDateFormatter,
                trafficOverviewMapper,
                mainDispatcher,
                backgroundDispatcher,
                analyticsTracker,
                statsWidgetUpdaters,
                resourceProvider,
                statsUtils
            )
    }
}
