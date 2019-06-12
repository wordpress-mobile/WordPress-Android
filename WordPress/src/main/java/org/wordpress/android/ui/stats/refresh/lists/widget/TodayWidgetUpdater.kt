package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.ImageView.ScaleType.FIT_START
import android.widget.RemoteViews
import com.bumptech.glide.request.target.AppWidgetTarget
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.ICON
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

private const val MIN_WIDTH = 250

class TodayWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteStore: SiteStore,
    private val imageManager: ImageManager,
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
        val minWidth = appWidgetManager.getAppWidgetOptions(appWidgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
        val showColumns = minWidth > MIN_WIDTH
        val colorModeId = appPrefsWrapper.getAppWidgetColorModeId(appWidgetId)
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        val siteModel = siteStore.getSiteBySiteId(siteId)
        val networkAvailable = networkUtilsWrapper.isNetworkAvailable()
        val layout = widgetUtils.getLayout(showColumns, colorModeId)
        val views = RemoteViews(context.packageName, layout)
        views.setTextViewText(R.id.widget_title, resourceProvider.getString(R.string.stats_insights_today_stats))
        val siteIconUrl = siteModel?.iconUrl
        val awt = AppWidgetTarget(context, R.id.widget_site_icon, views, appWidgetId)
        imageManager.load(awt, context, ICON, siteIconUrl ?: "", FIT_START)
        siteModel?.let {
            views.setOnClickPendingIntent(R.id.widget_title, widgetUtils.getPendingSelfIntent(context, siteModel.id))
        }
        if (networkAvailable && siteModel != null) {
            if (showColumns) {
                views.setOnClickPendingIntent(
                        R.id.widget_content,
                        widgetUtils.getPendingSelfIntent(context, siteModel.id)
                )
                showColumns(appWidgetManager, appWidgetId, views, siteModel)
            } else {
                widgetUtils.showList(appWidgetManager, views, context, appWidgetId, colorModeId, siteId)
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
