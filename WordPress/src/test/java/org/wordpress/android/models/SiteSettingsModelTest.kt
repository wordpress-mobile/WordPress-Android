package org.wordpress.android.models

import nl.jqno.equalsverifier.EqualsVerifier
import nl.jqno.equalsverifier.Warning
import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.Fixtures.createFakeSiteSettingsModel

class SiteSettingsModelTest {
    @Test
    fun equalsContract() {
        EqualsVerifier
                .forClass(SiteSettingsModel::class.java)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify()
    }

    @Test
    fun copyFrom() {
        val original = createFakeSiteSettingsModel()
        val copy = SiteSettingsModel()
        copy.copyFrom(original)
        assertEquals(original, copy)
    }
}
