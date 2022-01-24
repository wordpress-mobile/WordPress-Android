package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig.Companion.MY_SITE_DASHBOARD_PHASE_2
import javax.inject.Inject

/**
 * Configuration of the 'My Site Dashboard - Phase 2', which amongst others includes the new 'Dashboard Post Cards'
 * that will be added on the 'My Site' screen.
 */
@Feature(
        remoteField = MY_SITE_DASHBOARD_PHASE_2,
        defaultValue = true
)
class MySiteDashboardPhase2FeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_DASHBOARD_PHASE_2,
        MY_SITE_DASHBOARD_PHASE_2
) {
    companion object {
        const val MY_SITE_DASHBOARD_PHASE_2 = "my_site_dashboard_phase_2"
    }
}
