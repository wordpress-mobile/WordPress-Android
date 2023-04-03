package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val DOMAINS_DASHBOARD_CARD_FEATURE_REMOTE_FIELD = "domains_dashboard_card"

@Feature(DOMAINS_DASHBOARD_CARD_FEATURE_REMOTE_FIELD, false)
class DomainsDashboardCardFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_DOMAINS_DASHBOARD_CARD_FEATURE,
    DOMAINS_DASHBOARD_CARD_FEATURE_REMOTE_FIELD
)
