package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType
import org.wordpress.android.fluxc.store.stats.insights.SummaryStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewInsightDetails
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemGuideCard
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.GranularUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsUseCase.UiState
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toStatsSection
import org.wordpress.android.ui.stats.refresh.utils.trackWithType
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class TotalFollowersUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    statsType: StatsType,
    private val statsGranularity: StatsGranularity,
    private val selectedDateProvider: SelectedDateProvider,
    private val summaryStore: SummaryStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider,
    private val totalStatsMapper: TotalStatsMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val actionCardHandler: ActionCardHandler,
    private val useCaseMode: UseCaseMode
) : BaseStatsUseCase<Int, UiState>(
        statsType,
        mainDispatcher,
        bgDispatcher,
        UiState(),
        uiUpdateParams = listOf(UseCaseParam.SelectedDateParam(statsGranularity.toStatsSection()))
) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(TitleWithMore(R.string.stats_view_total_followers))

    override fun buildEmptyItem() = buildUiModel(0, UiState())

    override suspend fun loadCachedData() = summaryStore.getSummary(statsSiteProvider.siteModel)?.followers

    override suspend fun fetchRemoteData(forced: Boolean): State<Int> {
        val response = summaryStore.fetchSummary(statsSiteProvider.siteModel, forced)
        val model = response.model?.followers
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: Int, uiState: UiState): List<BlockListItem> {
        addActionCard(domainModel)
        val items = mutableListOf<BlockListItem>()
        items.add(buildTitle())
        items.add(ValueWithChartItem(value = domainModel.toString(), extraBottomMargin = true))
        if (totalStatsMapper.shouldShowFollowersGuideCard(domainModel)) {
            items.add(ListItemGuideCard(resourceProvider.getString(string.stats_insights_followers_guide_card)))
        }
        return items
    }

    private fun addActionCard(domainModel: Int) {
        if (domainModel <= 1) actionCardHandler.display(InsightType.ACTION_GROW)
    }

    private fun buildTitle() = TitleWithMore(
            R.string.stats_view_total_followers,
            navigationAction = if (useCaseMode == VIEW_ALL) null else ListItemInteraction.create(this::onViewMoreClick)
    )

    private fun onViewMoreClick() {
        analyticsTracker.trackWithType(AnalyticsTracker.Stat.STATS_INSIGHTS_VIEW_MORE, InsightType.TOTAL_FOLLOWERS)
        navigateTo(
                ViewInsightDetails(
                        StatsSection.TOTAL_FOLLOWERS_DETAIL,
                        StatsViewType.TOTAL_FOLLOWERS,
                        statsGranularity,
                        selectedDateProvider.getSelectedDate(statsGranularity)
                )
        )
    }

    class TotalFollowersUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val selectedDateProvider: SelectedDateProvider,
        private val summaryStore: SummaryStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val resourceProvider: ResourceProvider,
        private val totalStatsMapper: TotalStatsMapper,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val actionCardHandler: ActionCardHandler
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) = TotalFollowersUseCase(
                mainDispatcher,
                backgroundDispatcher,
                InsightType.TOTAL_FOLLOWERS,
                WEEKS,
                selectedDateProvider,
                summaryStore,
                statsSiteProvider,
                resourceProvider,
                totalStatsMapper,
                analyticsTracker,
                actionCardHandler,
                useCaseMode
        )
    }

    class TotalFollowersGranularUseCaseFactory @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val selectedDateProvider: SelectedDateProvider,
        private val summaryStore: SummaryStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val resourceProvider: ResourceProvider,
        private val totalStatsMapper: TotalStatsMapper,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val actionCardHandler: ActionCardHandler
    ) : GranularUseCaseFactory {
        override fun build(granularity: StatsGranularity, useCaseMode: UseCaseMode) =
                TotalFollowersUseCase(
                        mainDispatcher,
                        backgroundDispatcher,
                        TimeStatsType.OVERVIEW,
                        granularity,
                        selectedDateProvider,
                        summaryStore,
                        statsSiteProvider,
                        resourceProvider,
                        totalStatsMapper,
                        analyticsTracker,
                        actionCardHandler,
                        useCaseMode
        )
    }
}
