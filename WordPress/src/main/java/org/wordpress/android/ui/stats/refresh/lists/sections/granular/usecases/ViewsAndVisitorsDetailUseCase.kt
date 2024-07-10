package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightType.VIEWS_AND_VISITORS
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
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
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val viewsAndVisitorsMapper: ViewsAndVisitorsMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val statsWidgetUpdaters: StatsWidgetUpdaters,
    private val resourceProvider: ResourceProvider
) : BaseStatsUseCase<ViewsAndVisitorsDetailUseCase.ViewsAndVisitorsDetailUiModel, UiState>(
    VIEWS_AND_VISITORS,
    mainDispatcher,
    backgroundDispatcher,
    UiState(),
    fetchParams = listOf(UseCaseParam.SelectedDateParam(WEEKS))
) {
    override fun buildLoadingItem(): List<BlockListItem> =
        listOf(
            ValueItem(
                value = "0",
                unit = R.string.stats_views,
                isFirst = true,
                contentDescription = resourceProvider.getString(R.string.stats_loading_card)
            )
        )

    override suspend fun loadCachedData(): ViewsAndVisitorsDetailUiModel? {
        statsWidgetUpdaters.updateViewsWidget(statsSiteProvider.siteModel.siteId)
        val weeksCachedData = visitsAndViewsStore.getVisits(statsSiteProvider.siteModel, WEEKS, LimitMode.All)

        // Get DAYS model for chart values
        val selectedDate = getLastDate(weeksCachedData)
        val daysCachedData = selectedDate?.let {
            visitsAndViewsStore.getVisits(
                statsSiteProvider.siteModel,
                DAYS,
                LimitMode.Top(VIEWS_AND_VISITORS_ITEMS_TO_LOAD),
                it,
                false
            )
        }
        return if (weeksCachedData != null && daysCachedData != null) {
            ViewsAndVisitorsDetailUiModel(weeksCachedData, daysCachedData)
        } else {
            null
        }
    }

    private fun getLastDate(model: VisitsAndViewsModel?): Date? {
        selectedDateProvider.getSelectedDate(WEEKS)?.let { return it }
        val lastDateString = model?.dates?.lastOrNull()?.period
        return lastDateString?.let { statsDateFormatter.parseStatsDate(WEEKS, it) }
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<ViewsAndVisitorsDetailUiModel> {
        val weeksResponse = visitsAndViewsStore.fetchVisits(
            statsSiteProvider.siteModel,
            WEEKS,
            LimitMode.Top(VIEWS_AND_VISITORS_ITEMS_TO_LOAD),
            forced
        )
        val weeksModel = weeksResponse.model

        // Fetch DAYS model for chart values
        val selectedDate = getLastDate(weeksModel)
        val daysResponse = selectedDate?.let {
            visitsAndViewsStore.fetchVisits(
                statsSiteProvider.siteModel,
                DAYS,
                LimitMode.Top(VIEWS_AND_VISITORS_ITEMS_TO_LOAD),
                it,
                forced,
                false
            )
        }
        val daysModel = daysResponse?.model

        val error = getErrorMessage(weeksResponse) ?: getErrorMessage(daysResponse)

        return when {
            error != null -> {
                selectedDateProvider.onDateLoadingFailed(WEEKS)
                State.Error(error)
            }

            weeksModel != null &&
                weeksModel.dates.isNotEmpty() &&
                daysModel != null &&
                daysModel.dates.isNotEmpty() -> {
                selectedDateProvider.onDateLoadingSucceeded(WEEKS)
                State.Data(ViewsAndVisitorsDetailUiModel(weeksModel, daysModel))
            }
            else -> {
                selectedDateProvider.onDateLoadingSucceeded(WEEKS)
                State.Empty()
            }
        }
    }

    private fun getErrorMessage(response: StatsStore.OnStatsFetched<VisitsAndViewsModel>?) =
        response?.error?.message ?: response?.error?.type?.name

    @Suppress("LongMethod")
    override fun buildUiModel(
        domainModel: ViewsAndVisitorsDetailUiModel,
        uiState: UiState
    ): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isNotEmpty() && domainModel.daysDates.isNotEmpty()) {
            items.add(buildTitle())

            if (uiState.selectedPosition == 1) {
                items.add(viewsAndVisitorsMapper.buildChartLegendsPurple())
            } else {
                items.add(viewsAndVisitorsMapper.buildChartLegendsBlue())
            }

            val dateFromProvider = selectedDateProvider.getSelectedDate(WEEKS)
            val availableDates = domainModel.dates.map { statsDateFormatter.parseStatsDate(WEEKS, it.period) }
            val selectedDate = dateFromProvider ?: availableDates.last()
            val index = availableDates.indexOf(selectedDate)

            selectedDateProvider.selectDate(selectedDate, availableDates, WEEKS)
            val selectedItem = domainModel.daysDates.getOrNull(index) ?: domainModel.daysDates.last()

            items.add(
                viewsAndVisitorsMapper.buildWeekTitle(
                    domainModel.daysDates,
                    DAYS,
                    selectedItem,
                    uiState.selectedPosition
                )
            )
            items.addAll(
                viewsAndVisitorsMapper.buildChart(
                    domainModel.daysDates,
                    DAYS,
                    this::onLineSelected,
                    {},
                    uiState.selectedPosition,
                    selectedItem.period
                )
            )
            items.add(
                viewsAndVisitorsMapper.buildWeeksDetailInformation(
                    domainModel.daysDates,
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
            selectedDateProvider.onDateLoadingFailed(WEEKS)
            AppLog.e(T.STATS, "There is no data to be shown in the views & visitors block")
        }
        return items
    }

    private fun buildTitle() = TitleWithMore(
        R.string.stats_insights_views_and_visitors
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

    private fun onTopTipsLinkClick() {
        navigateTo(ViewUrl(TOP_TIPS_URL))
    }

    private fun onChipSelected(position: Int) {
        analyticsTracker.trackViewsVisitorsChips(position)
        updateUiState { it.copy(selectedPosition = position) }
    }

    data class UiState(val selectedPosition: Int = 0)

    data class ViewsAndVisitorsDetailUiModel(
        val period: String,
        val dates: List<VisitsAndViewsModel.PeriodData>,
        val daysDates: List<VisitsAndViewsModel.PeriodData>
    ) {
        constructor(
            weeksModel: VisitsAndViewsModel,
            daysModel: VisitsAndViewsModel
        ) : this(weeksModel.period, weeksModel.dates, daysModel.dates)
    }

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
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) = ViewsAndVisitorsDetailUseCase(
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
