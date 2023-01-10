package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.minified.MinifiedWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.weeks.WeekViewsWidgetUpdater
import javax.inject.Inject

interface WidgetUpdater {
    fun updateAppWidget(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager? = null
    )

    fun componentName(context: Context): ComponentName

    fun delete(appWidgetId: Int)

    class StatsWidgetUpdaters
    @Inject constructor(
        private val viewsWidgetUpdater: ViewsWidgetUpdater,
        private val allTimeWidgetUpdater: AllTimeWidgetUpdater,
        private val todayWidgetUpdater: TodayWidgetUpdater,
        private val minifiedWidgetUpdater: MinifiedWidgetUpdater,
        private val weekViewsWidgetUpdater: WeekViewsWidgetUpdater,
        private val appPrefsWrapper: AppPrefsWrapper,
        private val context: Context
    ) {
        private val widgetUpdaters = listOf(
            viewsWidgetUpdater,
            allTimeWidgetUpdater,
            todayWidgetUpdater,
            minifiedWidgetUpdater,
            weekViewsWidgetUpdater
        )

        fun update(context: Context) {
            widgetUpdaters.forEach {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(it.componentName(context))
                for (appWidgetId in allWidgetIds) {
                    it.updateAppWidget(context, appWidgetId, appWidgetManager)
                }
            }
        }

        fun updateViewsWidget(siteId: Long) {
            viewsWidgetUpdater.update(siteId)
        }

        fun updateTodayWidget(siteId: Long) {
            todayWidgetUpdater.update(siteId)
            minifiedWidgetUpdater.update(siteId)
        }

        fun updateAllTimeWidget(siteId: Long) {
            allTimeWidgetUpdater.update(siteId)
        }

        fun updateWeekViewsWidget(siteId: Long) {
            weekViewsWidgetUpdater.update(siteId)
        }

        private fun WidgetUpdater.update(
            siteId: Long
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(this.componentName(context))
            for (appWidgetId in allWidgetIds) {
                val widgetSiteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
                if (siteId == widgetSiteId) {
                    this.updateAppWidget(context, appWidgetId, appWidgetManager)
                }
            }
        }
    }
}
