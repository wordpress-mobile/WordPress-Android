package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.Context

interface WidgetUpdater {
    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
        appWidgetId: Int
    )

    fun updateAllWidgets(context: Context)
    fun delete(appWidgetId: Int)
}
