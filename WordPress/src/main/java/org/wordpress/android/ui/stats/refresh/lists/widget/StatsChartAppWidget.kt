package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.LinearLayout
import android.widget.RemoteViews
import com.github.mikephil.charting.charts.BarChart
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BarChartViewHolder

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [StatsChartAppWidgetConfigureActivity]
 */
class StatsChartAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            StatsChartAppWidgetConfigureActivity.deleteTitlePref(
                    context,
                    appWidgetId
            )
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

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
//            drawList(appWidgetId, context, appWidgetManager)
            drawChart(appWidgetId, context, appWidgetManager)
        }

        private fun drawChart(
            appWidgetId: Int,
            context: Context,
            appWidgetManager: AppWidgetManager
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val widthDp = when {
                minWidth > 0 -> minWidth
                maxWidth > 0 -> maxWidth
                else -> 400
            }
            val heightDp = when {
                minHeight > 0 -> minHeight
                maxHeight > 0 -> maxHeight
                else -> 400
            }
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.stats_chart_app_widget)
            try {
                val view = BarChart(context)
                val width = makeMeasureSpec(widthDp, EXACTLY)
                val height = makeMeasureSpec(heightDp, EXACTLY)
                view.layoutParams = LinearLayout.LayoutParams(widthDp, heightDp)
                view.measure(width, height)
                val bars = BarChartViewHolder.Companion.drawChart(
                        view,
                        BarChartItem(listOf(
                                Bar("2019-01-01", "2019-02-02", 20),
                                Bar("2019-01-01", "2019-02-02", 20),
                                Bar("2019-01-01", "2019-02-02", 20),
                                Bar("2019-01-01", "2019-02-02", 20),
                                Bar("2019-01-01", "2019-02-02", 20),
                                Bar("2019-01-01", "2019-02-02", 20)
                        ), null, null, null, null)
                )
                val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                view.draw(canvas)
//                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

//                val chartBitmap = view.chartBitmap
                views.setImageViewBitmap(R.id.chart, bitmap)
            } catch (e: Exception) {
                Log.d(
                        "vojta",
                        "Exception: ${e.message}"
                )
            }
//            views.
            views.setTextViewText(id.label_start, "Label start")
            views.setTextViewText(id.label_end, "Label end")
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun drawList(
            appWidgetId: Int,
            context: Context,
            appWidgetManager: AppWidgetManager
        ) {
            Log.d(
                    "vojta",
                    "org/wordpress/android/ui/stats/refresh/lists/widget/StatsChartAppWidget.drawList($appWidgetId)"
            )
            val widgetText = StatsChartAppWidgetConfigureActivity.loadTitlePref(
                    context,
                    appWidgetId
            )
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.stats_chart_app_widget)
            views.setTextViewText(id.appwidget_text, widgetText)
            // RemoteViews Service needed to provide adapter for ListView
            val svcIntent = Intent(context, WidgetService::class.java)
            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            svcIntent.data = Uri.parse(
                    svcIntent.toUri(Intent.URI_INTENT_SCHEME)
            )
            // setting adapter to listview of the widget
            views.setRemoteAdapter(id.list, svcIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

