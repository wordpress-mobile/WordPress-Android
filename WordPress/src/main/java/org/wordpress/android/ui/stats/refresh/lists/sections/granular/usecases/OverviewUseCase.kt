package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.OVERVIEW
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

const val OVERVIEW_ITEMS_TO_LOAD = 15

class OverviewUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val overviewMapper: OverviewMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val resourceProvider: ResourceProvider
) : BaseStatsUseCase<VisitsAndViewsModel, UiState>(
        OVERVIEW,
        mainDispatcher,
        UiState()
) {
    init {
        uiState.addSource(selectedDateProvider.granularSelectedDateChanged(statsGranularity)) {
            onUiState()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> =
            listOf(
                    ValueItem(
                            value = 0.toFormattedString(),
                            unit = R.string.stats_views,
                            isFirst = true,
                            contentDescription = resourceProvider.getString(R.string.stats_loading_card)
                    )
            )

    override suspend fun loadCachedData(): VisitsAndViewsModel? {
        return visitsAndViewsStore.getVisits(
                statsSiteProvider.siteModel,
                statsGranularity,
                LimitMode.All
        )
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<VisitsAndViewsModel> {
        val response = visitsAndViewsStore.fetchVisits(
                statsSiteProvider.siteModel,
                statsGranularity,
                LimitMode.Top(OVERVIEW_ITEMS_TO_LOAD),
                forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> {
                selectedDateProvider.onDateLoadingFailed(statsGranularity)
                State.Error(error.message ?: error.type.name)
            }
            model != null && model.dates.isNotEmpty() -> {
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
                State.Data(model)
            }
            else -> {
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
                State.Empty()
            }
        }
    }

    override fun buildUiModel(domainModel: VisitsAndViewsModel, uiState: UiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isNotEmpty()) {
            val dateFromProvider = selectedDateProvider.getSelectedDate(statsGranularity)
            val visibleBarCount = uiState.visibleBarCount ?: domainModel.dates.size
            val availableDates = domainModel.dates.map {
                statsDateFormatter.parseStatsDate(
                        statsGranularity,
                        it.period
                )
            }
            val selectedDate = dateFromProvider ?: availableDates.last()
            val index = availableDates.indexOf(selectedDate)

            selectedDateProvider.selectDate(
                    selectedDate,
                    availableDates.takeLast(visibleBarCount),
                    statsGranularity
            )
            val selectedItem = domainModel.dates.getOrNull(index) ?: domainModel.dates.last()
            val previousItem = domainModel.dates.getOrNull(domainModel.dates.indexOf(selectedItem) - 1)
            items.add(
                    overviewMapper.buildTitle(
                            selectedItem,
                            previousItem,
                            uiState.selectedPosition,
                            isLast = selectedItem == domainModel.dates.last(),
                            statsGranularity = statsGranularity
                    )
            )
            items.addAll(
                    overviewMapper.buildChart(
                            domainModel.dates,
                            statsGranularity,
                            this::onBarSelected,
                            this::onBarChartDrawn,
                            uiState.selectedPosition,
                            selectedItem.period
                    )
            )
            items.add(overviewMapper.buildColumns(selectedItem, this::onColumnSelected, uiState.selectedPosition))
        } else {
            selectedDateProvider.onDateLoadingFailed(statsGranularity)
            AppLog.e(T.STATS, "There is no data to be shown in the overview block")
        }
        return items
    }

    private fun onBarSelected(period: String?) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_OVERVIEW_BAR_CHART_TAPPED, statsGranularity)
        if (period != null && period != "empty") {
            val selectedDate = statsDateFormatter.parseStatsDate(statsGranularity, period)
            selectedDateProvider.selectDate(
                    selectedDate,
                    statsGranularity
            )
        }
    }

    private fun onColumnSelected(position: Int) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_OVERVIEW_TYPE_TAPPED, statsGranularity)
        updateUiState { it.copy(selectedPosition = position) }
    }

    private fun onBarChartDrawn(visibleBarCount: Int) {
        updateUiState { it.copy(visibleBarCount = visibleBarCount) }
    }

    data class UiState(val selectedPosition: Int = 0, val visibleBarCount: Int? = null)

    class OverviewUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter,
        private val overviewMapper: OverviewMapper,
        private val visitsAndViewsStore: VisitsAndViewsStore,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val resourceProvider: ResourceProvider
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                OverviewUseCase(
                        granularity,
                        visitsAndViewsStore,
                        selectedDateProvider,
                        statsSiteProvider,
                        statsDateFormatter,
                        overviewMapper,
                        mainDispatcher,
                        analyticsTracker,
                        resourceProvider
                )
    }
}
