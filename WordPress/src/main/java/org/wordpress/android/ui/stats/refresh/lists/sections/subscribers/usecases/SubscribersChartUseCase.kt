package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.SubscriberType.SUBSCRIBERS_CHART
import org.wordpress.android.fluxc.store.stats.subscribers.SubscribersStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

const val SUBSCRIBERS_CHART_ITEMS_TO_LOAD = 30

class SubscribersChartUseCase @Inject constructor(
    private val subscribersStore: SubscribersStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val subscribersMapper: SubscribersMapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : StatelessUseCase<SubscribersModel>(SUBSCRIBERS_CHART, mainDispatcher, backgroundDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_subscribers))

    override suspend fun loadCachedData() = subscribersStore.getSubscribers(
        statsSiteProvider.siteModel,
        DAYS,
        LimitMode.Top(SUBSCRIBERS_CHART_ITEMS_TO_LOAD)
    )

    override suspend fun fetchRemoteData(forced: Boolean): State<SubscribersModel> {
        val response = subscribersStore.fetchSubscribers(
            statsSiteProvider.siteModel,
            DAYS,
            LimitMode.Top(SUBSCRIBERS_CHART_ITEMS_TO_LOAD),
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

    override fun buildUiModel(domainModel: SubscribersModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isEmpty()) {
            AppLog.e(T.STATS, "There is no data to be shown in the subscribers chart block")
        } else {
            items.add(buildTitle())

            items.add(subscribersMapper.buildChart(domainModel.dates, this::onLineSelected))
        }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_view_subscriber_growth)

    private fun onLineSelected() {
        analyticsTracker.track(AnalyticsTracker.Stat.STATS_SUBSCRIBERS_CHART_TAPPED)
    }

    class SubscribersUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val statsSiteProvider: StatsSiteProvider,
        private val subscribersMapper: SubscribersMapper,
        private val subscribersStore: SubscribersStore,
        private val analyticsTracker: AnalyticsTrackerWrapper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
            SubscribersChartUseCase(
                subscribersStore,
                statsSiteProvider,
                subscribersMapper,
                mainDispatcher,
                backgroundDispatcher,
                analyticsTracker
            )
    }
}
