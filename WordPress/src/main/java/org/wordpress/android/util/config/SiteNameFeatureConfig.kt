package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.util.experiments.SiteNameABExperiment
import javax.inject.Inject

/**
 * Configuration of the Site Name step in the Site Creation flow
 */
@FeatureInDevelopment
class SiteNameFeatureConfig
@Inject constructor(appConfig: AppConfig, private val siteNameABExperiment: SiteNameABExperiment) : FeatureConfig(
        appConfig,
        BuildConfig.SITE_NAME
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && siteNameABExperiment.getVariation() != Control
    }
}
