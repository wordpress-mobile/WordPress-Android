package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_INSIGHTS_TOTAL_LIKES_GUIDE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_TOTAL_LIKES_ERROR
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
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
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
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

class TotalLikesUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val latestPostStore: LatestPostInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val totalStatsMapper: TotalStatsMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val statsWidgetUpdaters: StatsWidgetUpdaters,
    private val localeManagerWrapper: LocaleManagerWrapper
) : StatelessUseCase<VisitsAndViewsModel>(TOTAL_LIKES, mainDispatcher, bgDispatcher) {
    override fun buildLoadingItem() = listOf(TitleWithMore(string.stats_view_total_likes))

    override fun buildEmptyItem() = listOf(buildTitle(), Empty())

    override suspend fun loadCachedData(): VisitsAndViewsModel? {
        statsWidgetUpdaters.updateViewsWidget(statsSiteProvider.siteModel.siteId)
        val cachedData = visitsAndViewsStore.getVisits(
                statsSiteProvider.siteModel,
                DAYS,
                LimitMode.All
        )
        if (cachedData != null) {
            logIfIncorrectData(cachedData, statsSiteProvider.siteModel, false)
        }
        return cachedData
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<VisitsAndViewsModel> {
        val response = visitsAndViewsStore.fetchVisits(
                statsSiteProvider.siteModel,
                DAYS,
                LimitMode.Top(TotalStatsMapper.DAY_COUNT_TOTAL),
                forced
        )
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.dates.isNotEmpty() -> {
                logIfIncorrectData(model, statsSiteProvider.siteModel, true)
                State.Data(model)
            }
            else -> State.Empty()
        }
    }

    /**
     * Track the incorrect data shown for some users
     * see https://github.com/wordpress-mobile/WordPress-Android/issues/11412
     */
    @Suppress("MagicNumber")
    private fun logIfIncorrectData(
        model: VisitsAndViewsModel,
        site: SiteModel,
        fetched: Boolean
    ) {
        model.dates.lastOrNull()?.let { lastDayData ->
            val yesterday = localeManagerWrapper.getCurrentCalendar()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            val lastDayDate = statsDateFormatter.parseStatsDate(DAYS, lastDayData.period)
            if (lastDayDate.before(yesterday.time)) {
                val currentCalendar = localeManagerWrapper.getCurrentCalendar()
                val lastItemAge = ceil((currentCalendar.timeInMillis - lastDayDate.time) / 86400000.0)
                analyticsTracker.track(
                        STATS_TOTAL_LIKES_ERROR,
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
            if (it.postTitle.isNotBlank()) {
                items.add(
                        ListItemGuideCard(
                                text = resourceProvider.getString(
                                        string.stats_insights_likes_guide_card,
                                        it.postTitle,
                                        it.postLikeCount
                                ),
                                links = listOf(Clickable(
                                                link = it.postTitle,
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

    class TotalLikesUseCaseFactory
    @Inject constructor(
        @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
        @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
        private val visitsAndViewsStore: VisitsAndViewsStore,
        private val latestPostStore: LatestPostInsightsStore,
        private val statsSiteProvider: StatsSiteProvider,
        private val resourceProvider: ResourceProvider,
        private val statsDateFormatter: StatsDateFormatter,
        private val totalStatsMapper: TotalStatsMapper,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val statsWidgetUpdaters: StatsWidgetUpdaters,
        private val localeManagerWrapper: LocaleManagerWrapper
    ) : InsightUseCaseFactory {
        override fun build(useCaseMode: UseCaseMode) =
                TotalLikesUseCase(
                        mainDispatcher,
                        backgroundDispatcher,
                        visitsAndViewsStore,
                        latestPostStore,
                        statsSiteProvider,
                        resourceProvider,
                        statsDateFormatter,
                        totalStatsMapper,
                        analyticsTracker,
                        statsWidgetUpdaters,
                        localeManagerWrapper
                )
    }
}
