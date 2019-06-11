package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.WordPress
import javax.inject.Inject

const val SHOW_CHANGE_VALUE_KEY = "show_change_value_key"
const val COLOR_MODE_KEY = "color_mode_key"
const val SITE_ID_KEY = "site_id_key"

class StatsViewsWidget : AppWidgetProvider() {
    @Inject lateinit var viewsWidgetUpdater: ViewsWidgetUpdater

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        (context.applicationContext as WordPress).component().inject(this)
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (appWidgetId > -1) {
            viewsWidgetUpdater.updateAppWidget(
                    context,
                    appWidgetId = appWidgetId
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        (context.applicationContext as WordPress).component().inject(this)
        for (appWidgetId in appWidgetIds) {
            viewsWidgetUpdater.updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        (context.applicationContext as WordPress).component().inject(this)
        for (appWidgetId in appWidgetIds) {
            viewsWidgetUpdater.delete(appWidgetId)
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
            viewsWidgetUpdater.updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
            )
        }
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }
}
