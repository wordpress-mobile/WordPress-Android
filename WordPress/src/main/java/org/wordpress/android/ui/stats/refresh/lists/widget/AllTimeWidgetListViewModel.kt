@file:JvmName("StatsWidgetConfigureViewModelKt")

package org.wordpress.android.ui.stats.refresh.lists.widget

import androidx.annotation.LayoutRes
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class AllTimeWidgetListViewModel
@Inject constructor(
    private val siteStore: SiteStore,
    private val allTimeStore: AllTimeInsightsStore,
    private val resourceProvider: ResourceProvider
) {
    private var siteId: Int? = null
    private var colorModeId: Int? = null
    private var appWidgetId: Int? = null
    private val mutableData = mutableListOf<AllTimeItemUiModel>()
    val data: List<AllTimeItemUiModel> = mutableData
    fun start(siteId: Int, colorModeId: Int, appWidgetId: Int) {
        this.siteId = siteId
        this.colorModeId = colorModeId
        this.appWidgetId = appWidgetId
    }

    fun onDataSetChanged(onError: (appWidgetId: Int) -> Unit) {
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
                    onError(nonNullAppWidgetId)
                }
            }
        }
    }

    private fun buildListItemUiModel(
        domainModel: InsightsAllTimeModel,
        localSiteId: Int
    ): List<AllTimeItemUiModel> {
        val layout = when (colorModeId) {
            Color.DARK.ordinal -> R.layout.stats_views_widget_item_dark
            Color.LIGHT.ordinal -> R.layout.stats_views_widget_item_light
            else -> R.layout.stats_views_widget_item_light
        }
        return listOf(
                AllTimeItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.stats_views),
                        domainModel.views.toFormattedString()
                ),
                AllTimeItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.stats_visitors),
                        domainModel.visitors.toFormattedString()
                ),
                AllTimeItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.posts),
                        domainModel.posts.toFormattedString()
                ),
                AllTimeItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.stats_insights_best_ever),
                        domainModel.viewsBestDayTotal.toFormattedString()
                )
        )
    }

    data class AllTimeItemUiModel(
        @LayoutRes val layout: Int,
        val localSiteId: Int,
        val key: String,
        val value: String
    )
}
