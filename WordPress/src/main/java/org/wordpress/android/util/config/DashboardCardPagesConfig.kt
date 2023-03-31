package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

const val DASHBOARD_CARD_PAGES_REMOTE_FIELD = "dashboard_card_pages"

@Feature(DASHBOARD_CARD_PAGES_REMOTE_FIELD, false)
class DashboardCardPagesConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.DASHBOARD_CARD_PAGES,
    DASHBOARD_CARD_PAGES_REMOTE_FIELD
)
