package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val CLARITY_ANALYTICS_TRACKING_REMOTE_FIELD = "clarity_analytics_tracking"

@Feature(CLARITY_ANALYTICS_TRACKING_REMOTE_FIELD, false)
class ClarityAnalyticsTrackingConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.CLARITY_ANALYTICS_TRACKING,
    CLARITY_ANALYTICS_TRACKING_REMOTE_FIELD,
)
