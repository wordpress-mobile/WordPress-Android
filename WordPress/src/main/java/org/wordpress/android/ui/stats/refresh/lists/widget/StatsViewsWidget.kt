package org.wordpress.android.ui.stats.refresh.lists.widget

import org.wordpress.android.modules.AppComponent
import javax.inject.Inject

class StatsViewsWidget : StatsWidget() {
    @Inject lateinit var viewsWidgetUpdater: ViewsWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = viewsWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
