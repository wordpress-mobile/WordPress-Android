package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import org.wordpress.android.R.layout
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel

class ViewsListProvider(val context: Context, intent: Intent) : RemoteViewsFactory {
    override fun onCreate() {
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun onDataSetChanged() {
    }

    override fun hasStableIds(): Boolean = true

    override fun getViewTypeCount(): Int = 1

    override fun onDestroy() {
    }

    private val listItemList = mutableListOf<VisitsAndViewsModel.PeriodData>()
    private var appWidgetId: Int = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun getCount(): Int = listItemList.size

    override fun getItemId(position: Int): Long {
        return listItemList[position].period.hashCode().toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        return RemoteViews(context.packageName, layout.stats_block_referred_item)
    }
}
