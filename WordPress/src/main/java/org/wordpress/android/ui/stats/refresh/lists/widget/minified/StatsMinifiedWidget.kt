package org.wordpress.android.ui.stats.refresh.lists.widget.minified

import org.wordpress.android.modules.AppComponent
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidget
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetUpdater
import javax.inject.Inject

class StatsMinifiedWidget : StatsWidget() {
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
