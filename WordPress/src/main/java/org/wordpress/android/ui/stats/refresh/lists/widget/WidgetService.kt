package org.wordpress.android.ui.stats.refresh.lists.widget

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ViewsListProvider(this.applicationContext, intent)
    }
}
