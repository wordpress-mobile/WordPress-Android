package org.wordpress.android.util.extensions

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSiteModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("MaxLineLength")
class IndividualJetpackPluginExtensionsKtTest {
    // region SiteModel extensions
    @Test
    fun `SiteModel - Should return FALSE if plugins string is null when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(siteModel(null).isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `SiteModel - Should return FALSE if plugins string is empty when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(siteModel("").isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `SiteModel - Should return FALSE if plugins string is not valid when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(siteModel("something").isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `SiteModel - Should return FALSE if plugins list is empty when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(siteModel("").isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `SiteModel - Should return FALSE if plugins list contains jetpack when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(siteModel("jetpack-1,jetpack").isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `SiteModel - Should return FALSE if plugins list does not contain at least one element that starts with jetpack- when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(siteModel("plugin1,plugin2").isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `SiteModel - Should return TRUE if plugins list is not empty, does not contain jetpack and contains at least one element that starts with jetpack- when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertTrue(siteModel("jetpack-1").isJetpackIndividualPluginConnectedWithoutFullPlugin())
        assertTrue(siteModel("jetpack-1,something").isJetpackIndividualPluginConnectedWithoutFullPlugin())
        assertTrue(siteModel("something,jetpack-1").isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `SiteModel - Should return list of plugin names if list contains known individual jetpack plugins, filtering out non-jetpack plugins and unknown jetpack plugins`() {
        val model = siteModel(
            "jetpack-search," +
                    "jetpack-backup," +
                    "jetpack-protect," +
                    "jetpack-videopress," +
                    "jetpack-social," +
                    "jetpack-boost," +
                    "jetpack-unknown," +
                    "not-jetpack-plugin"
        )
        val expected = listOf(
            "Jetpack Search",
            "Jetpack VaultPress Backup",
            "Jetpack Protect",
            "Jetpack VideoPress",
            "Jetpack Social",
            "Jetpack Boost",
        )
        assertEquals(expected, model.activeIndividualJetpackPluginNames())
    }
    // endregion

    // region JetpackCPConnectedSiteModel extensions
    @Test
    fun `JetpackCPConnectedSiteModel - Should return FALSE if plugins string is null when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(
            jetpackCPConnectedSiteModel(null)
                .isJetpackIndividualPluginConnectedWithoutFullPlugin()
        )
    }

    @Test
    fun `JetpackCPConnectedSiteModel - Should return FALSE if plugins string is empty when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(jetpackCPConnectedSiteModel("").isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `JetpackCPConnectedSiteModel - Should return FALSE if plugins string is not valid when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(
            jetpackCPConnectedSiteModel("something")
                .isJetpackIndividualPluginConnectedWithoutFullPlugin()
        )
    }

    @Test
    fun `JetpackCPConnectedSiteModel - Should return FALSE if plugins list is empty when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(jetpackCPConnectedSiteModel("").isJetpackIndividualPluginConnectedWithoutFullPlugin())
    }

    @Test
    fun `JetpackCPConnectedSiteModel - Should return FALSE if plugins list contains jetpack when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(
            jetpackCPConnectedSiteModel("jetpack-1,jetpack")
                .isJetpackIndividualPluginConnectedWithoutFullPlugin()
        )
    }

    @Test
    fun `JetpackCPConnectedSiteModel - Should return FALSE if plugins list does not contain at least one element that starts with jetpack- when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertFalse(
            jetpackCPConnectedSiteModel("plugin1,plugin2")
                .isJetpackIndividualPluginConnectedWithoutFullPlugin()
        )
    }

    @Test
    fun `JetpackCPConnectedSiteModel - Should return TRUE if plugins list is not empty, does not contain jetpack and contains at least one element that starts with jetpack- when isJetpackIndividualPluginConnectedWithoutFullPlugin is called`() {
        assertTrue(
            jetpackCPConnectedSiteModel("jetpack-1")
                .isJetpackIndividualPluginConnectedWithoutFullPlugin()
        )
        assertTrue(
            jetpackCPConnectedSiteModel("jetpack-1,something")
                .isJetpackIndividualPluginConnectedWithoutFullPlugin()
        )
        assertTrue(
            jetpackCPConnectedSiteModel("something,jetpack-1")
                .isJetpackIndividualPluginConnectedWithoutFullPlugin()
        )
    }

    @Test
    fun `JetpackCPConnectedSiteModel - Should return list of plugin names if list contains known individual jetpack plugins, filtering out non-jetpack plugins and unknown jetpack plugins`() {
        val model = jetpackCPConnectedSiteModel(
            "jetpack-search," +
                    "jetpack-backup," +
                    "jetpack-protect," +
                    "jetpack-videopress," +
                    "jetpack-social," +
                    "jetpack-boost," +
                    "jetpack-unknown," +
                    "not-jetpack-plugin"
        )
        val expected = listOf(
            "Jetpack Search",
            "Jetpack VaultPress Backup",
            "Jetpack Protect",
            "Jetpack VideoPress",
            "Jetpack Social",
            "Jetpack Boost",
        )
        assertEquals(expected, model.activeIndividualJetpackPluginNames())
    }
    // endregion

    private fun siteModel(activeJpPlugins: String?) =
        SiteModel().apply {
            activeJetpackConnectionPlugins = activeJpPlugins
        }

    private fun jetpackCPConnectedSiteModel(activeJpPlugins: String?) =
        JetpackCPConnectedSiteModel(
            remoteSiteId = null,
            localSiteId = 0,
            url = "url",
            name = "name",
            description = "description",
            activeJetpackConnectionPlugins = activeJpPlugins?.split(",")?.toList() ?: listOf()
        )
}
