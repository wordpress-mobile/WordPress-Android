package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import android.view.View
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater.StatsWidgetUpdaters
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject
import javax.inject.Named

class AllTimeStatsUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val allTimeStore: AllTimeInsightsStore,
    private val statsSiteProvider: StatsSiteProvider,
    private val statsDateFormatter: StatsDateFormatter,
    private val statsWidgetUpdaters: StatsWidgetUpdaters,
    private val statsUtils: StatsUtils,
    private val popupMenuHandler: ItemPopupMenuHandler
) : StatelessUseCase<InsightsAllTimeModel>(ALL_TIME_STATS, mainDispatcher, backgroundDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_all_time_stats))

    override fun buildEmptyItem(): List<BlockListItem> {
        return listOf(buildTitle(), Empty())
    }

    override suspend fun loadCachedData(): InsightsAllTimeModel? {
        statsWidgetUpdaters.updateAllTimeWidget(statsSiteProvider.siteModel.siteId)
        return allTimeStore.getAllTimeInsights(statsSiteProvider.siteModel)
    }

    override suspend fun fetchRemoteData(forced: Boolean): State<InsightsAllTimeModel> {
        val response = allTimeStore.fetchAllTimeInsights(statsSiteProvider.siteModel, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.hasData() -> State.Data(
                model
            )
            else -> State.Empty()
        }
    }

    private fun InsightsAllTimeModel.hasData() =
        this.posts > 0 || this.views > 0 || this.visitors > 0 || this.viewsBestDayTotal > 0

    override fun buildUiModel(domainModel: InsightsAllTimeModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(buildTitle())

        val hasPosts = domainModel.posts > 0
        val hasViews = domainModel.views > 0
        val hasVisitors = domainModel.visitors > 0
        val hasViewsBestDayTotal = domainModel.viewsBestDayTotal > 0
        if (!hasPosts && !hasViews && !hasVisitors && !hasViewsBestDayTotal) {
            items.add(Empty())
        } else {
            items.add(
                QuickScanItem(
                    Column(R.string.stats_views, statsUtils.toFormattedString(domainModel.views)),
                    Column(R.string.stats_visitors, statsUtils.toFormattedString(domainModel.visitors))
                )
            )
            val tooltip = if (domainModel.viewsBestDay.isNotEmpty()) {
                statsDateFormatter.printDate(domainModel.viewsBestDay)
            } else {
                null
            }
            items.add(
                QuickScanItem(
                    Column(R.string.posts, statsUtils.toFormattedString(domainModel.posts)),
                    Column(
                        R.string.stats_insights_best_ever,
                        statsUtils.toFormattedString(domainModel.viewsBestDayTotal),
                        tooltip
                    )
                )
            )
        }
        return items
    }

    private fun buildTitle() = Title(R.string.stats_insights_all_time_stats, menuAction = this::onMenuClick)

    private fun onMenuClick(view: View) {
        popupMenuHandler.onMenuClick(view, type)
    }
}
