package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_VIEWS_AND_VISITORS_ERROR
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType.INSIGHTS_VIEWS_AND_VISITORS
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewInsightDetails
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewUrl
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.INSIGHT_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsUseCase.UiState
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toStatsSection
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.ui.stats.refresh.utils.trackViewsVisitorsChips
import org.wordpress.android.ui.stats.refresh.utils.trackWithType
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.ceil

const val VIEWS_AND_VISITORS_ITEMS_TO_LOAD = 15
const val TOP_TIPS_URL = "https://wordpress.com/support/getting-more-views-and-traffic/"

class ViewsAndVisitorsUseCase
@Inject constructor(
    statsType: StatsType,
    private val statsGranularity: StatsGranularity,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val viewsAndVisitorsMapper: ViewsAndVisitorsMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val statsWidgetUpdaters: StatsWidgetUpdaters,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val resourceProvider: ResourceProvider,
    private val useCaseMode: UseCaseMode
) : BaseStatsUseCase<VisitsAndViewsModel, UiState>(
        statsType,
        mainDispatcher,
        backgroundDispatcher,
        UiState(),
        uiUpdateParams = listOf(UseCaseParam.SelectedDateParam(statsGranularity.toStatsSection()))
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

    override suspend fun loadCachedData(): VisitsAndViewsModel? {
        statsWidgetUpdaters.updateViewsWidget(statsSiteProvider.siteModel.siteId)
        val cachedData = visitsAndViewsStore.getVisits(
                statsSiteProvider.siteModel,
                statsGranularity,
                LimitMode.Top(VIEWS_AND_VISITORS_ITEMS_TO_LOAD)
        )
        if (cachedData != null) {
            logIfIncorrectData(cachedData, statsGranularity, statsSiteProvider.siteModel, false)
        }
        return cachedData
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<VisitsAndViewsModel> {
        val response = visitsAndViewsStore.fetchVisits(
                statsSiteProvider.siteModel,
                statsGranularity,
                LimitMode.Top(VIEWS_AND_VISITORS_ITEMS_TO_LOAD),
                forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> {
                State.Error(error.message ?: error.type.name)
            }
            model != null && model.dates.isNotEmpty() -> {
                logIfIncorrectData(model, statsGranularity, statsSiteProvider.siteModel, true)
                State.Data(model)
            }
            else -> {
                State.Empty()
            }
        }
    }

    /**
     * Track the incorrect data shown for some users
     * see https://github.com/wordpress-mobile/WordPress-Android/issues/11412
     */
    @Suppress("MagicNumber")
    private fun logIfIncorrectData(
        model: VisitsAndViewsModel,
        granularity: StatsGranularity,
        site: SiteModel,
        fetched: Boolean
    ) {
        model.dates.lastOrNull()?.let { lastDayData ->
            val yesterday = localeManagerWrapper.getCurrentCalendar()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            val lastDayDate = statsDateFormatter.parseStatsDate(granularity, lastDayData.period)
            if (lastDayDate.before(yesterday.time)) {
                val currentCalendar = localeManagerWrapper.getCurrentCalendar()
                val lastItemAge = ceil((currentCalendar.timeInMillis - lastDayDate.time) / 86400000.0)
                analyticsTracker.track(
                        STATS_VIEWS_AND_VISITORS_ERROR,
                        mapOf(
                                "stats_last_date" to statsDateFormatter.printStatsDate(lastDayDate),
                                "stats_current_date" to statsDateFormatter.printStatsDate(currentCalendar.time),
                                "stats_age_in_days" to lastItemAge.toInt(),
                                "is_jetpack_connected" to site.isJetpackConnected,
                                "is_atomic" to site.isWPComAtomic,
                                "action_source" to if (fetched) "remote" else "cached"
                        )
                )
            }
        }
    }

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
                        statsGranularity,
                        it.period
                )
            }
            val selectedDate = dateFromProvider ?: availableDates.last()
            val index = availableDates.indexOf(selectedDate)

            selectedDateProvider.selectDate(selectedDate, availableDates.takeLast(visibleLineCount), statsGranularity)

            val selectedItem = domainModel.dates.getOrNull(index) ?: domainModel.dates.last()

            items.add(
                    viewsAndVisitorsMapper.buildTitle(
                            domainModel.dates,
                            statsGranularity = statsGranularity,
                            selectedItem,
                            uiState.selectedPosition
                    )
            )
            items.addAll(
                    viewsAndVisitorsMapper.buildChart(
                            domainModel.dates,
                            statsGranularity,
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
            AppLog.e(T.STATS, "There is no data to be shown in the views and visitors block")
        }
        return items
    }

    private fun buildTitle() = TitleWithMore(
            string.stats_insights_views_and_visitors,
            navigationAction = if (useCaseMode == BLOCK) ListItemInteraction.create(this::onViewMoreClick) else null
    )

    private fun onViewMoreClick() {
        analyticsTracker.trackWithType(AnalyticsTracker.Stat.STATS_INSIGHTS_VIEW_MORE, InsightType.VIEWS_AND_VISITORS)
        navigateTo(
                ViewInsightDetails(
                        INSIGHT_DETAIL,
                        INSIGHTS_VIEWS_AND_VISITORS,
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity)
                )
        )
    }

    private fun onTopTipsLinkClick() {
        navigateTo(ViewUrl(TOP_TIPS_URL))
    }

    private fun onLineSelected(period: String?) {
        analyticsTracker.trackGranular(
                AnalyticsTracker.Stat.STATS_VIEWS_AND_VISITORS_LINE_CHART_TAPPED,
                statsGranularity
        )
        if (period != null && period != "empty") {
            val selectedDate = statsDateFormatter.parseStatsDate(statsGranularity, period)
            selectedDateProvider.selectDate(
                    selectedDate,
                    statsGranularity
            )
        }
    }

    private fun onChipSelected(position: Int) {
        analyticsTracker.trackViewsVisitorsChips(position)
        updateUiState { it.copy(selectedPosition = position) }
    }

    private fun onLineChartDrawn(visibleLineCount: Int) {
        updateUiState { it.copy(visibleLineCount = visibleLineCount) }
    }

    data class UiState(val selectedPosition: Int = 0, val visibleLineCount: Int? = null)

    class ViewsAndVisitorsUseCaseFactory
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
        private val localeManagerWrapper: LocaleManagerWrapper,
        private val resourceProvider: ResourceProvider
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                ViewsAndVisitorsUseCase(
                        InsightType.VIEWS_AND_VISITORS,
                        DAYS,
                        visitsAndViewsStore,
                        selectedDateProvider,
                        statsSiteProvider,
                        statsDateFormatter,
                        viewsAndVisitorsMapper,
                        mainDispatcher,
                        backgroundDispatcher,
                        analyticsTracker,
                        statsWidgetUpdaters,
                        localeManagerWrapper,
                        resourceProvider,
                        useCaseMode
                )
    }
}
