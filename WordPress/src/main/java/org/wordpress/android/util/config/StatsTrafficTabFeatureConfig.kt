package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val STATS_TRAFFIC_TAB_REMOTE_FIELD = "stats_traffic_tab"

@Feature(STATS_TRAFFIC_TAB_REMOTE_FIELD, false)
class StatsTrafficTabFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.STATS_TRAFFIC_TAB,
    STATS_TRAFFIC_TAB_REMOTE_FIELD
)
