package org.wordpress.android.ui.sitecreation;

import org.wordpress.android.util.wizard.WizardStep;

public enum SiteCreationStep implements WizardStep {
    SEGMENTS, VERTICALS, DOMAINS;

    public static SiteCreationStep fromString(String input) {
        switch (input) {
            case "site_creation_segments":
                return SEGMENTS;
            case "site_creation_verticals":
                return VERTICALS;
            case "site_creation_domains":
                return DOMAINS;
            // TODO we should consider skipping the step when it's unknown
            default:
                throw new IllegalArgumentException("SiteCreationStep not recognized: $input");
        }
    }
}
