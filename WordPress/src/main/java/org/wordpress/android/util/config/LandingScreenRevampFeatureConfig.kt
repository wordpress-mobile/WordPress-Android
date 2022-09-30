package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

/**
 * Configuration for the landing screen revamp work
 */
@Feature(LandingScreenRevampFeatureConfig.LANDING_SCREEN_REVAMP_REMOTE_FIELD, false)
class LandingScreenRevampFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.LANDING_SCREEN_REVAMP,
        LANDING_SCREEN_REVAMP_REMOTE_FIELD,
) {
    companion object {
        const val LANDING_SCREEN_REVAMP_REMOTE_FIELD = "landing_screen_revamp_remote_field"
    }
}
