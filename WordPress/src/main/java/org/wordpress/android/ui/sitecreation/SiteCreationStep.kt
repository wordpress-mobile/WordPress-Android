package org.wordpress.android.ui.sitecreation

import org.wordpress.android.util.wizard.WizardStep

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
                // TODO we should consider skipping the step when it's unknown
                else -> throw IllegalArgumentException("SiteCreationStep not recognized: \$input")
            }
        }
    }
}
