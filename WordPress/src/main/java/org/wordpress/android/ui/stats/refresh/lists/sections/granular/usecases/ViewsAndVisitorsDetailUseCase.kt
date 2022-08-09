package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.VIEWS_AND_VISITORS
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularStatefulUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases.ViewsAndVisitorsDetailUseCase.UiState
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.TOP_TIPS_URL
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.VIEWS_AND_VISITORS_ITEMS_TO_LOAD
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsMapper
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.ui.stats.refresh.utils.trackViewsVisitorsChips
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
class ViewsAndVisitorsDetailUseCase constructor(
    statsGranularity: StatsGranularity,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    selectedDateProvider: SelectedDateProvider,
    statsSiteProvider: StatsSiteProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val viewsAndVisitorsMapper: ViewsAndVisitorsMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val statsWidgetUpdaters: StatsWidgetUpdaters,
    private val resourceProvider: ResourceProvider
) : GranularStatefulUseCase<VisitsAndViewsModel, UiState>(
        VIEWS_AND_VISITORS,
        mainDispatcher,
        backgroundDispatcher,
        statsSiteProvider,
        selectedDateProvider,
        statsGranularity,
        UiState()
) {
    override fun buildLoadingItem(): List<BlockListItem> =
            listOf(
                    ValueItem(
                            value = "0",
                            unit = string.stats_views,
                            isFirst = true,
                            contentDescription = resourceProvider.getString(string.stats_loading_card)
                    )
            )

    override suspend fun loadCachedData(selectedDate: Date, site: SiteModel): VisitsAndViewsModel? {
        statsWidgetUpdaters.updateViewsWidget(statsSiteProvider.siteModel.siteId)
        val cachedData = visitsAndViewsStore.getVisits(
                site,
                DAYS,
                LimitMode.Top(VIEWS_AND_VISITORS_ITEMS_TO_LOAD),
                selectedDate
        )
        if (cachedData != null) {
            selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
        }
        return cachedData
    }

    override suspend fun fetchRemoteData(
        selectedDate: Date,
        site: SiteModel,
        forced: Boolean
    ): State<VisitsAndViewsModel> {
        val response = visitsAndViewsStore.fetchVisits(
                site,
                DAYS,
                LimitMode.Top(VIEWS_AND_VISITORS_ITEMS_TO_LOAD),
                selectedDate,
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

    @Suppress("LongMethod")
    override fun buildUiModel(
        domainModel: VisitsAndViewsModel,
        uiState: UiState
    ): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isNotEmpty()) {
            items.add(buildTitle())

            if (uiState.selectedPosition == 1) {
                items.add(viewsAndVisitorsMapper.buildChartLegendsPurple())
            } else {
                items.add(viewsAndVisitorsMapper.buildChartLegendsBlue())
            }

            val dateFromProvider = selectedDateProvider.getSelectedDate(statsGranularity)
            val visibleLineCount = uiState.visibleLineCount ?: domainModel.dates.size
            val availableDates = domainModel.dates.map {
                statsDateFormatter.parseStatsDate(
                        DAYS,
                        it.period
                )
            }
            val selectedDate = dateFromProvider ?: availableDates.last()
            val index = availableDates.indexOf(selectedDate)

            selectedDateProvider.selectDate(
                    selectedDate,
                    availableDates.takeLast(visibleLineCount),
                    DAYS
            )
            val selectedItem = domainModel.dates.getOrNull(index) ?: domainModel.dates.last()

            items.add(
                    viewsAndVisitorsMapper.buildTitle(
                            domainModel.dates,
                            DAYS,
                            selectedItem,
                            uiState.selectedPosition
                    )
            )
            items.addAll(
                    viewsAndVisitorsMapper.buildChart(
                            domainModel.dates,
                            DAYS,
                            this::onLineSelected,
                            this::onLineChartDrawn,
                            uiState.selectedPosition,
                            selectedItem.period
                    )
            )
            items.add(
                    viewsAndVisitorsMapper.buildInformation(
                            domainModel.dates,
                            uiState.selectedPosition,
                            this::onTopTipsLinkClick
                    )
            )
            items.add(
                    viewsAndVisitorsMapper.buildChips(
                            this::onChipSelected,
                            uiState.selectedPosition
                    )
            )
        } else {
            selectedDateProvider.onDateLoadingFailed(statsGranularity)
            AppLog.e(T.STATS, "There is no data to be shown in the overview block")
        }
        return items
    }

    private fun buildTitle() = TitleWithMore(
            string.stats_insights_views_and_visitors
    )

    private fun onLineSelected(period: String?) {
        analyticsTracker.trackGranular(
                AnalyticsTracker.Stat.STATS_VIEWS_AND_VISITORS_LINE_CHART_TAPPED,
                DAYS
        )
        if (period != null && period != "empty") {
            val selectedDate = statsDateFormatter.parseStatsDate(DAYS, period)
            selectedDateProvider.selectDate(
                    selectedDate,
                    DAYS
            )
        }
    }

    private fun onLineChartDrawn(visibleLineCount: Int) {
        updateUiState { it.copy(visibleLineCount = visibleLineCount) }
    }

    private fun onTopTipsLinkClick() {
        navigateTo(ViewUrl(TOP_TIPS_URL))
    }

    private fun onChipSelected(position: Int) {
        analyticsTracker.trackViewsVisitorsChips(position)
        updateUiState { it.copy(selectedPosition = position) }
    }

    data class UiState(val selectedPosition: Int = 0, val visibleLineCount: Int? = null)

    class ViewsAndVisitorsGranularUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter,
        private val viewsAndVisitorsMapper: ViewsAndVisitorsMapper,
        private val visitsAndViewsStore: VisitsAndViewsStore,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val statsWidgetUpdaters: StatsWidgetUpdaters,
        private val resourceProvider: ResourceProvider
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                ViewsAndVisitorsDetailUseCase(
                        granularity,
                        visitsAndViewsStore,
                        selectedDateProvider,
                        statsSiteProvider,
                        statsDateFormatter,
                        viewsAndVisitorsMapper,
                        mainDispatcher,
                        backgroundDispatcher,
                        analyticsTracker,
                        statsWidgetUpdaters,
                        resourceProvider
                )
    }
}
