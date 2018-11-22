package org.wordpress.android.util.wizard;

public enum SiteCreationStep implements WizardStep {
    SEGMENTS, VERTICALS;

    public static SiteCreationStep fromString(String input) {
        switch (input) {
            case "site_creation_segments":
                return SEGMENTS;
            case "site_creation_verticals":
                return VERTICALS;
            // TODO we should consider skipping the step when it's unknown
            default:
                throw new IllegalArgumentException("SiteCreationStep not recognized: $input");
        }
    }
}
