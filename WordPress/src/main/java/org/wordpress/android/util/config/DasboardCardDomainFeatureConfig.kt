package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val DASHBOARD_CARD_DOMAIN_REMOTE_FIELD = "dashboard_card_domain"

@Feature(DASHBOARD_CARD_DOMAIN_REMOTE_FIELD, false)
class DashboardCardDomainFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.DASHBOARD_CARD_DOMAIN,
    DASHBOARD_CARD_DOMAIN_REMOTE_FIELD
)
