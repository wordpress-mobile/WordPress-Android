package org.wordpress.android.models;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class SiteSettingsModelTest {
    @Test
    public void equalsContract() {
        EqualsVerifier
                .forClass(SiteSettingsModel.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}
