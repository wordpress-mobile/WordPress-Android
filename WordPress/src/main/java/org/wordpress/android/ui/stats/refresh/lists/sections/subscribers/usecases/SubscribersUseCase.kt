package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.SubscriberType.SUBSCRIBERS
import org.wordpress.android.fluxc.store.stats.subscribers.SubscribersStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases.SubscribersUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.trackGranular
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

const val SUBSCRIBERS_ITEMS_TO_LOAD = 30

class SubscribersUseCase @Inject constructor(
    private val subscribersStore: SubscribersStore,
    private val selectedDateProvider: SelectedDateProvider,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val subscribersMapper: SubscribersMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : BaseStatsUseCase<SubscribersModel, UiState>(
    SUBSCRIBERS,
    mainDispatcher,
    backgroundDispatcher,
    UiState(),
    uiUpdateParams = listOf(UseCaseParam.SelectedDateParam(DAYS))
) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf()

    override suspend fun loadCachedData() = subscribersStore.getSubscribers(
        statsSiteProvider.siteModel,
        DAYS,
        LimitMode.Top(SUBSCRIBERS_ITEMS_TO_LOAD)
    )

    override suspend fun fetchRemoteData(forced: Boolean): State<SubscribersModel> {
        val response = subscribersStore.fetchSubscribers(
            statsSiteProvider.siteModel,
            DAYS,
            LimitMode.Top(SUBSCRIBERS_ITEMS_TO_LOAD),
            forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.dates.isNotEmpty() -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: SubscribersModel, uiState: UiState): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isNotEmpty()) {
            items.add(buildTitle())

            val dateFromProvider = selectedDateProvider.getSelectedDate(DAYS)
            val visibleLineCount = uiState.visibleLineCount ?: domainModel.dates.size
            val availableDates = domainModel.dates.map { statsDateFormatter.parseStatsDate(DAYS, it.period) }
            val selectedDate = dateFromProvider ?: availableDates.last()
            val index = availableDates.indexOf(selectedDate)

            selectedDateProvider.selectDate(selectedDate, availableDates.takeLast(visibleLineCount), DAYS)

            val selectedItem = domainModel.dates.getOrNull(index) ?: domainModel.dates.last()

            items.add(
                subscribersMapper.buildChart(
                    domainModel.dates.reversed(),
                    this::onLineSelected,
                    this::onLineChartDrawn,
                    selectedItem.period
                )
            )
        } else {
            selectedDateProvider.onDateLoadingFailed(DAYS)
            AppLog.e(T.STATS, "There is no data to be shown in the subscribers block")
        }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_subscribers_subscribers)

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

    data class UiState(val selectedPosition: Int = 0, val visibleLineCount: Int? = null)

    class SubscribersUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val statsSiteProvider: StatsSiteProvider,
        private val selectedDateProvider: SelectedDateProvider,
        private val statsDateFormatter: StatsDateFormatter,
        private val subscribersMapper: SubscribersMapper,
        private val subscribersStore: SubscribersStore,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
            SubscribersUseCase(
                subscribersStore,
                selectedDateProvider,
                statsSiteProvider,
                statsDateFormatter,
                subscribersMapper,
                mainDispatcher,
                backgroundDispatcher,
                analyticsTracker
            )
    }
}
