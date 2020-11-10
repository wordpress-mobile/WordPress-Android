package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Activity Log Filters feature.
 */
@FeatureInDevelopment
class ActivityLogFiltersFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.ACTIVITY_LOG_FILTERS
)
