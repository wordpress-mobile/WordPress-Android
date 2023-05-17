package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val DASHBOARD_CARD_FREE_TO_PAID_PLANS_REMOTE_FIELD = "dashboard_card_free_to_paid_plans"

@Feature(DASHBOARD_CARD_FREE_TO_PAID_PLANS_REMOTE_FIELD, false)
class DashboardCardFreeToPaidPlansFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.DASHBOARD_CARD_FREE_TO_PAID_PLANS,
    DASHBOARD_CARD_FREE_TO_PAID_PLANS_REMOTE_FIELD
)
