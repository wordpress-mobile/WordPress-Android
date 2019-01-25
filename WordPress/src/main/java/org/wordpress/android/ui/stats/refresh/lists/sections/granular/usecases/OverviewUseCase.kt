package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.OVERVIEW
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.UseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.OverviewUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

private const val PAGE_SIZE = 15

class OverviewUseCase
constructor(
    private val statsGranularity: StatsGranularity,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val overviewMapper: OverviewMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : StatefulUseCase<VisitsAndViewsModel, UiState>(
        OVERVIEW,
        mainDispatcher,
        UiState()
) {
    override fun buildLoadingItem(): List<BlockListItem> =
            listOf(
                    Title(
                            text = statsDateFormatter.printGranularDate(
                                    selectedDateProvider.getCurrentDate(),
                                    statsGranularity
                            )
                    )
            )

    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = visitsAndViewsStore.getVisits(
                site,
                selectedDateProvider.getCurrentDate(),
                statsGranularity
        )
        dbModel?.let { onModel(it) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = visitsAndViewsStore.fetchVisits(
                site,
                PAGE_SIZE,
                selectedDateProvider.getCurrentDate(),
                statsGranularity,
                forced
        )
        val model = response.model
        val error = response.error

        when {
            error != null -> onError(error.message ?: error.type.name)
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override fun buildStatefulUiModel(domainModel: VisitsAndViewsModel, uiState: UiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isNotEmpty()) {
            val selectedDate = uiState.selectedDate ?: domainModel.dates.last().period
            selectedDateProvider.selectDate(
                    statsDateFormatter.parseStatsDate(statsGranularity, selectedDate),
                    statsGranularity
            )
            val selectedItem = domainModel.dates.find { it.period == uiState.selectedDate }
                    ?: domainModel.dates.last()
            items.add(
                    overviewMapper.buildTitle(
                            selectedItem.period,
                            selectedDate,
                            domainModel.period,
                            statsGranularity
                    )
            )
            items.add(
                    overviewMapper.buildChart(
                            domainModel,
                            statsGranularity,
                            this::onBarSelected,
                            uiState.selectedPosition,
                            selectedDate
                    )
            )
            items.add(overviewMapper.buildColumns(selectedItem, this::onColumnSelected, uiState.selectedPosition))
        } else {
            AppLog.e(T.STATS, "There is no data to be shown in the overview block")
        }
        return items
    }

    private fun onBarSelected(period: String?) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_OVERVIEW_BAR_CHART_TAPPED, statsGranularity)
        if (period != null && period != "empty") {
            updateUiState { previousState -> previousState.copy(selectedDate = period) }
            selectedDateProvider.selectDate(
                    statsDateFormatter.parseStatsDate(statsGranularity, period),
                    statsGranularity
            )
        } else {
            onUiState(null)
        }
    }

    private fun onColumnSelected(position: Int) {
        analyticsTracker.trackGranular(AnalyticsTracker.Stat.STATS_OVERVIEW_TYPE_TAPPED, statsGranularity)
        updateUiState { previousState -> previousState.copy(selectedPosition = position) }
    }

    data class UiState(
        val selectedPosition: Int = 0,
        val selectedDate: String? = null
    )

    class OverviewUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter,
        private val overviewMapper: OverviewMapper,
        private val visitsAndViewsStore: VisitsAndViewsStore,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : UseCaseFactory {
        override fun build(granularity: StatsGranularity) =
                OverviewUseCase(
                        granularity,
                        visitsAndViewsStore,
                        selectedDateProvider,
                        statsDateFormatter,
                        overviewMapper,
                        mainDispatcher,
                        analyticsTracker
                )
    }
}
