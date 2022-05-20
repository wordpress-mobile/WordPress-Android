package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ACTION_REMINDER
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.ActionCardHandler
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.config.StatsRevampV2FeatureConfig
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.roundToInt

class MostPopularInsightsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val mostPopularStore: MostPopularInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val dateUtils: DateUtils,
    private val resourceProvider: ResourceProvider,
    private val statsRevampV2FeatureConfig: StatsRevampV2FeatureConfig,
    private val popupMenuHandler: ItemPopupMenuHandler,
    private val actionCardHandler: ActionCardHandler
) : StatelessUseCase<InsightsMostPopularModel>(MOST_POPULAR_DAY_AND_HOUR, mainDispatcher, backgroundDispatcher) {
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

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override fun buildUiModel(domainModel: InsightsMostPopularModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        val highestDayPercent = resourceProvider.getString(
                R.string.stats_most_popular_percent_views,
                domainModel.highestDayPercent.roundToInt()
        )
        val highestHourPercent = resourceProvider.getString(
                R.string.stats_most_popular_percent_views,
                domainModel.highestHourPercent.roundToInt()
        )
        items.add(buildTitle())
        items.add(
                QuickScanItem(
                        Column(
                                R.string.stats_insights_best_day,
                                dateUtils.getWeekDay(domainModel.highestDayOfWeek),
                                if (statsRevampV2FeatureConfig.isEnabled()) highestDayPercent else null,
                                highestDayPercent
                        ),
                        Column(
                                R.string.stats_insights_best_hour,
                                dateUtils.getHour(domainModel.highestHour),
                                if (statsRevampV2FeatureConfig.isEnabled()) highestHourPercent else null,
                                highestHourPercent
                        )
                )
        )
        addActionCard()
        return items
    }

    private fun addActionCard() {
        val model = mostPopularStore.getMostPopularInsights(statsSiteProvider.siteModel)
        val popular = model?.let { it.highestDayOfWeek > 0 || it.highestHour > 0 } == true
        if (popular) actionCardHandler.addCard(ACTION_REMINDER) else actionCardHandler.removeCard(ACTION_REMINDER)
    }

    private fun buildTitle() = Title(
            textResource = if (statsRevampV2FeatureConfig.isEnabled()) {
                R.string.stats_insights_popular_title
            } else {
                R.string.stats_insights_popular
            },
            menuAction = if (statsRevampV2FeatureConfig.isEnabled()) null else this::onMenuClick)

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }
}
