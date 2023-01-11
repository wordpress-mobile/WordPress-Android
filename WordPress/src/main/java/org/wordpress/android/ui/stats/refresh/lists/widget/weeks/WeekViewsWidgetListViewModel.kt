package org.wordpress.android.ui.stats.refresh.lists.widget.weeks

import androidx.annotation.LayoutRes
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WeekViewsWidgetListViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val visitsAndViewsStore: VisitsAndViewsStore,
    private val resourceProvider: ResourceProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val statsUtils: StatsUtils
) {
    private var siteId: Int? = null
    private var colorMode: Color = Color.LIGHT
    private var appWidgetId: Int? = null
    private val mutableData = mutableListOf<WeekItemUiModel>()
    val data: List<WeekItemUiModel> = mutableData
    fun start(siteId: Int, colorMode: Color, appWidgetId: Int) {
        this.siteId = siteId
        this.colorMode = colorMode
        this.appWidgetId = appWidgetId
    }

    @Suppress("NestedBlockDepth")
    fun onDataSetChanged(onError: (appWidgetId: Int) -> Unit) {
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
                    onError(nonNullAppWidgetId)
                }
            }
        }
    }

    private fun buildListItemUiModel(
        domainModel: VisitsAndViewsModel,
        localSiteId: Int
    ): List<WeekItemUiModel> {
        val layout = when (colorMode) {
            Color.DARK -> R.layout.stats_views_widget_item_dark
            Color.LIGHT -> R.layout.stats_views_widget_item_light
        }
        return listOf(
            WeekItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(R.string.stats_views),
                statsUtils.toFormattedString(domainModel.dates.last().views, ONE_THOUSAND)
            ),
            WeekItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(R.string.stats_visitors),
                statsUtils.toFormattedString(domainModel.dates.last().visitors, ONE_THOUSAND)
            ),
            WeekItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(R.string.likes),
                statsUtils.toFormattedString(domainModel.dates.last().likes, ONE_THOUSAND)
            ),
            WeekItemUiModel(
                layout,
                localSiteId,
                resourceProvider.getString(R.string.stats_comments),
                statsUtils.toFormattedString(domainModel.dates.last().comments, ONE_THOUSAND)
            )
        )
    }

    data class WeekItemUiModel(
        @LayoutRes val layout: Int,
        val localSiteId: Int,
        val key: String,
        val value: String
    )

    companion object {
        const val LIST_ITEM_COUNT = 7
    }
}
