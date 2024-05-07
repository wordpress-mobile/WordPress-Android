package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_FOLLOWERS
import org.wordpress.android.fluxc.store.stats.subscribers.SubscribersStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewInsightDetails
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemGuideCard
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.stats.refresh.utils.trackWithType
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class TotalFollowersUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val subscribersStore: SubscribersStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider,
    private val totalStatsMapper: TotalStatsMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val actionCardHandler: ActionCardHandler,
    private val useCaseMode: UseCaseMode,
    private val statsUtils: StatsUtils
) : StatelessUseCase<Int>(TOTAL_FOLLOWERS, mainDispatcher, bgDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(TitleWithMore(R.string.stats_view_total_subscribers))

    override fun buildEmptyItem() = buildUiModel(0)

    override suspend fun loadCachedData(): Int {
        val model = subscribersStore.getSubscribers(
            statsSiteProvider.siteModel,
            StatsGranularity.DAYS,
            LimitMode.Top(1)
        )
        return getTotalSubscribersFromModel(model)
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<Int> {
        val response = subscribersStore.fetchSubscribers(
            statsSiteProvider.siteModel,
            StatsGranularity.DAYS,
            LimitMode.Top(1),
            forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.dates.isNotEmpty() -> State.Data(getTotalSubscribersFromModel(model))
            else -> State.Empty()
        }
    }

    private fun getTotalSubscribersFromModel(model: SubscribersModel?): Int {
        val dates = model?.dates
        return if (dates.isNullOrEmpty()) 0 else dates.first().subscribers.toInt()
    }

    override fun buildUiModel(domainModel: Int): List<BlockListItem> {
        addActionCard(domainModel)
        val items = mutableListOf<BlockListItem>()
        items.add(buildTitle())
        items.add(ValueWithChartItem(
            value = statsUtils.toFormattedString(domainModel, MILLION),
            extraBottomMargin = true
        ))
        if (totalStatsMapper.shouldShowFollowersGuideCard(domainModel)) {
            items.add(ListItemGuideCard(resourceProvider.getString(R.string.stats_insights_subscribers_guide_card)))
        }
        return items
    }

    private fun addActionCard(domainModel: Int) {
        if (domainModel <= 1) actionCardHandler.display(InsightType.ACTION_GROW)
    }

    private fun buildTitle() = TitleWithMore(
        R.string.stats_view_total_subscribers,
        navigationAction = if (useCaseMode == VIEW_ALL) null else ListItemInteraction.create(this::onViewMoreClick)
    )

    private fun onViewMoreClick() {
        analyticsTracker.trackWithType(AnalyticsTracker.Stat.STATS_INSIGHTS_VIEW_MORE, TOTAL_FOLLOWERS)
        navigateTo(
            ViewInsightDetails(
                StatsSection.TOTAL_FOLLOWERS_DETAIL,
                StatsViewType.TOTAL_FOLLOWERS,
                null,
                null
            )
        )
    }

    class TotalFollowersUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val subscribersStore: SubscribersStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val resourceProvider: ResourceProvider,
        private val totalStatsMapper: TotalStatsMapper,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val actionCardHandler: ActionCardHandler,
        private val statsUtils: StatsUtils
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) = TotalFollowersUseCase(
            mainDispatcher,
            backgroundDispatcher,
            subscribersStore,
            statsSiteProvider,
            resourceProvider,
            totalStatsMapper,
            analyticsTracker,
            actionCardHandler,
            useCaseMode,
            statsUtils
        )
    }
}
