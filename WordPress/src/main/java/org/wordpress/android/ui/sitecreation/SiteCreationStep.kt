package org.wordpress.android.ui.sitecreation

import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.INTENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SEGMENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW
import org.wordpress.android.util.config.SiteIntentQuestionFeatureConfig
import org.wordpress.android.util.wizard.WizardStep
import javax.inject.Inject
import javax.inject.Singleton

enum class SiteCreationStep : WizardStep {
    SEGMENTS, DOMAINS, SITE_PREVIEW, INTENTS;
}

@Singleton
class SiteCreationStepsProvider @Inject constructor(
    private val siteIntentQuestionFeatureConfig: SiteIntentQuestionFeatureConfig
) {
    fun getSteps(): List<SiteCreationStep> {
        if (siteIntentQuestionFeatureConfig.isEnabled()) {
            return listOf(
                    INTENTS,
                    SEGMENTS,
                    DOMAINS,
                    SITE_PREVIEW
            )
        }

        return listOf(
                SEGMENTS,
                DOMAINS,
                SITE_PREVIEW
        )
    }
}
