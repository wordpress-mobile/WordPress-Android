package org.wordpress.android.ui.stats.refresh.lists.widget.weeks

import org.wordpress.android.modules.AppComponent
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidget
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import javax.inject.Inject

class StatsWeekWidget : StatsWidget() {
    @Inject
    lateinit var weekViewsWidgetUpdater: WeekViewsWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = weekViewsWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
