package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.roundToInt

class MostPopularInsightsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val mostPopularStore: MostPopularInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val dateUtils: DateUtils,
    private val resourceProvider: ResourceProvider
) : StatelessUseCase<InsightsMostPopularModel>(MOST_POPULAR_DAY_AND_HOUR, mainDispatcher) {
    override suspend fun loadCachedData(): InsightsMostPopularModel? {
        return mostPopularStore.getMostPopularInsights(statsSiteProvider.siteModel)
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<InsightsMostPopularModel> {
        val response = mostPopularStore.fetchMostPopularInsights(statsSiteProvider.siteModel, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null -> State.Data(model)
            else -> State.Empty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_popular))

    override fun buildUiModel(domainModel: InsightsMostPopularModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_popular))
        items.add(
                QuickScanItem(
                        Column(
                                R.string.stats_insights_best_day,
                                dateUtils.getWeekDay(domainModel.highestDayOfWeek),
                                resourceProvider.getString(
                                        R.string.stats_most_popular_percent_views,
                                        domainModel.highestDayPercent.roundToInt()
                                )
                        ),
                        Column(
                                R.string.stats_insights_best_hour,
                                dateUtils.getHour(domainModel.highestHour),
                                resourceProvider.getString(
                                        R.string.stats_most_popular_percent_views,
                                        domainModel.highestHourPercent.roundToInt()
                                )
                        )
                )
        )
        return items
    }
}
