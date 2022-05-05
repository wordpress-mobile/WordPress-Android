package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_FOLLOWERS
import org.wordpress.android.fluxc.store.stats.insights.SummaryStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.NavigationTarget.ViewTotalFollowersStats
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TitleWithMore
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueWithChartItem
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

class TotalFollowersUseCase @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val summaryStore: SummaryStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : StatelessUseCase<Int>(TOTAL_FOLLOWERS, mainDispatcher, bgDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_view_total_followers))

    override fun buildEmptyItem() = buildUiModel(0)

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

    override fun buildUiModel(domainModel: Int) =
            listOf(buildTitle(), ValueWithChartItem(domainModel))

    private fun buildTitle() = TitleWithMore(
            R.string.stats_view_total_followers,
            navigationAction = ListItemInteraction.create(this::onViewMoreClick)
    )

    private fun onViewMoreClick() {
        analyticsTracker.track(
                AnalyticsTracker.Stat.STATS_TOTAL_FOLLOWERS_VIEW_MORE_TAPPED,
                statsSiteProvider.siteModel
        )
        navigateTo(ViewTotalFollowersStats) // TODO: Connect this to proper second level navigation later
    }
}
