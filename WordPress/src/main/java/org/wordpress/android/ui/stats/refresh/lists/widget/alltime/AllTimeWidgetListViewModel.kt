package org.wordpress.android.ui.stats.refresh.lists.widget.alltime

import androidx.annotation.LayoutRes
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class AllTimeWidgetListViewModel
@Inject constructor(
    private val siteStore: SiteStore,
    private val allTimeStore: AllTimeInsightsStore,
    private val resourceProvider: ResourceProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val statsUtils: StatsUtils
) {
    private var siteId: Int? = null
    private var colorMode: Color = Color.LIGHT
    private var appWidgetId: Int? = null
    private val mutableData = mutableListOf<AllTimeItemUiModel>()
    val data: List<AllTimeItemUiModel> = mutableData
    fun start(siteId: Int, colorMode: Color, appWidgetId: Int) {
        this.siteId = siteId
        this.colorMode = colorMode
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
                        appWidgetId?.let {
                            appPrefsWrapper.setAppWidgetHasData(true, it)
                        }
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
        val layout = when (colorMode) {
            Color.DARK -> R.layout.stats_views_widget_item_dark
            Color.LIGHT -> R.layout.stats_views_widget_item_light
        }
        return listOf(
            AllTimeItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(R.string.stats_views),
                statsUtils.toFormattedString(domainModel.views, ONE_THOUSAND)
            ),
            AllTimeItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(R.string.stats_visitors),
                statsUtils.toFormattedString(domainModel.visitors, ONE_THOUSAND)
            ),
            AllTimeItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(R.string.posts),
                statsUtils.toFormattedString(domainModel.posts, ONE_THOUSAND)
            ),
            AllTimeItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(R.string.stats_insights_best_ever),
                statsUtils.toFormattedString(domainModel.viewsBestDayTotal, ONE_THOUSAND)
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
