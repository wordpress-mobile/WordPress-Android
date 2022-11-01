package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_INSIGHTS_TOTAL_LIKES_GUIDE_TAPPED
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.All
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_LIKES
import org.wordpress.android.fluxc.store.stats.insights.LatestPostInsightsStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsViewType
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewInsightDetails
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewPost
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemGuideCard
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text.Clickable
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.InsightUseCaseFactory
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.LatestPostSummaryUseCase.LinkClickParams
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.trackWithType
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class TotalLikesUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val latestPostStore: LatestPostInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider,
    private val totalStatsMapper: TotalStatsMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val statsWidgetUpdaters: StatsWidgetUpdaters
) : StatelessUseCase<VisitsAndViewsModel>(TOTAL_LIKES, mainDispatcher, bgDispatcher) {
    override fun buildLoadingItem() = listOf(TitleWithMore(string.stats_view_total_likes))

    override fun buildEmptyItem() = listOf(buildTitle(), Empty())

    override suspend fun loadCachedData(): VisitsAndViewsModel? {
        statsWidgetUpdaters.updateViewsWidget(statsSiteProvider.siteModel.siteId)
        return visitsAndViewsStore.getVisits(
                statsSiteProvider.siteModel,
                DAYS,
                All
        )
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<VisitsAndViewsModel> {
        val response = visitsAndViewsStore.fetchVisits(
                statsSiteProvider.siteModel,
                DAYS,
                LimitMode.Top(TOTAL_LIKES_ITEMS_TO_LOAD),
                forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.dates.isNotEmpty() -> {
                State.Data(model)
            }
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: VisitsAndViewsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        if (domainModel.dates.isNotEmpty()) {
            items.add(buildTitle())
            items.add(totalStatsMapper.buildTotalLikesValue(domainModel.dates))
            totalStatsMapper.buildTotalLikesInformation(domainModel.dates)?.let { items.add(it) }
            if (totalStatsMapper.shouldShowLikesGuideCard(domainModel.dates)) {
                buildLatestPostGuideCard(items, this::onLinkClicked)
            }
        } else {
            AppLog.e(T.STATS, "There is no data to be shown in the total likes block")
        }
        return items
    }

    private fun buildTitle() = TitleWithMore(
            string.stats_view_total_likes,
            navigationAction = ListItemInteraction.create(this::onViewMoreClick)
    )

    private fun buildLatestPostGuideCard(
        items: MutableList<BlockListItem>,
        navigationAction: (params: LinkClickParams) -> Unit
    ) {
        val postModel = latestPostStore.getLatestPostInsights(statsSiteProvider.siteModel)
        postModel?.let {
            if (it.postTitle.isNotBlank() && it.postLikeCount > 0) {
                val htmlTitle = HtmlCompat.fromHtml(it.postTitle, HtmlCompat.FROM_HTML_MODE_LEGACY)
                items.add(
                        ListItemGuideCard(
                                text = resourceProvider.getString(
                                        if (it.postLikeCount <= 1) {
                                            string.stats_insights_like_guide_card
                                        } else {
                                            string.stats_insights_likes_guide_card
                                        },
                                        htmlTitle,
                                        it.postLikeCount
                                ),
                                links = listOf(Clickable(
                                                link = htmlTitle.toString(),
                                                navigationAction = ListItemInteraction.create(
                                                        data = LinkClickParams(it.postId, it.postURL),
                                                        action = navigationAction
                                                )
                                        )
                                ),
                                bolds = listOf(it.postLikeCount.toString())
                        ))
            }
        }
    }

    private fun onLinkClicked(params: LinkClickParams) {
        analyticsTracker.track(STATS_INSIGHTS_TOTAL_LIKES_GUIDE_TAPPED)
        navigateTo(ViewPost(params.postId, params.postUrl))
    }

    private fun onViewMoreClick() {
        analyticsTracker.trackWithType(AnalyticsTracker.Stat.STATS_INSIGHTS_VIEW_MORE, TOTAL_LIKES)
        navigateTo(
                ViewInsightDetails(
                        StatsSection.TOTAL_LIKES_DETAIL,
                        StatsViewType.TOTAL_LIKES,
                        null,
                        null
                )
        )
    }

    companion object {
        private const val TOTAL_LIKES_ITEMS_TO_LOAD = 15
    }

    class TotalLikesUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val visitsAndViewsStore: VisitsAndViewsStore,
        private val latestPostStore: LatestPostInsightsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val resourceProvider: ResourceProvider,
        private val totalStatsMapper: TotalStatsMapper,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val statsWidgetUpdaters: StatsWidgetUpdaters
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                TotalLikesUseCase(
                        mainDispatcher,
                        backgroundDispatcher,
                        visitsAndViewsStore,
                        latestPostStore,
                        statsSiteProvider,
                        resourceProvider,
                        totalStatsMapper,
                        analyticsTracker,
                        statsWidgetUpdaters
                )
    }
}
