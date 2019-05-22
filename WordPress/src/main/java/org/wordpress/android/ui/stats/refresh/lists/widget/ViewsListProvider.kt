package org.wordpress.android.ui.stats.refresh.lists.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import org.wordpress.android.R

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

    private val listItemList = mutableListOf<Item>()
    private var appWidgetId: Int = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
    )

    init {
        populateListItem();
    }

    private fun populateListItem() {
        for (i in 0..10L) {
            listItemList.add(Item(i, "Heading: $i"))
        }
    }

    override fun getCount(): Int = listItemList.size

    override fun getItemId(position: Int): Long {
        return listItemList[position].id
    }

    override fun getViewAt(position: Int): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.stats_block_referred_item)
        remoteView.setTextViewText(R.id.title, listItemList[position].heading)
        return remoteView
    }
    data class Item(val id: Long, val heading: String)
}
