package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import org.wordpress.android.R
import org.wordpress.android.R.id

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [StatsViewsWidgetConfigureActivity]
 */
class StatsViewsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            // TODO Delete shared prefs of the fragment
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (context != null && appWidgetManager != null) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            drawList(appWidgetId, context, appWidgetManager)
        }

        private fun drawList(
            appWidgetId: Int,
            context: Context,
            appWidgetManager: AppWidgetManager
        ) {
            val views = RemoteViews(context.packageName, R.layout.stats_chart_app_widget)
            val svcIntent = Intent(context, WidgetService::class.java)
            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            svcIntent.data = Uri.parse(
                    svcIntent.toUri(Intent.URI_INTENT_SCHEME)
            )
            views.setRemoteAdapter(id.list, svcIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

