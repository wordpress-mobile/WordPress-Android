package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.MySiteDashboardTodaysStatsCardFeatureConfig.Companion.MY_SITE_DASHBOARD_TODAYS_STATS_CARD
import javax.inject.Inject

/**
 * Configuration of the 'My Site Dashboard - Today's Stats Card' that will add the card on the 'My Site' screen.
 */
@Feature(
    remoteField = MY_SITE_DASHBOARD_TODAYS_STATS_CARD,
    defaultValue = true
)
class MySiteDashboardTodaysStatsCardFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.MY_SITE_DASHBOARD_TODAYS_STATS_CARD,
    MY_SITE_DASHBOARD_TODAYS_STATS_CARD
) {
    companion object {
        const val MY_SITE_DASHBOARD_TODAYS_STATS_CARD = "my_site_dashboard_todays_stats_card"
    }
}
