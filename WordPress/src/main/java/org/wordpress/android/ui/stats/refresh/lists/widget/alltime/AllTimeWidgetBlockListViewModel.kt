package org.wordpress.android.ui.stats.refresh.lists.widget.alltime

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetBlockListProvider.BlockItemUiModel
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetBlockListProvider.WidgetBlockListViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class AllTimeWidgetBlockListViewModel
@Inject constructor(
    private val siteStore: SiteStore,
    private val allTimeStore: AllTimeInsightsStore,
    private val resourceProvider: ResourceProvider,
    private val allTimeWidgetUpdater: AllTimeWidgetUpdater
) : WidgetBlockListViewModel {
    private var siteId: Int? = null
    private var colorMode: Color = Color.LIGHT
    private var appWidgetId: Int? = null
    private val mutableData = mutableListOf<BlockItemUiModel>()
    override val data: List<BlockItemUiModel> = mutableData
    override fun start(siteId: Int, colorMode: Color, appWidgetId: Int) {
        this.siteId = siteId
        this.colorMode = colorMode
        this.appWidgetId = appWidgetId
    }

    override fun onDataSetChanged(context: Context) {
        siteId?.apply {
            val site = siteStore.getSiteByLocalId(this)
            if (site != null) {
                runBlocking {
                    allTimeStore.fetchAllTimeInsights(site)
                }
                allTimeStore.getAllTimeInsights(site)?.let { visitsAndViewsModel ->
                    val uiModels = buildListItemUiModel(visitsAndViewsModel, this)
                    if (uiModels != data) {
                        mutableData.clear()
                        mutableData.addAll(uiModels)
                    }
                }
            } else {
                appWidgetId?.let { nonNullAppWidgetId ->
                    allTimeWidgetUpdater.updateAppWidget(context, appWidgetId = nonNullAppWidgetId)
                }
            }
        }
    }

    private fun buildListItemUiModel(
        domainModel: InsightsAllTimeModel,
        localSiteId: Int
    ): List<BlockItemUiModel> {
        val layout = when (colorMode) {
            Color.DARK -> R.layout.stats_widget_block_item_dark
            Color.LIGHT -> R.layout.stats_widget_block_item_light
        }
        return listOf(
                BlockItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.stats_views),
                        domainModel.views.toFormattedString(MILLION),
                        resourceProvider.getString(R.string.stats_visitors),
                        domainModel.visitors.toFormattedString(MILLION)
                ),
                BlockItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.posts),
                        domainModel.posts.toFormattedString(MILLION),
                        resourceProvider.getString(R.string.stats_insights_best_ever),
                        domainModel.viewsBestDayTotal.toFormattedString(MILLION)
                )
        )
    }
}
