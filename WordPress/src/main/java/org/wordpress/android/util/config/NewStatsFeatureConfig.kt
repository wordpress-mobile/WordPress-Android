package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.NewStatsFeatureConfig.Companion.NEW_STATS_REMOTE_FIELD
import javax.inject.Inject

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
) {
    companion object {
        const val NEW_STATS_REMOTE_FIELD = "android_new_stats"
    }
}
