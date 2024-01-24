package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature

private const val SITE_MONITORING_FEATURE_REMOTE_FIELD = "site_monitoring"

@Feature(SITE_MONITORING_FEATURE_REMOTE_FIELD, false)
class SiteMonitoringFeatureConfig(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_SITE_MONITORING,
    SITE_MONITORING_FEATURE_REMOTE_FIELD
)
