package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val DYNAMIC_DASHBOARD_CARDS_FEATURE_REMOTE_FIELD = "dynamic_dashboard_cards"

@Feature(DYNAMIC_DASHBOARD_CARDS_FEATURE_REMOTE_FIELD, false)
class DynamicDashboardCardsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.DYNAMIC_DASHBOARD_CARDS,
    DYNAMIC_DASHBOARD_CARDS_FEATURE_REMOTE_FIELD,
)
