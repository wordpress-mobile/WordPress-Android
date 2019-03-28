package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

class TodayStatsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val todayStore: TodayInsightsStore,
    private val statsSiteProvider: StatsSiteProvider
) : StatelessUseCase<VisitsModel>(TODAY_STATS, mainDispatcher) {
    override suspend fun loadCachedData(): VisitsModel? {
        return todayStore.getTodayInsights(statsSiteProvider.siteModel)
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<VisitsModel> {
        val response = todayStore.fetchTodayInsights(statsSiteProvider.siteModel, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.hasData() -> State.Data(model)
            else -> State.Empty()
        }
    }

    private fun VisitsModel.hasData() =
            this.comments > 0 || this.views > 0 || this.likes > 0 || this.visitors > 0

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_today_stats))

    override fun buildUiModel(domainModel: VisitsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_today_stats))

        val hasViews = domainModel.views > 0
        val hasVisitors = domainModel.visitors > 0
        val hasLikes = domainModel.likes > 0
        val hasComments = domainModel.comments > 0
        if (!hasViews && !hasVisitors && !hasLikes && !hasComments) {
            items.add(Empty())
        } else {
            items.add(
                    QuickScanItem(
                            Column(R.string.stats_views, domainModel.views.toFormattedString()),
                            Column(R.string.stats_visitors, domainModel.visitors.toFormattedString())
                    )
            )
            items.add(
                    QuickScanItem(
                            Column(R.string.stats_likes, domainModel.likes.toFormattedString()),
                            Column(R.string.stats_comments, domainModel.comments.toFormattedString())
                    )
            )
        }
        return items
    }
}
