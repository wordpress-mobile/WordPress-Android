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
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.roundToInt

class AnnualSiteStatsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val mostPopularStore: MostPopularInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val resourceProvider: ResourceProvider
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

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_annual_site_stats))

    override fun buildUiModel(domainModel: YearsInsightsModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_annual_site_stats))
        items.add(
                QuickScanItem(
                        Column(
                                string.stats_insights_best_day,
                                dateUtils.getWeekDay(domainModel.highestDayOfWeek),
                                resourceProvider.getString(
                                        string.stats_most_popular_percent_views,
                                        domainModel.highestDayPercent.roundToInt()
                                )
                        ),
                        Column(
                                string.stats_insights_best_hour,
                                dateUtils.getHour(domainModel.highestHour),
                                resourceProvider.getString(
                                        string.stats_most_popular_percent_views,
                                        domainModel.highestHourPercent.roundToInt()
                                )
                        )
                )
        )
        return items
    }
}
