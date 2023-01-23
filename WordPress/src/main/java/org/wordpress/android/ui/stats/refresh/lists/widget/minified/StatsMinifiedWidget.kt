package org.wordpress.android.ui.stats.refresh.lists.widget.minified

import org.wordpress.android.modules.AppComponent
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidget
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetUpdater
import javax.inject.Inject

class StatsMinifiedWidget : StatsWidget() {
    @Inject
    lateinit var minifiedWidgetUpdater: MinifiedWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = minifiedWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
