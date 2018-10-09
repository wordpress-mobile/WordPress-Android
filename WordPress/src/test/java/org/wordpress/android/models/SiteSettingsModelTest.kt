package org.wordpress.android.models

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.Fixtures.createFakeSiteSettingsModel

class SiteSettingsModelTest {
    @Test
    fun copyFrom() {
        val original = createFakeSiteSettingsModel()
        val copy = SiteSettingsModel()
        copy.copyFrom(original)
        assertEquals(original, copy)
    }
}
