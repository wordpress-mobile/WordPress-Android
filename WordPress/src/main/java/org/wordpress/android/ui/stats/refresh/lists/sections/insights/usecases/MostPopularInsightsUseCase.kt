package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.roundToInt

class MostPopularInsightsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val mostPopularStore: MostPopularInsightsStore,
    private val dateUtils: DateUtils,
    private val resourceProvider: ResourceProvider
) : StatelessUseCase<InsightsMostPopularModel>(MOST_POPULAR_DAY_AND_HOUR, mainDispatcher) {
    override suspend fun loadCachedData(site: SiteModel) {
        val dbModel = mostPopularStore.getMostPopularInsights(site)
        dbModel?.let { onModel(dbModel) }
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
        val response = mostPopularStore.fetchMostPopularInsights(site, forced)
        val model = response.model
        val error = response.error

        when {
            error != null -> onError(error.message ?: error.type.name)
            model != null -> onModel(model)
            else -> onEmpty()
        }
    }

    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_popular))

    override fun buildUiModel(domainModel: InsightsMostPopularModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_popular))
        items.add(
                ListItemWithIcon(
                        icon = R.drawable.ic_calendar_white_24dp,
                        text = dateUtils.getWeekDay(domainModel.highestDayOfWeek),
                        value = resourceProvider.getString(
                                R.string.stats_most_popular_percent_views,
                                domainModel.highestDayPercent.roundToInt()
                        ),
                        showDivider = true
                )
        )
        items.add(
                ListItemWithIcon(
                        icon = R.drawable.ic_time_white_24dp,
                        text = dateUtils.getHour(domainModel.highestHour),
                        value = resourceProvider.getString(
                                R.string.stats_most_popular_percent_views,
                                domainModel.highestHourPercent.roundToInt()
                        ),
                        showDivider = false
                )
        )
        return items
    }
}
