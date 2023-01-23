package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.WordPress
import org.wordpress.android.modules.AppComponent

const val IS_WIDE_VIEW_KEY = "show_change_value_key"
const val SITE_ID_KEY = "site_id_key"

abstract class StatsWidget : AppWidgetProvider() {
    abstract val widgetUpdater: WidgetUpdater

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        inject((context.applicationContext as WordPress).component())
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (appWidgetId > -1) {
            widgetUpdater.updateAppWidget(
                context,
                appWidgetId = appWidgetId
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        inject((context.applicationContext as WordPress).component())
        for (appWidgetId in appWidgetIds) {
            widgetUpdater.updateAppWidget(
                context,
                appWidgetId,
                appWidgetManager
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        inject((context.applicationContext as WordPress).component())
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
            inject((context.applicationContext as WordPress).component())
            widgetUpdater.updateAppWidget(
                context,
                appWidgetId,
                appWidgetManager
            )
        }
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    abstract fun inject(appComponent: AppComponent)
}
