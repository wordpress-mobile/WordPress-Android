package org.wordpress.android.ui.stats.refresh.lists.widget.views

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.StatsTimeframe.DAY
import org.wordpress.android.ui.stats.refresh.StatsActivity
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.ViewType.WEEK_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.WidgetUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class ViewsWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteStore: SiteStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
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
        val layout = when (colorMode) {
            DARK -> R.layout.stats_widget_list_dark
            LIGHT -> R.layout.stats_widget_list_light
        }
        val views = RemoteViews(context.packageName, layout)
        views.setTextViewText(R.id.widget_title, resourceProvider.getString(R.string.stats_views))
        widgetUtils.setSiteIcon(siteModel, context, views, appWidgetId)
        siteModel?.let {
            views.setOnClickPendingIntent(
                    R.id.widget_title,
                    widgetUtils.getPendingSelfIntent(context, siteModel.id, DAY)
            )
            views.setPendingIntentTemplate(R.id.widget_content, getPendingTemplate(context))
        }
        if (networkAvailable && siteModel != null) {
            widgetUtils.showList(
                    appWidgetManager,
                    views,
                    context,
                    appWidgetId,
                    colorMode,
                    siteId,
                    WEEK_VIEWS,
                    wideView
            )
        } else {
            widgetUtils.showError(appWidgetManager, views, appWidgetId, networkAvailable, resourceProvider, context)
        }
    }

    override fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val viewsWidget = ComponentName(context, StatsViewsWidget::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(viewsWidget)
        for (appWidgetId in allWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
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
