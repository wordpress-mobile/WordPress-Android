package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.OnboardingImprovementsFeatureConfig.Companion.ONBOARDING_IMPROVEMENTS
import javax.inject.Inject

/**
 * Configuration of the 'Onboarding Improvement: Existing Users' feature.
 */
@Feature(remoteField = ONBOARDING_IMPROVEMENTS, defaultValue = true)
class OnboardingImprovementsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(appConfig, BuildConfig.ONBOARDING_IMPROVEMENTS, ONBOARDING_IMPROVEMENTS) {
    companion object {
        const val ONBOARDING_IMPROVEMENTS = "onboarding_improvements"
    }
}
