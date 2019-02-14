package org.wordpress.android.ui.sitecreation

import org.wordpress.android.util.wizard.WizardStep
import javax.inject.Inject
import javax.inject.Singleton

enum class SiteCreationStep : WizardStep {
    SEGMENTS, VERTICALS, SITE_INFO, DOMAINS, SITE_PREVIEW;

    companion object {
        fun fromString(input: String): SiteCreationStep {
            return when (input) {
                "site_creation_segments" -> SEGMENTS
                "site_creation_verticals" -> VERTICALS
                "site_creation_site_info" -> SITE_INFO
                "site_creation_domains" -> DOMAINS
                "site_creation_site_preview" -> SITE_PREVIEW
                else -> throw IllegalArgumentException("SiteCreationStep not recognized: \$input")
            }
        }
    }
}

@Singleton
class NewSiteCreationStepsProvider @Inject constructor() {
    fun getSteps(): List<SiteCreationStep> {
        return listOf(
                SiteCreationStep.fromString("site_creation_segments"),
                SiteCreationStep.fromString("site_creation_verticals"),
                SiteCreationStep.fromString("site_creation_site_info"),
                SiteCreationStep.fromString("site_creation_domains"),
                SiteCreationStep.fromString("site_creation_site_preview")
        )
    }
}
