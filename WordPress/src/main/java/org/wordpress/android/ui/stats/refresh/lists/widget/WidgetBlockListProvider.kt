package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.annotation.LayoutRes
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.StatsTimeframe.INSIGHTS
import org.wordpress.android.ui.stats.refresh.StatsActivity
import org.wordpress.android.ui.stats.refresh.StatsActivity.StatsLaunchedFrom
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.getColorMode

class WidgetBlockListProvider(val context: Context, val viewModel: WidgetBlockListViewModel, intent: Intent) :
    RemoteViewsFactory {
    private val colorMode: Color = intent.getColorMode()
    private val siteId: Int = intent.getIntExtra(SITE_ID_KEY, -1)
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

    override fun onCreate() {
        viewModel.start(siteId, colorMode, appWidgetId)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun onDataSetChanged() {
        viewModel.onDataSetChanged(context)
    }

    override fun hasStableIds(): Boolean = true

    override fun getViewTypeCount(): Int = 1

    override fun onDestroy() {
    }

    override fun getCount(): Int {
        return viewModel.data.size
    }

    override fun getItemId(position: Int): Long {
        return viewModel.data[position].startKey.hashCode().toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val uiModel = viewModel.data[position]
        val rv = RemoteViews(context.packageName, uiModel.layout)
        rv.setTextViewText(R.id.start_block_title, uiModel.startKey)
        rv.setTextViewText(R.id.start_block_value, uiModel.startValue)
        rv.setTextViewText(R.id.end_block_title, uiModel.endKey)
        rv.setTextViewText(R.id.end_block_value, uiModel.endValue)
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(WordPress.LOCAL_SITE_ID, uiModel.localSiteId)
        intent.putExtra(StatsActivity.ARG_DESIRED_TIMEFRAME, uiModel.targetTimeframe)
        intent.putExtra(StatsActivity.ARG_LAUNCHED_FROM, StatsLaunchedFrom.STATS_WIDGET)
        rv.setOnClickFillInIntent(R.id.container, intent)
        return rv
    }

    data class BlockItemUiModel(
        @LayoutRes val layout: Int,
        val localSiteId: Int,
        val startKey: String,
        val startValue: String,
        val endKey: String,
        val endValue: String,
        val targetTimeframe: StatsTimeframe = INSIGHTS
    )

    interface WidgetBlockListViewModel {
        val data: List<BlockItemUiModel>
        fun start(siteId: Int, colorMode: Color, appWidgetId: Int)
        fun onDataSetChanged(context: Context)
    }
}
