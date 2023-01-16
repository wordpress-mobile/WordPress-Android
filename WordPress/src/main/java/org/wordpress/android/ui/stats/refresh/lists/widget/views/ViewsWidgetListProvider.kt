package org.wordpress.android.ui.stats.refresh.lists.widget.views

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.StatsActivity
import org.wordpress.android.ui.stats.refresh.StatsActivity.Companion.INITIAL_SELECTED_PERIOD_KEY
import org.wordpress.android.ui.stats.refresh.StatsActivity.StatsLaunchedFrom
import org.wordpress.android.ui.stats.refresh.lists.widget.IS_WIDE_VIEW_KEY
import org.wordpress.android.ui.stats.refresh.lists.widget.SITE_ID_KEY
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.getColorMode
import javax.inject.Inject

class ViewsWidgetListProvider(val context: Context, intent: Intent) : RemoteViewsFactory {
    @Inject
    lateinit var viewModel: ViewsWidgetListViewModel

    @Inject
    lateinit var viewsWidgetUpdater: ViewsWidgetUpdater
    private val isWideView: Boolean = intent.getBooleanExtra(IS_WIDE_VIEW_KEY, true)
    private val colorMode = intent.getColorMode()
    private val siteId: Int = intent.getIntExtra(SITE_ID_KEY, 0)
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreate() {
        viewModel.start(siteId, colorMode, isWideView, appWidgetId)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun onDataSetChanged() {
        viewModel.onDataSetChanged { appWidgetId ->
            viewsWidgetUpdater.updateAppWidget(
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
        if (uiModel.isNeutralChangeVisible) {
            rv.setTextViewText(R.id.neutral_change, uiModel.change)
            rv.setViewVisibility(R.id.neutral_change, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.neutral_change, View.GONE)
        }
        if (uiModel.isPositiveChangeVisible) {
            rv.setTextViewText(R.id.positive_change, uiModel.change)
            rv.setViewVisibility(R.id.positive_change, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.positive_change, View.GONE)
        }
        if (uiModel.isNegativeChangeVisible) {
            rv.setTextViewText(R.id.negative_change, uiModel.change)
            rv.setViewVisibility(R.id.negative_change, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.negative_change, View.GONE)
        }
        rv.setTextViewText(R.id.value, uiModel.value)
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(INITIAL_SELECTED_PERIOD_KEY, uiModel.period)
        intent.putExtra(WordPress.LOCAL_SITE_ID, uiModel.localSiteId)
        intent.putExtra(StatsActivity.ARG_DESIRED_TIMEFRAME, StatsTimeframe.DAY)
        intent.putExtra(StatsActivity.ARG_LAUNCHED_FROM, StatsLaunchedFrom.STATS_WIDGET)
        rv.setOnClickFillInIntent(R.id.container, intent)
        return rv
    }
}
