package org.wordpress.android.util.extensions

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SiteModelExtensionsKtTest {
    // region isJetpackConnectedWithoutFullPlugin
    @Test
    fun `Should return FALSE if plugins string is null`() {
        assertFalse(siteModel(null).isJetpackConnectedWithoutFullPlugin())
    }

    @Test
    fun `Should return FALSE if plugins string is empty`() {
        assertFalse(siteModel("").isJetpackConnectedWithoutFullPlugin())
    }

    @Test
    fun `Should return FALSE if plugins string is not valid`() {
        assertFalse(siteModel("something").isJetpackConnectedWithoutFullPlugin())
    }

    @Test
    fun `Should return FALSE if plugins list is empty`() {
        assertFalse(siteModel("").isJetpackConnectedWithoutFullPlugin())
    }

    @Test
    fun `Should return FALSE if plugins list contains jetpack`() {
        assertFalse(siteModel("jetpack-1,jetpack").isJetpackConnectedWithoutFullPlugin())
    }

    @Test
    fun `Should return FALSE if plugins list does not contain at least one element that starts with jetpack-`() {
        assertFalse(siteModel("plugin1,plugin2").isJetpackConnectedWithoutFullPlugin())
    }

    @Test
    @Suppress("MaxLineLength")
    fun `Should return TRUE if plugins list is not empty, does not contain jetpack and contains at least one element that starts with jetpack-`() {
        assertTrue(siteModel("jetpack-1").isJetpackConnectedWithoutFullPlugin())
        assertTrue(siteModel("jetpack-1,something").isJetpackConnectedWithoutFullPlugin())
    }

    private fun siteModel(activeJpPlugins: String?) =
        SiteModel().apply {
            activeJetpackConnectionPlugins = activeJpPlugins
        }
    // endregion
}
