package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the 'My Site Dashboard - Stats Card' that will add Stats Card on the 'My Site' screen.
 */
@FeatureInDevelopment
class MySiteDashboardTodaysStatsCardFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_DASHBOARD_TODAYS_STATS_CARD
)
