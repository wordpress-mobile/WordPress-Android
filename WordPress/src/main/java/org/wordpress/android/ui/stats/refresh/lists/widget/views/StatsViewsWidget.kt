package org.wordpress.android.ui.stats.refresh.lists.widget.views

import org.wordpress.android.modules.AppComponent
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidget
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import javax.inject.Inject

class StatsViewsWidget : StatsWidget() {
    @Inject
    lateinit var viewsWidgetUpdater: ViewsWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = viewsWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
