package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.minified.MinifiedWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetUpdater
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
        viewsWidgetUpdater: ViewsWidgetUpdater,
        allTimeWidgetUpdater: AllTimeWidgetUpdater,
        todayWidgetUpdater: TodayWidgetUpdater,
        minifiedWidgetUpdater: MinifiedWidgetUpdater
    ) {
        private val widgetUpdaters = listOf(
                viewsWidgetUpdater,
                allTimeWidgetUpdater,
                todayWidgetUpdater,
                minifiedWidgetUpdater
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
    }
}
