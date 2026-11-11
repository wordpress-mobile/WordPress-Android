package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.PackageManagerWrapper
import javax.inject.Inject

class JetpackFeatureRemovalWidgetHelper @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val packageManagerWrapper: PackageManagerWrapper
) {
    private val widgetReceivers = listOf(
            STATS_VIEW_WIDGET_ALIAS, STATS_ALL_TIME_WIDGET_ALIAS,
            STATS_TODAY_WIDGET_ALIAS, STATS_MINIFIED_WIDGET_ALIAS
    )

    fun disableWidgetReceiversIfNeeded() {
        if (buildConfigWrapper.isJetpackApp) {
            widgetReceivers.forEach { packageManagerWrapper.enableComponentEnabledSetting(it) }
        } else {
            widgetReceivers.forEach { packageManagerWrapper.disableComponentEnabledSetting(it) }
        }
    }

    companion object {
        const val STATS_VIEW_WIDGET_ALIAS =
                "org.wordpress.android.ui.stats.refresh.lists.widget.views.StatsViewsWidget"
        const val STATS_ALL_TIME_WIDGET_ALIAS =
                "org.wordpress.android.ui.stats.refresh.lists.widget.alltime.StatsAllTimeWidget"
        const val STATS_TODAY_WIDGET_ALIAS =
                "org.wordpress.android.ui.stats.refresh.lists.widget.today.StatsTodayWidget"
        const val STATS_MINIFIED_WIDGET_ALIAS =
                "org.wordpress.android.ui.stats.refresh.lists.widget.minified.StatsMinifiedWidget"
    }
}
