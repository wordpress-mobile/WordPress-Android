package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration for the landing screen revamp work
 */
@FeatureInDevelopment
class LandingScreenRevampFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.LANDING_SCREEN_REVAMP) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() || BuildConfig.IS_JETPACK_APP
    }
}
