package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig.Companion.MY_SITE_DASHBOARD_TABS
import javax.inject.Inject

/**
 * Configuration of the 'My Site Dashboard - Tabs' that will display tabs on the 'My Site' screen.
 */
@Feature(
        remoteField = MY_SITE_DASHBOARD_TABS,
        defaultValue = true
)
class MySiteDashboardTabsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_DASHBOARD_TABS,
        MY_SITE_DASHBOARD_TABS
) {
    companion object {
        const val MY_SITE_DASHBOARD_TABS = "my_site_dashboard_tabs"
    }
}
