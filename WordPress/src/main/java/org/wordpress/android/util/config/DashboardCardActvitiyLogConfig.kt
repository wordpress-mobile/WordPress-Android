package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

const val DASHBOARD_CARD_ACTIVITY_LOG_REMOTE_FIELD = "dashboard_card_activity_log"

@Feature(DASHBOARD_CARD_ACTIVITY_LOG_REMOTE_FIELD, false)
class DashboardCardActivityLogConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.DASHBOARD_CARD_ACTIVITY_LOG,
    DASHBOARD_CARD_ACTIVITY_LOG_REMOTE_FIELD
)
