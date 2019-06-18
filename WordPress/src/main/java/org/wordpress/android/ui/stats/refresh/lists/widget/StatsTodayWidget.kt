package org.wordpress.android.ui.stats.refresh.lists.widget

import org.wordpress.android.modules.AppComponent
import javax.inject.Inject

class StatsTodayWidget : StatsWidget() {
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
