package org.wordpress.android.ui.stats.refresh.lists.widget.today

import org.wordpress.android.modules.AppComponent
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidget
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import javax.inject.Inject

class StatsTodayWidget : StatsWidget() {
    @Inject
    lateinit var todayWidgetUpdater: TodayWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
