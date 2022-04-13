package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_OVERVIEW_ERROR
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.VIEWS_AND_VISITORS
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsUseCase.UiState
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toStatsSection
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.ceil

const val OVERVIEW_ITEMS_TO_LOAD = 15

class ViewsAndVisitorsUseCase
@Inject constructor(
    private val statsGranularity: StatsGranularity,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val viewsAndVisitorsMapper: ViewsAndVisitorsMapper,
    private val popupMenuHandler: ItemPopupMenuHandler,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val statsWidgetUpdaters: StatsWidgetUpdaters,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val resourceProvider: ResourceProvider
) : BaseStatsUseCase<VisitsAndViewsModel, UiState>(
        VIEWS_AND_VISITORS,
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
                LimitMode.All
        )
        if (cachedData != null) {
            logIfIncorrectData(cachedData, statsGranularity, statsSiteProvider.siteModel, false)
            selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
        }
        return cachedData
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
                logIfIncorrectData(model, statsGranularity, statsSiteProvider.siteModel, true)
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
                State.Data(model)
            }
            else -> {
                selectedDateProvider.onDateLoadingSucceeded(statsGranularity)
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
                        STATS_OVERVIEW_ERROR,
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
                    viewsAndVisitorsMapper.buildTitle(
                            selectedItem,
                            previousItem,
                            uiState.selectedPosition,
                            isLast = selectedItem == domainModel.dates.last(),
                            statsGranularity = statsGranularity
                    )
            )
            items.addAll(
                    viewsAndVisitorsMapper.buildChart(
                            domainModel.dates,
                            statsGranularity,
                            this::onBarSelected,
                            this::onBarChartDrawn,
                            uiState.selectedPosition,
                            selectedItem.period
                    )
            )
            items.add(
                    viewsAndVisitorsMapper.buildInformation()
            )
            items.add(
                    viewsAndVisitorsMapper.buildChips(
                            selectedItem,
                            this::onColumnSelected,
                            uiState.selectedPosition
                    )
            )
        } else {
            selectedDateProvider.onDateLoadingFailed(statsGranularity)
            AppLog.e(T.STATS, "There is no data to be shown in the overview block")
        }
        return items
    }

    private fun buildTitle() = TitleWithMore(string.stats_insights_views_and_visitors, moreAction = this::onMenuClick)

    private fun onMenuClick(view: View) {
        // TODO: Connect this to second level navigation later
        popupMenuHandler.onMenuClick(view, type)
    }

    private fun onBarSelected(period: String?) {
        analyticsTracker.trackGranular(
                AnalyticsTracker.Stat.STATS_OVERVIEW_BAR_CHART_TAPPED,
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

    private fun onColumnSelected(position: Int) {
        analyticsTracker.trackGranular(
                AnalyticsTracker.Stat.STATS_OVERVIEW_TYPE_TAPPED,
                statsGranularity
        )
        updateUiState { it.copy(selectedPosition = position) }
    }

    private fun onBarChartDrawn(visibleBarCount: Int) {
        updateUiState { it.copy(visibleBarCount = visibleBarCount) }
    }

    data class UiState(val selectedPosition: Int = 0, val visibleBarCount: Int? = null)

    class ViewsAndVisitorsUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter,
        private val viewsAndVisitorsMapper: ViewsAndVisitorsMapper,
        private val popupMenuHandler: ItemPopupMenuHandler,
        private val visitsAndViewsStore: VisitsAndViewsStore,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val statsWidgetUpdaters: StatsWidgetUpdaters,
        private val localeManagerWrapper: LocaleManagerWrapper,
        private val resourceProvider: ResourceProvider
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                ViewsAndVisitorsUseCase(
                        WEEKS,
                        visitsAndViewsStore,
                        selectedDateProvider,
                        statsSiteProvider,
                        statsDateFormatter,
                        viewsAndVisitorsMapper,
                        popupMenuHandler,
                        mainDispatcher,
                        backgroundDispatcher,
                        analyticsTracker,
                        statsWidgetUpdaters,
                        localeManagerWrapper,
                        resourceProvider
                )
    }
}
