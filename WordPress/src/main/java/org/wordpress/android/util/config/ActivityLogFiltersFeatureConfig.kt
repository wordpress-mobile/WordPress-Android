package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.ActivityLogFiltersFeatureConfig.Companion.ACTIVITY_LOG_FILTERS
import javax.inject.Inject

/**
 * Configuration of the Activity Log Filters feature.
 */
@Feature(remoteField = ACTIVITY_LOG_FILTERS, defaultValue = true)
class ActivityLogFiltersFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.ACTIVITY_LOG_FILTERS,
        ACTIVITY_LOG_FILTERS
) {
    companion object {
        const val ACTIVITY_LOG_FILTERS = "activity_log_filters_enabled"
    }
}
