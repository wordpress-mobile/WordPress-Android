package org.wordpress.android.ui.stats.refresh.lists.widget.views

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.StatsTimeframe.DAY
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.WEEK_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.WidgetUtils
import org.wordpress.android.ui.stats.refresh.utils.trackWithWidgetType
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class ViewsWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val widgetUtils: WidgetUtils,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : WidgetUpdater {
    override fun updateAppWidget(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager?
    ) {
        val widgetManager = appWidgetManager ?: AppWidgetManager.getInstance(context)
        val isWideView = widgetUtils.isWidgetWiderThanLimit(widgetManager, appWidgetId)
        val colorMode = appPrefsWrapper.getAppWidgetColor(appWidgetId) ?: LIGHT
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        val siteModel = siteStore.getSiteBySiteId(siteId)
        val networkAvailable = networkUtilsWrapper.isNetworkAvailable()
        val layout = widgetUtils.getLayout(colorMode = colorMode)
        val views = RemoteViews(context.packageName, layout)
        views.setViewVisibility(R.id.widget_title_container, View.VISIBLE)
        views.setTextViewText(R.id.widget_title, resourceProvider.getString(R.string.stats_views))
        widgetUtils.setSiteIcon(siteModel, context, views, appWidgetId)
        val hasAccessToken = accountStore.hasAccessToken()
        val widgetHasData = appPrefsWrapper.hasAppWidgetData(appWidgetId)
        if (networkAvailable && hasAccessToken && siteModel != null) {
            siteModel.let {
                views.setOnClickPendingIntent(
                    R.id.widget_title_container,
                    widgetUtils.getPendingSelfIntent(context, siteModel.id, DAY)
                )
            }
            widgetUtils.showList(
                widgetManager,
                views,
                context,
                appWidgetId,
                colorMode,
                siteModel.id,
                WEEK_VIEWS,
                isWideView
            )
        } else if (!widgetHasData || !hasAccessToken || siteModel == null) {
            widgetUtils.showError(
                widgetManager,
                views,
                appWidgetId,
                networkAvailable,
                hasAccessToken,
                resourceProvider,
                context,
                StatsViewsWidget::class.java
            )
        }
    }

    override fun componentName(context: Context) = ComponentName(context, StatsViewsWidget::class.java)

    override fun delete(appWidgetId: Int) {
        analyticsTrackerWrapper.trackWithWidgetType(AnalyticsTracker.Stat.STATS_WIDGET_REMOVED, WEEK_VIEWS)
        appPrefsWrapper.removeAppWidgetColorModeId(appWidgetId)
        appPrefsWrapper.removeAppWidgetSiteId(appWidgetId)
    }
}
