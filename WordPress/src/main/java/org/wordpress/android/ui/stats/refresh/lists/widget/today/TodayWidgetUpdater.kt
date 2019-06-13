package org.wordpress.android.ui.stats.refresh.lists.widget.today

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.ViewType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.WidgetUtils
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class TodayWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteStore: SiteStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val todayInsightsStore: TodayInsightsStore,
    private val widgetUtils: WidgetUtils
) : WidgetUpdater {
    override fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val showColumns = widgetUtils.isWidgetWiderThanLimit(appWidgetManager, appWidgetId)
        val colorMode = appPrefsWrapper.getAppWidgetColor(appWidgetId) ?: Color.LIGHT
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        val siteModel = siteStore.getSiteBySiteId(siteId)
        val networkAvailable = networkUtilsWrapper.isNetworkAvailable()
        val views = RemoteViews(context.packageName, widgetUtils.getLayout(showColumns, colorMode))
        views.setTextViewText(R.id.widget_title, resourceProvider.getString(R.string.stats_insights_today_stats))
        widgetUtils.setSiteIcon(siteModel, context, views, appWidgetId)
        siteModel?.let {
            views.setOnClickPendingIntent(
                    R.id.widget_title, widgetUtils.getPendingSelfIntent(
                    context,
                    siteModel.id,
                    StatsTimeframe.INSIGHTS
            )
            )
        }
        if (networkAvailable && siteModel != null) {
            if (showColumns) {
                views.setOnClickPendingIntent(
                        R.id.widget_content,
                        widgetUtils.getPendingSelfIntent(context, siteModel.id, StatsTimeframe.INSIGHTS)
                )
                showColumns(appWidgetManager, appWidgetId, views, siteModel)
            } else {
                widgetUtils.showList(
                        appWidgetManager,
                        views,
                        context,
                        appWidgetId,
                        colorMode,
                        siteId,
                        ViewType.TODAY_VIEWS
                )
            }
        } else {
            widgetUtils.showError(appWidgetManager, views, appWidgetId, networkAvailable, resourceProvider, context)
        }
    }

    override fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val viewsWidget = ComponentName(context, StatsTodayWidget::class.java)
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
        loadCachedAllTimeInsights(appWidgetManager, appWidgetId, site, views)
        GlobalScope.launch {
            runBlocking {
                todayInsightsStore.fetchTodayInsights(site)
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
        val todayInsights = todayInsightsStore.getTodayInsights(site)
        views.setTextViewText(R.id.first_block_title, resourceProvider.getString(R.string.stats_views))
        views.setTextViewText(R.id.first_block_value, todayInsights?.views?.toFormattedString() ?: "-")
        views.setTextViewText(R.id.second_block_title, resourceProvider.getString(R.string.stats_visitors))
        views.setTextViewText(R.id.second_block_value, todayInsights?.visitors?.toFormattedString() ?: "-")
        views.setTextViewText(R.id.third_block_title, resourceProvider.getString(R.string.posts))
        views.setTextViewText(R.id.third_block_value, todayInsights?.posts?.toFormattedString() ?: "-")
        views.setTextViewText(R.id.fourth_block_title, resourceProvider.getString(R.string.stats_comments))
        views.setTextViewText(R.id.fourth_block_value, todayInsights?.comments?.toFormattedString() ?: "-")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun delete(appWidgetId: Int) {
        appPrefsWrapper.removeAppWidgetColorModeId(appWidgetId)
        appPrefsWrapper.removeAppWidgetSiteId(appWidgetId)
    }
}
