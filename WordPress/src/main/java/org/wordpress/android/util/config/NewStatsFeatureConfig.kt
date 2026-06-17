package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val NEW_STATS_REMOTE_FIELD = "android_new_stats"

/**
 * Configuration for the new Stats experience.
 */
@Feature(NEW_STATS_REMOTE_FIELD, false)
class NewStatsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.NEW_STATS,
    NEW_STATS_REMOTE_FIELD,
)
