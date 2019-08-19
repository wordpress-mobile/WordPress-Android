package org.wordpress.android.ui.stats.refresh.lists.widget

import android.content.Intent
import android.widget.RemoteViewsService
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetBlockListProviderFactory
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetListProvider
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.ALL_TIME_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.TODAY_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.WEEK_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetBlockListProviderFactory
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetListProvider
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.getViewType
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetListProvider

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val wideView = intent.getBooleanExtra(IS_WIDE_VIEW_KEY, true)
        return when (intent.getViewType()) {
            WEEK_VIEWS -> ViewsWidgetListProvider(this.applicationContext, intent)
            ALL_TIME_VIEWS -> {
                if (wideView) {
                    AllTimeWidgetBlockListProviderFactory(this.applicationContext, intent).build()
                } else {
                    AllTimeWidgetListProvider(this.applicationContext, intent)
                }
            }
            TODAY_VIEWS -> {
                if (wideView) {
                    TodayWidgetBlockListProviderFactory(this.applicationContext, intent).build()
                } else {
                    TodayWidgetListProvider(this.applicationContext, intent)
                }
            }
        }
    }
}
