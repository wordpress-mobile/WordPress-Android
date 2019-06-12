package org.wordpress.android.ui.stats.refresh.lists.widget

import android.content.Intent
import android.widget.RemoteViewsService
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureFragment.ViewType
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureFragment.ViewType.ALL_TIME_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureFragment.ViewType.WEEK_VIEWS

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val viewTypeOrdinal = intent.getIntExtra(VIEW_TYPE_KEY, -1)
        return if (viewTypeOrdinal > -1 && viewTypeOrdinal < ViewType.values().size) {
            when (ViewType.values()[viewTypeOrdinal]) {
                WEEK_VIEWS -> ViewsWidgetListProvider(this.applicationContext, intent)
                ALL_TIME_VIEWS -> AllTimeWidgetListProvider(this.applicationContext, intent)
            }
        } else {
            ViewsWidgetListProvider(this.applicationContext, intent)
        }
    }
}
