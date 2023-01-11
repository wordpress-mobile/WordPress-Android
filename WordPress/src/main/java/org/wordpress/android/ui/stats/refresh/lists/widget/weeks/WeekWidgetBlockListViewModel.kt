package org.wordpress.android.ui.stats.refresh.lists.widget.weeks

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetBlockListProvider.BlockItemUiModel
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetBlockListProvider.WidgetBlockListViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WeekWidgetBlockListViewModel
@Inject constructor(
    private val siteStore: SiteStore,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val resourceProvider: ResourceProvider,
    private val weekViewsWidgetUpdater: WeekViewsWidgetUpdater,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val statsUtils: StatsUtils
) : WidgetBlockListViewModel {
    private var siteId: Int? = null
    private var colorMode: Color = Color.LIGHT
    private var appWidgetId: Int? = null
    private val mutableData = mutableListOf<BlockItemUiModel>()
    override val data: List<BlockItemUiModel> = mutableData
    override fun start(
        siteId: Int,
        colorMode: Color,
        appWidgetId: Int
    ) {
        this.siteId = siteId
        this.colorMode = colorMode
        this.appWidgetId = appWidgetId
    }

    @Suppress("NestedBlockDepth")
    override fun onDataSetChanged(context: Context) {
        siteId?.let { nonNullSiteId ->
            val site = siteStore.getSiteByLocalId(nonNullSiteId)
            if (site != null) {
                runBlocking {
                    visitsAndViewsStore.fetchVisits(site, WEEKS, LimitMode.Top(1))
                }
                visitsAndViewsStore.getVisits(site, WEEKS, LimitMode.All)?.let { visitsAndViewsModel ->
                    val uiModels = buildListItemUiModel(visitsAndViewsModel, nonNullSiteId)
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
                    weekViewsWidgetUpdater.updateAppWidget(context, appWidgetId = nonNullAppWidgetId)
                }
            }
        }
    }

    private fun buildListItemUiModel(
        domainModel: VisitsAndViewsModel,
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
                resourceProvider.getString(string.stats_views),
                statsUtils.toFormattedString(domainModel.dates.last().views, MILLION),
                resourceProvider.getString(string.stats_visitors),
                statsUtils.toFormattedString(domainModel.dates.last().visitors, MILLION)
            ),
            BlockItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(string.likes),
                statsUtils.toFormattedString(domainModel.dates.last().likes, MILLION),
                resourceProvider.getString(string.stats_comments),
                statsUtils.toFormattedString(domainModel.dates.last().comments, MILLION)
            )
        )
    }
}
