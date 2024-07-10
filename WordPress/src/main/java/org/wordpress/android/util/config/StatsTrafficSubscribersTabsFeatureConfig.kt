package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val STATS_TRAFFIC_SUBSCRIBERS_TABS_REMOTE_FIELD = "stats_traffic_subscribers_tabs"

@Feature(STATS_TRAFFIC_SUBSCRIBERS_TABS_REMOTE_FIELD, false)
class StatsTrafficSubscribersTabsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.STATS_TRAFFIC_SUBSCRIBERS_TABS,
    STATS_TRAFFIC_SUBSCRIBERS_TABS_REMOTE_FIELD
)
