package org.wordpress.android.ui.sitecreation

import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.INTENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_DESIGNS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_NAME
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW
import org.wordpress.android.util.config.SiteIntentQuestionFeatureConfig
import org.wordpress.android.util.config.SiteNameFeatureConfig
import org.wordpress.android.util.experiments.SiteNameABExperiment
import org.wordpress.android.util.wizard.WizardStep
import javax.inject.Inject
import javax.inject.Singleton

enum class SiteCreationStep : WizardStep {
    SITE_DESIGNS, DOMAINS, SITE_PREVIEW, INTENTS, SITE_NAME;
}

@Singleton
class SiteCreationStepsProvider @Inject constructor(
    private val siteIntentQuestionFeatureConfig: SiteIntentQuestionFeatureConfig,
    private val siteNameFeatureConfig: SiteNameFeatureConfig,
    private val siteNameABExperiment: SiteNameABExperiment
) {
    fun getSteps(): List<SiteCreationStep> {
        val isSiteNameEnabled = siteNameFeatureConfig.isEnabled() && siteNameABExperiment.getVariation() != Control
        val isIntentsEnabled = siteIntentQuestionFeatureConfig.isEnabled()

        return when {
            isSiteNameEnabled -> listOf(INTENTS, SITE_NAME, SITE_DESIGNS, SITE_PREVIEW)
            isIntentsEnabled -> listOf(INTENTS, SITE_DESIGNS, DOMAINS, SITE_PREVIEW)
            else -> listOf(SITE_DESIGNS, DOMAINS, SITE_PREVIEW)
        }
    }
}
