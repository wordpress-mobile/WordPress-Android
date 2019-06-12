package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.stats.OldStatsActivity
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color
import javax.inject.Inject

class AllTimeWidgetListProvider(val context: Context, intent: Intent) : RemoteViewsFactory {
    @Inject lateinit var viewModel: AllTimeWidgetListViewModel
    @Inject lateinit var widgetUpdated: AllTimeWidgetUpdater
    private val showChangeColumn: Boolean = intent.getBooleanExtra(SHOW_CHANGE_VALUE_KEY, true)
    private val colorModeId: Int = intent.getIntExtra(COLOR_MODE_KEY, Color.LIGHT.ordinal)
    private val siteId: Long = intent.getLongExtra(SITE_ID_KEY, 0L)
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreate() {
        viewModel.start(siteId, colorModeId, showChangeColumn, appWidgetId)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun onDataSetChanged() {
        viewModel.onDataSetChanged { appWidgetId ->
            widgetUpdated.updateAppWidget(
                    context,
                    appWidgetId = appWidgetId
            )
        }
    }

    override fun hasStableIds(): Boolean = true

    override fun getViewTypeCount(): Int = 1

    override fun onDestroy() {
    }

    override fun getCount(): Int {
        return viewModel.data.size
    }

    override fun getItemId(position: Int): Long {
        return viewModel.data[position].key.hashCode().toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val uiModel = viewModel.data[position]
        val rv = RemoteViews(context.packageName, uiModel.layout)
        rv.setTextViewText(R.id.period, uiModel.key)
        rv.setViewVisibility(R.id.divider, View.VISIBLE)
        rv.setTextViewText(R.id.value, uiModel.value)
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(WordPress.LOCAL_SITE_ID, uiModel.localSiteId)
        intent.putExtra(OldStatsActivity.ARG_DESIRED_TIMEFRAME, StatsTimeframe.DAY)
        intent.putExtra(OldStatsActivity.ARG_LAUNCHED_FROM, OldStatsActivity.StatsLaunchedFrom.STATS_WIDGET)
        rv.setOnClickFillInIntent(R.id.container, intent)
        return rv
    }
}
