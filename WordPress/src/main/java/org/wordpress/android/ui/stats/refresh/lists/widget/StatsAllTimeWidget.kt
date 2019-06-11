package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.WordPress
import javax.inject.Inject

class StatsAllTimeWidget : AppWidgetProvider() {
    @Inject lateinit var widgetUpdater: AllTimeWidgetUpdater

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        (context.applicationContext as WordPress).component().inject(this)
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (appWidgetId > -1) {
            widgetUpdater.updateAppWidget(
                    context,
                    appWidgetId = appWidgetId
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        (context.applicationContext as WordPress).component().inject(this)
        for (appWidgetId in appWidgetIds) {
            widgetUpdater.updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        (context.applicationContext as WordPress).component().inject(this)
        for (appWidgetId in appWidgetIds) {
            widgetUpdater.delete(appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if (context != null) {
            (context.applicationContext as WordPress).component().inject(this)
            widgetUpdater.updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
            )
        }
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }
}
