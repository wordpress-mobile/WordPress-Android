package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import org.wordpress.android.util.experiments.SiteIntentQuestionABExperiment
import org.wordpress.android.util.experiments.SiteIntentQuestionABExperiment.Variation.TREATMENT
import javax.inject.Inject

/**
 * Configuration of the Site Intent Question step in the Site Creation flow
 */
@FeatureInDevelopment
class SiteIntentQuestionFeatureConfig
@Inject constructor(appConfig: AppConfig, private val experiment: SiteIntentQuestionABExperiment) : FeatureConfig(
        appConfig,
        BuildConfig.SITE_INTENT_QUESTION
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && experiment.variant == TREATMENT
    }
}
