package org.wordpress.android.ui.sitecreation

import org.wordpress.android.util.config.SiteIntentQuestionFeatureConfig
import org.wordpress.android.util.wizard.WizardStep
import javax.inject.Inject
import javax.inject.Singleton

enum class SiteCreationStep : WizardStep {
    SEGMENTS, DOMAINS, SITE_PREVIEW, INTENTS;

    companion object {
        fun fromString(input: String): SiteCreationStep {
            return when (input) {
                "site_creation_intents" -> INTENTS
                "site_creation_segments" -> SEGMENTS
                "site_creation_domains" -> DOMAINS
                "site_creation_site_preview" -> SITE_PREVIEW
                else -> throw IllegalArgumentException("SiteCreationStep not recognized: \$input")
            }
        }
    }
}

@Singleton
class SiteCreationStepsProvider @Inject constructor(
    private val siteIntentQuestionFeatureConfig: SiteIntentQuestionFeatureConfig
) {
    fun getSteps(): List<SiteCreationStep> {
        if (siteIntentQuestionFeatureConfig.isEnabled()) {
            return listOf(
                    SiteCreationStep.fromString("site_creation_intents"),
                    SiteCreationStep.fromString("site_creation_segments"),
                    SiteCreationStep.fromString("site_creation_domains"),
                    SiteCreationStep.fromString("site_creation_site_preview")
            )
        }

        return listOf(
                SiteCreationStep.fromString("site_creation_segments"),
                SiteCreationStep.fromString("site_creation_domains"),
                SiteCreationStep.fromString("site_creation_site_preview")
        )
    }
}
