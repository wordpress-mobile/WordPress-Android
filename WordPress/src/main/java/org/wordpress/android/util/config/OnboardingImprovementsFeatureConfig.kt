package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the 'Onboarding Improvement: Existing Users' feature.
 */
@FeatureInDevelopment
class OnboardingImprovementsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(appConfig, BuildConfig.GLOBAL_STYLE_SUPPORT)
