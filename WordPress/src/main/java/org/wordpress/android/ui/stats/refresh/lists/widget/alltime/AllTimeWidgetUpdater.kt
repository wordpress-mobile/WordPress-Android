package org.wordpress.android.ui.stats.refresh.lists.widget.alltime

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.StatsTimeframe.INSIGHTS
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.ViewType.ALL_TIME_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.WidgetUtils
import org.wordpress.android.ui.stats.refresh.utils.MILLION
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

private const val EMPTY_VALUE = "-"

class AllTimeWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteStore: SiteStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val allTimeStore: AllTimeInsightsStore,
    private val widgetUtils: WidgetUtils
) : WidgetUpdater {
    override fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val wideView = widgetUtils.isWidgetWiderThanLimit(appWidgetManager, appWidgetId)
        val colorMode = appPrefsWrapper.getAppWidgetColor(appWidgetId) ?: LIGHT
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        val siteModel = siteStore.getSiteBySiteId(siteId)
        val networkAvailable = networkUtilsWrapper.isNetworkAvailable()
        val views = RemoteViews(context.packageName, widgetUtils.getLayout(wideView, colorMode))
        views.setTextViewText(R.id.widget_title, resourceProvider.getString(R.string.stats_insights_all_time_stats))
        widgetUtils.setSiteIcon(siteModel, context, views, appWidgetId)
        siteModel?.let {
            views.setOnClickPendingIntent(
                    R.id.widget_title,
                    widgetUtils.getPendingSelfIntent(context, siteModel.id, INSIGHTS)
            )
        }
        if (networkAvailable && siteModel != null) {
            if (wideView) {
                views.setOnClickPendingIntent(
                        R.id.widget_content,
                        widgetUtils.getPendingSelfIntent(context, siteModel.id, INSIGHTS)
                )
                showColumns(appWidgetManager, appWidgetId, views, siteModel)
            } else {
                widgetUtils.showList(
                        appWidgetManager,
                        views,
                        context,
                        appWidgetId,
                        colorMode,
                        siteModel.id,
                        ALL_TIME_VIEWS,
                        wideView
                )
            }
        } else {
            widgetUtils.showError(appWidgetManager, views, appWidgetId, networkAvailable, resourceProvider, context)
        }
    }

    override fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val viewsWidget = ComponentName(context, StatsAllTimeWidget::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(viewsWidget)
        for (appWidgetId in allWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun showColumns(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        views: RemoteViews,
        site: SiteModel
    ) {
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error, View.GONE)
        loadCachedAllTimeInsights(appWidgetManager, appWidgetId, site, views)
        GlobalScope.launch {
            runBlocking {
                allTimeStore.fetchAllTimeInsights(site)
            }
            loadCachedAllTimeInsights(appWidgetManager, appWidgetId, site, views)
        }
    }

    private fun loadCachedAllTimeInsights(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        site: SiteModel,
        views: RemoteViews
    ) {
        val allTimeInsights = allTimeStore.getAllTimeInsights(site)
        views.setTextViewText(R.id.first_block_title, resourceProvider.getString(R.string.stats_views))
        val viewsValue = allTimeInsights?.views?.toFormattedString(MILLION) ?: EMPTY_VALUE
        views.setTextViewText(R.id.first_block_value, viewsValue)
        views.setTextViewText(R.id.second_block_title, resourceProvider.getString(R.string.stats_visitors))
        val visitorsValue = allTimeInsights?.visitors?.toFormattedString(MILLION) ?: EMPTY_VALUE
        views.setTextViewText(R.id.second_block_value, visitorsValue)
        views.setTextViewText(R.id.third_block_title, resourceProvider.getString(R.string.posts))
        val postsValue = allTimeInsights?.posts?.toFormattedString(MILLION) ?: EMPTY_VALUE
        views.setTextViewText(R.id.third_block_value, postsValue)
        views.setTextViewText(R.id.fourth_block_title, resourceProvider.getString(R.string.stats_insights_best_ever))
        val bestDayValue = allTimeInsights?.viewsBestDayTotal?.toFormattedString(MILLION) ?: EMPTY_VALUE
        views.setTextViewText(R.id.fourth_block_value, bestDayValue)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun delete(appWidgetId: Int) {
        appPrefsWrapper.removeAppWidgetColorModeId(appWidgetId)
        appPrefsWrapper.removeAppWidgetSiteId(appWidgetId)
    }
}
