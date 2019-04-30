package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ANNUAL_SITE_STATS
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import javax.inject.Inject
import javax.inject.Named

class AnnualSiteStatsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val mostPopularStore: MostPopularInsightsStore,
    private val statsSiteProvider: StatsSiteProvider
) : StatelessUseCase<YearsInsightsModel>(ANNUAL_SITE_STATS, mainDispatcher) {
    override suspend fun loadCachedData(): YearsInsightsModel? {
        return mostPopularStore.getYearsInsights(statsSiteProvider.siteModel)
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<YearsInsightsModel> {
        val response = mostPopularStore.fetchYearsInsights(statsSiteProvider.siteModel, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_this_year_site_stats))

    override fun buildUiModel(domainModel: YearsInsightsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_this_year_site_stats))
        val lastYear = domainModel.years.last()
        items.add(
                QuickScanItem(
                        Column(
                                string.stats_insights_year,
                                lastYear.year
                        ),
                        Column(
                                string.stats_insights_posts,
                                lastYear.totalPosts.toFormattedString()
                        )
                )
        )
        items.add(
                QuickScanItem(
                        Column(
                                string.stats_insights_total_comments,
                                lastYear.totalComments.toFormattedString()
                        ),
                        Column(
                                string.stats_insights_average_comments,
                                lastYear.avgComments?.toFormattedString() ?: "0"
                        )
                )
        )
        items.add(
                QuickScanItem(
                        Column(
                                string.stats_insights_total_likes,
                                lastYear.totalLikes.toFormattedString()
                        ),
                        Column(
                                string.stats_insights_average_likes,
                                lastYear.avgLikes?.toFormattedString() ?: "0"
                        )
                )
        )
        items.add(
                QuickScanItem(
                        Column(
                                string.stats_insights_total_words,
                                lastYear.totalWords.toFormattedString()
                        ),
                        Column(
                                string.stats_insights_average_words,
                                lastYear.avgWords?.toFormattedString() ?: "0"
                        )
                )
        )
        return items
    }
}
