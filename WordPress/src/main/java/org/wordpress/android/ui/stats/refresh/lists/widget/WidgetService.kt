package org.wordpress.android.ui.stats.refresh.lists.widget

import android.content.Intent
import android.widget.RemoteViewsService
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.ViewType.ALL_TIME_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.ViewType.TODAY_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.ViewType.WEEK_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetListProvider
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetListProvider
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.getViewType
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetListProvider

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return when (intent.getViewType()) {
            WEEK_VIEWS -> ViewsWidgetListProvider(this.applicationContext, intent)
            ALL_TIME_VIEWS -> AllTimeWidgetListProvider(this.applicationContext, intent)
            TODAY_VIEWS -> TodayWidgetListProvider(this.applicationContext, intent)
        }
    }
}
