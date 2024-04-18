package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val STATS_TRAFFIC_SUBSCRIBERS_TAB_REMOTE_FIELD = "stats_traffic_subscribers_tab"

@Feature(STATS_TRAFFIC_SUBSCRIBERS_TAB_REMOTE_FIELD, false)
class StatsTrafficSubscribersTabFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.STATS_TRAFFIC_SUBSCRIBERS_TAB,
    STATS_TRAFFIC_SUBSCRIBERS_TAB_REMOTE_FIELD
)
