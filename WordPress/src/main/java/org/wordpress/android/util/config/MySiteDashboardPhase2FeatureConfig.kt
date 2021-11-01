package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the 'My Site Dashboard - Phase 2', which amongst others includes the new 'Dashboard Post Cards'
 * that will be added on the 'My Site' screen.
 */
@FeatureInDevelopment
class MySiteDashboardPhase2FeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_DASHBOARD_PHASE_2
)
