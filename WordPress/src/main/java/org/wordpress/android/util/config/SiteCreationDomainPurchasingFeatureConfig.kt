package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.util.config.AppConfig.FeatureState.ManuallyOverriden
import org.wordpress.android.util.experiments.SiteCreationDomainPurchasingExperiment
import javax.inject.Inject

@FeatureInDevelopment
class SiteCreationDomainPurchasingFeatureConfig
@Inject constructor(
    appConfig: AppConfig,
    private val experiment: SiteCreationDomainPurchasingExperiment,
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_SITE_CREATION_DOMAIN_PURCHASING,
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && experiment.getVariation() != Control
    }

    fun isEnabledOrManuallyOverridden() = (featureState() as? ManuallyOverriden)?.isEnabled ?: isEnabled()

    fun isEnabledState(): Boolean {
        return featureState().isEnabled
    }
}
