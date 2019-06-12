package org.wordpress.android.ui.stats.refresh.lists.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView.ScaleType.FIT_START
import android.widget.RemoteViews
import com.bumptech.glide.request.target.AppWidgetTarget
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.OldStatsActivity
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.StatsActivity
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureFragment.ViewType
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.ICON
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.random.Random

private const val MIN_WIDTH = 250

class AllTimeWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteStore: SiteStore,
    private val imageManager: ImageManager,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val allTimeStore: AllTimeInsightsStore
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
        val layout = if (showColumns) {
            when (colorModeId) {
                DARK.ordinal -> R.layout.stats_widget_all_time_blocks_dark
                LIGHT.ordinal -> R.layout.stats_widget_all_time_blocks_light
                else -> R.layout.stats_widget_all_time_blocks_light
            }
        } else {
            when (colorModeId) {
                DARK.ordinal -> R.layout.stats_widget_all_time_list_dark
                LIGHT.ordinal -> R.layout.stats_widget_all_time_list_light
                else -> R.layout.stats_widget_all_time_list_light
            }
        }
        val views = RemoteViews(context.packageName, layout)
        val siteIconUrl = siteModel?.iconUrl
        val awt = AppWidgetTarget(context, R.id.widget_site_icon, views, appWidgetId)
        imageManager.load(awt, context, ICON, siteIconUrl ?: "", FIT_START)
        siteModel?.let {
            views.setOnClickPendingIntent(R.id.widget_title, getPendingSelfIntent(context, siteModel.id))
        }
        if (networkAvailable && siteModel != null) {
            if (showColumns) {
                views.setOnClickPendingIntent(R.id.widget_content, getPendingSelfIntent(context, siteModel.id))
                showColumns(appWidgetManager, appWidgetId, views, siteModel)
            } else {
                views.setPendingIntentTemplate(R.id.widget_content, getPendingTemplate(context))
                showList(appWidgetManager, views, context, appWidgetId, showColumns, colorModeId, siteId)
            }
        } else {
            showError(appWidgetManager, views, appWidgetId, networkAvailable, resourceProvider, context)
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
        views.setTextViewText(R.id.widget_views, allTimeInsights?.views?.toFormattedString() ?: "-")
        views.setTextViewText(R.id.widget_visitors, allTimeInsights?.visitors?.toFormattedString() ?: "-")
        views.setTextViewText(R.id.widget_posts, allTimeInsights?.posts?.toFormattedString() ?: "-")
        views.setTextViewText(
                R.id.widget_best_views,
                allTimeInsights?.viewsBestDayTotal?.toFormattedString() ?: "-"
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun showList(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        showChangeColumn: Boolean,
        colorModeId: Int,
        siteId: Long
    ) {
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error, View.GONE)
        val listIntent = Intent(context, WidgetService::class.java)
        listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        listIntent.putExtra(SHOW_CHANGE_VALUE_KEY, showChangeColumn)
        listIntent.putExtra(COLOR_MODE_KEY, colorModeId)
        listIntent.putExtra(VIEW_TYPE_KEY, ViewType.ALL_TIME_VIEWS.ordinal)
        listIntent.putExtra(SITE_ID_KEY, siteId)
        listIntent.data = Uri.parse(
                listIntent.toUri(Intent.URI_INTENT_SCHEME)
        )
        views.setRemoteAdapter(R.id.widget_content, listIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun showError(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetId: Int,
        networkAvailable: Boolean,
        resourceProvider: ResourceProvider,
        context: Context
    ) {
        views.setViewVisibility(R.id.widget_content, View.GONE)
        views.setViewVisibility(R.id.widget_error, View.VISIBLE)
        val errorMessage = if (!networkAvailable) {
            R.string.stats_widget_error_no_network
        } else {
            R.string.stats_widget_error_no_data
        }
        views.setTextViewText(
                R.id.widget_error_message,
                resourceProvider.getString(errorMessage)
        )
        val intentSync = Intent(context, StatsAllTimeWidget::class.java)
        intentSync.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        intentSync.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val pendingSync = PendingIntent.getBroadcast(
                context,
                Random(appWidgetId).nextInt(),
                intentSync,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_error, pendingSync)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingSelfIntent(context: Context, localSiteId: Int): PendingIntent {
        val intent = Intent(context, StatsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(WordPress.LOCAL_SITE_ID, localSiteId)
        intent.putExtra(OldStatsActivity.ARG_DESIRED_TIMEFRAME, StatsTimeframe.INSIGHTS)
        intent.putExtra(OldStatsActivity.ARG_LAUNCHED_FROM, OldStatsActivity.StatsLaunchedFrom.STATS_WIDGET)
        return PendingIntent.getActivity(
                context,
                Random(localSiteId).nextInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getPendingTemplate(context: Context): PendingIntent {
        val intent = Intent(context, StatsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun delete(appWidgetId: Int) {
        appPrefsWrapper.removeAppWidgetColorModeId(appWidgetId)
        appPrefsWrapper.removeAppWidgetSiteId(appWidgetId)
    }
}
