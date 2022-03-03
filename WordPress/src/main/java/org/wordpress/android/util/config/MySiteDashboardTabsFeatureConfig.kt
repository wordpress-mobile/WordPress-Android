package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the 'My Site Dashboard - Tabs' that will display tabs on the 'My Site' screen.
 */
@FeatureInDevelopment
class MySiteDashboardTabsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_DASHBOARD_TABS
)
